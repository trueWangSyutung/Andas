#!/usr/bin/env python3
"""Single pandas measurement, invoked in a fresh Python process per run.

Usage:
  python3 bench/pandas_bench.py read  bench/data/bench_50000_c15.csv
  python3 bench/pandas_bench.py sum   bench/data/bench_50000_c15.csv
  python3 bench/pandas_bench.py merge bench/data/join_left_5000.csv bench/data/join_right_5000.csv
"""
from __future__ import annotations

import gc
import json
import resource
import sys
import time

import pandas as pd
import psutil


def rss_mb() -> float:
    return psutil.Process().memory_info().rss / 1024 / 1024


def maxrss_mb() -> float:
    value = resource.getrusage(resource.RUSAGE_SELF).ru_maxrss
    # macOS reports bytes, Linux reports KiB
    return value / 1024 / 1024 if sys.platform == "darwin" else value / 1024


def read_csv(path: str):
    return pd.read_csv(path, engine="c")


def main() -> int:
    if len(sys.argv) < 3:
        print(__doc__, file=sys.stderr)
        return 2
    mode = sys.argv[1]
    path = sys.argv[2]
    warm = "bench/data/bench_10000_c15.csv"

    import os

    if not os.path.exists(warm):
        warm = path

    # Warm up pandas / the C parser and the operation, then measure a fresh read.
    warm_df = read_csv(warm)
    if mode not in ("read",):
        if mode == "merge":
            warm_left = read_csv("bench/data/join_left_5000.csv")
            warm_right = read_csv("bench/data/join_right_5000.csv")
            run_operation(mode, warm_left, warm_right)
        else:
            run_operation(mode, warm_df, None)
    warm_df = None
    gc.collect()

    before_rss = rss_mb()
    before_max = maxrss_mb()
    started = time.perf_counter()
    df = read_csv(path)
    read_ms = (time.perf_counter() - started) * 1000
    after_rss = rss_mb()
    after_max = maxrss_mb()
    rows = int(len(df))

    result = None
    op_ms = 0.0
    if mode != "read":
        right = read_csv(sys.argv[3]) if mode == "merge" and len(sys.argv) > 3 else None
        # warm the operation on the measured frame once
        run_operation(mode, df, right)
        started = time.perf_counter()
        result = run_operation(mode, df, right)
        op_ms = (time.perf_counter() - started) * 1000

    print(
        json.dumps(
            {
                "impl": "pandas",
                "mode": mode,
                "rows": rows,
                "read_ms": round(read_ms, 2),
                "op_ms": round(op_ms, 2),
                "peak_mb": round(max(0.0, after_max - before_max), 1),
                "used_mb": round(after_rss - before_rss, 1),
                "status": "ok",
                "result": "" if result is None else str(result),
                "python": sys.version.split()[0],
                "pandas": pd.__version__,
            },
            ensure_ascii=False,
        )
    )
    return 0


def run_operation(mode: str, df, right):
    if mode == "sum":
        return float(df["double_col_10"].sum())
    if mode == "mean":
        return float(df["double_col_10"].mean())
    if mode == "std":
        return float(df["double_col_10"].std())
    if mode == "describe":
        d = df["double_col_10"].describe()
        return f'{int(d["count"])};{d["mean"]:.6f};{d["std"]:.6f}'
    if mode == "sort":
        sorted_df = df.sort_values("double_col_10", ascending=True)
        return f'{len(sorted_df)};{sorted_df.iloc[0]["double_col_10"]}'
    if mode == "filter":
        return int(len(df[df["double_col_10"] > 90.0]))
    if mode == "groupby":
        grouped = df.groupby("string_col_3")["double_col_10"].sum()
        return int(len(grouped))
    if mode == "merge":
        merged = df.merge(right, on="id", how="inner")
        return int(len(merged))
    raise ValueError(mode)


if __name__ == "__main__":
    sys.exit(main())
