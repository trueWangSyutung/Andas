#!/usr/bin/env python3
"""Build the pandas vs Andas comparison tables from summary CSVs."""
from __future__ import annotations

import csv
from pathlib import Path

RESULTS = Path(__file__).resolve().parent / "results"


def load(path):
    with (RESULTS / path).open(encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def num(value):
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def main() -> None:
    andas = load("summary.csv")
    pandas_rows = load("pandas_summary.csv")

    def find(rows, impl, mode, n):
        for row in rows:
            if row.get("impl") == impl and row["mode"] == mode and int(row["rows"]) == n:
                return row
        return None

    pandas_map = {
        (row["mode"], int(row["rows"])): row for row in pandas_rows
    }

    output = []
    for n in [10_000, 25_000, 50_000, 100_000, 200_000]:
        base = find(andas, "baseline", "read", n)
        new = find(andas, "new", "read", n)
        pan = pandas_map.get(("read", n))
        output.append(
            {
                "kind": "read",
                "rows": n,
                "baseline_ms": base["read_ms"] if base and base["status"] == "ok" else "",
                "optimized_ms": new["read_ms"] if new and new["status"] == "ok" else "",
                "pandas_ms": pan["read_ms"] if pan else "",
                "baseline_mb": base["used_mb"] if base and base["status"] == "ok" else "",
                "optimized_mb": new["used_mb"] if new and new["status"] == "ok" else "",
                "pandas_mb": pan["used_mb"] if pan else "",
            }
        )

    for mode in ["sum", "mean", "std", "describe", "sort", "filter", "groupby", "merge"]:
        n = 5000 if mode == "merge" else 50000
        base = find(andas, "baseline", mode, n)
        new = find(andas, "new", mode, n)
        pan = pandas_map.get((mode, n))
        output.append(
            {
                "kind": mode,
                "rows": n,
                "baseline_ms": base["op_ms"] if base else "",
                "optimized_ms": new["op_ms"] if new else "",
                "pandas_ms": pan["op_ms"] if pan else "",
                "baseline_mb": "",
                "optimized_mb": "",
                "pandas_mb": "",
            }
        )

    with (RESULTS / "pandas_comparison.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=[
                "kind", "rows", "baseline_ms", "optimized_ms", "pandas_ms",
                "baseline_mb", "optimized_mb", "pandas_mb",
            ],
        )
        writer.writeheader()
        writer.writerows(output)

    print(f"{'kind':<10}{'rows':>8}{'baseline':>12}{'optimized':>12}{'pandas':>10}{'opt/pandas':>12}")
    for row in output:
        base = num(row["baseline_ms"])
        opt = num(row["optimized_ms"])
        pan = num(row["pandas_ms"])
        ratio = f"{opt / pan:.2f}x" if opt and pan else "-"
        print(
            f"{row['kind']:<10}{row['rows']:>8}"
            f"{(f'{base:.1f}' if base else 'OOM'):>12}"
            f"{(f'{opt:.1f}' if opt else '-'):>12}"
            f"{(f'{pan:.1f}' if pan else '-'):>10}{ratio:>12}"
        )


if __name__ == "__main__":
    main()
