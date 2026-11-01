#!/usr/bin/env python3
"""Run the Andas CSV / computation benchmarks for the technical report.

Each measurement runs in a fresh JVM.  The baseline is the pre-optimization
revision checked out in .baseline/ (JNI is unavailable on the JVM, so both
implementations use their Kotlin fallback paths here).
"""
from __future__ import annotations

import json
import statistics
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BENCH = ROOT / "bench"
RESULTS = BENCH / "results"
STDLIB = (BENCH / "stdlib.path").read_text().strip()

IMPLS = {
    "baseline": [
        str(BENCH / "classes" / "baseline"),
        str(ROOT / ".baseline/android-pandas/build/tmp/kotlin-classes/debug"),
        STDLIB,
    ],
    "new": [
        str(BENCH / "classes" / "new"),
        str(ROOT / "android-pandas/build/tmp/kotlin-classes/debug"),
        STDLIB,
    ],
}

READ_SIZES = [10_000, 25_000, 50_000, 100_000, 200_000]
OPS = ["sum", "mean", "std", "describe", "sort", "filter", "groupby"]
REPS = 3
TIMEOUT = 180


def run(impl: str, mode: str, args: list[str], heap: str) -> dict:
    cmd = [
        "java",
        f"-Xmx{heap}",
        "-Dimpl=" + impl,
        "-cp",
        ":".join(IMPLS[impl]),
        "Bench",
        mode,
        *args,
    ]
    try:
        completed = subprocess.run(
            cmd, capture_output=True, text=True, timeout=TIMEOUT, check=False
        )
    except subprocess.TimeoutExpired:
        return {"status": "timeout", "error": f"timeout>{TIMEOUT}s"}

    stdout = completed.stdout.strip().splitlines()
    if not stdout:
        return {
            "status": "error",
            "error": (completed.stderr or "").strip().splitlines()[-1:] or ["no output"],
        }
    try:
        record = json.loads(stdout[-1])
    except json.JSONDecodeError:
        return {"status": "error", "error": stdout[-1][:300]}
    return record


def median(values):
    values = [v for v in values if isinstance(v, (int, float))]
    return round(statistics.median(values), 2) if values else None


def main() -> int:
    RESULTS.mkdir(parents=True, exist_ok=True)
    raw: list[dict] = []

    jobs = []
    for impl in IMPLS:
        for rows in READ_SIZES:
            for _ in range(REPS):
                jobs.append((impl, "read", [str(BENCH / f"data/bench_{rows}_c15.csv")], "256m"))
        for op in OPS:
            for _ in range(REPS):
                jobs.append((impl, op, [str(BENCH / "data/bench_50000_c15.csv")], "1g"))
        for _ in range(REPS):
            jobs.append(
                (
                    impl,
                    "merge",
                    [
                        str(BENCH / "data/join_left_5000.csv"),
                        str(BENCH / "data/join_right_5000.csv"),
                    ],
                    "1g",
                )
            )

    total = len(jobs)
    for index, (impl, mode, args, heap) in enumerate(jobs, 1):
        record = run(impl, mode, args, heap)
        record.update({"impl": impl, "mode": mode, "heap": heap, "args": args})
        if not record.get("rows"):
            import re

            match = re.search(r"bench_(\d+)_c15", args[0])
            if match:
                record["rows"] = int(match.group(1))
            elif mode == "merge":
                record["rows"] = 5000
        raw.append(record)
        print(
            f"[{index:>3}/{total}] {impl:<8} {mode:<9} "
            f"status={record.get('status')} "
            f"read={record.get('read_ms')}ms op={record.get('op_ms')}ms "
            f"peak={record.get('peak_mb')}MB",
            flush=True,
        )

    (RESULTS / "raw.json").write_text(json.dumps(raw, ensure_ascii=False, indent=2))

    # Aggregate: group by (impl, mode, rows)
    groups: dict[tuple, list[dict]] = {}
    for record in raw:
        rows = record.get("rows") or 0
        groups.setdefault((record["impl"], record["mode"], rows), []).append(record)

    summary = []
    for (impl, mode, rows), records in sorted(groups.items()):
        ok = [r for r in records if r.get("status") == "ok"]
        statuses = {r.get("status") for r in records}
        status = "ok" if len(ok) == len(records) else (
            "partial" if ok else next(iter(statuses))
        )
        summary.append(
            {
                "impl": impl,
                "mode": mode,
                "rows": rows,
                "read_ms": median([r.get("read_ms") for r in ok]),
                "op_ms": median([r.get("op_ms") for r in ok]),
                "peak_mb": median([r.get("peak_mb") for r in ok]),
                "used_mb": median([r.get("used_mb") for r in ok]),
                "status": status,
                "reps": len(ok),
                "error": next((r.get("error") for r in records if r.get("error")), None),
            }
        )

    # Speedups baseline / new for matching (mode, rows)
    by_key = {(s["impl"], s["mode"], s["rows"]): s for s in summary}
    for item in summary:
        base = by_key.get(("baseline", item["mode"], item["rows"]))
        if base and base["status"] == "ok" and item["status"] == "ok":
            if item["mode"] == "read":
                item["speedup"] = round(base["read_ms"] / item["read_ms"], 2) if item["read_ms"] else None
            else:
                item["speedup"] = round(base["op_ms"] / item["op_ms"], 2) if item["op_ms"] else None
            item["peak_ratio"] = (
                round(base["peak_mb"] / item["peak_mb"], 2) if item["peak_mb"] else None
            )
        else:
            item["speedup"] = None
            item["peak_ratio"] = None

    columns = [
        "impl", "mode", "rows", "read_ms", "op_ms", "peak_mb", "used_mb",
        "status", "reps", "speedup", "peak_ratio", "error",
    ]
    with (RESULTS / "summary.csv").open("w", encoding="utf-8") as handle:
        handle.write(",".join(columns) + "\n")
        for item in summary:
            handle.write(
                ",".join(
                    "" if item.get(col) is None else str(item.get(col)).replace(",", ";")
                    for col in columns
                )
                + "\n"
            )

    print("\nSummary written to bench/results/summary.csv")
    return 0


if __name__ == "__main__":
    sys.exit(main())
