#!/usr/bin/env python3
"""Run the pandas reference measurements (3 fresh processes per configuration)."""
from __future__ import annotations

import csv
import json
import statistics
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BENCH = ROOT / "bench"
RESULTS = BENCH / "results"
REPS = 3
TIMEOUT = 180
SIZES = [10_000, 25_000, 50_000, 100_000, 200_000]
OPS = ["sum", "mean", "std", "describe", "sort", "filter", "groupby", "merge"]


def run(mode: str, args: list[str]) -> dict:
    cmd = [sys.executable, str(BENCH / "pandas_bench.py"), mode, *args]
    try:
        completed = subprocess.run(cmd, capture_output=True, text=True, timeout=TIMEOUT, check=False)
    except subprocess.TimeoutExpired:
        return {"status": "timeout", "error": f"timeout>{TIMEOUT}s"}
    lines = completed.stdout.strip().splitlines()
    if not lines:
        return {"status": "error", "error": (completed.stderr or "no output")[-300:]}
    try:
        return json.loads(lines[-1])
    except json.JSONDecodeError:
        return {"status": "error", "error": lines[-1][:300]}


def median(values):
    values = [v for v in values if isinstance(v, (int, float))]
    return round(statistics.median(values), 2) if values else None


def main() -> int:
    RESULTS.mkdir(parents=True, exist_ok=True)
    raw: list[dict] = []

    for rows in SIZES:
        for _ in range(REPS):
            record = run("read", [str(BENCH / f"data/bench_{rows}_c15.csv")])
            record.update({"mode": "read", "target_rows": rows})
            raw.append(record)
            print(f"read {rows}: {record.get('read_ms')} ms / {record.get('used_mb')} MB", flush=True)

    for op in OPS:
        for _ in range(REPS):
            if op == "merge":
                args = [str(BENCH / "data/join_left_5000.csv"), str(BENCH / "data/join_right_5000.csv")]
            else:
                args = [str(BENCH / "data/bench_50000_c15.csv")]
            record = run(op, args)
            record.update({"mode": op, "target_rows": 5000 if op == "merge" else 50000})
            raw.append(record)
            print(
                f"{op}: {record.get('op_ms')} ms (read {record.get('read_ms')} ms)",
                flush=True,
            )

    (RESULTS / "pandas_raw.json").write_text(json.dumps(raw, ensure_ascii=False, indent=2))

    groups: dict[tuple, list[dict]] = {}
    for record in raw:
        groups.setdefault((record["mode"], record.get("target_rows", 0)), []).append(record)

    with (RESULTS / "pandas_summary.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(
            ["impl", "mode", "rows", "read_ms", "op_ms", "peak_mb", "used_mb", "status", "reps"]
        )
        for (mode, rows), records in sorted(groups.items()):
            ok = [r for r in records if r.get("status") == "ok"]
            status = "ok" if len(ok) == len(records) else ("partial" if ok else records[0].get("status"))
            writer.writerow(
                [
                    "pandas", mode, rows,
                    median([r.get("read_ms") for r in ok]),
                    median([r.get("op_ms") for r in ok]),
                    median([r.get("peak_mb") for r in ok]),
                    median([r.get("used_mb") for r in ok]),
                    status, len(ok),
                ]
            )
    print("pandas results written")
    return 0


if __name__ == "__main__":
    sys.exit(main())
