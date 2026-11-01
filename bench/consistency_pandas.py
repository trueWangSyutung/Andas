#!/usr/bin/env python3
"""Compute the reference metrics with pandas and write expected JSON for Android/JVM tests."""
from __future__ import annotations

import json
from pathlib import Path

import pandas as pd

ROOT = Path(__file__).resolve().parent
DATA = ROOT / "data"
RESULTS = ROOT / "results"
ASSETS = ROOT.parent / "android-pandas" / "src" / "androidTest" / "assets"


def main() -> None:
    df = pd.read_csv(DATA / "consistency_5000_c15.csv", engine="c")
    left = pd.read_csv(DATA / "join_left_5000.csv", engine="c")
    right = pd.read_csv(DATA / "join_right_5000.csv", engine="c")

    sorted_df = df.sort_values("double_col_10", ascending=True)
    filtered = df[df["double_col_10"] > 90.0]
    grouped = df.groupby("string_col_3")["double_col_10"].sum()
    merged = left.merge(right, on="id", how="inner")

    series = df["double_col_10"]
    mean = float(series.mean())
    std_pop = float(series.std(ddof=0))
    normalized = (series - mean) / std_pop
    describe = series.describe()

    metrics = {
        "rows": int(len(df)),
        "cols": int(df.shape[1]),
        "sum_double_col_10": float(series.sum()),
        "mean_double_col_10": mean,
        "std_double_col_10": float(series.std(ddof=1)),
        "min_double_col_10": float(series.min()),
        "max_double_col_10": float(series.max()),
        "mean_long_col_4": float(df["long_col_4"].mean()),
        "describe_count": float(describe["count"]),
        "describe_mean": float(describe["mean"]),
        "describe_std": float(describe["std"]),
        "describe_min": float(describe["min"]),
        "describe_max": float(describe["max"]),
        "sort_first": float(sorted_df.iloc[0]["double_col_10"]),
        "sort_last": float(sorted_df.iloc[-1]["double_col_10"]),
        "sort_first50_sum": float(sorted_df["double_col_10"].head(50).sum()),
        "sort_last50_sum": float(sorted_df["double_col_10"].tail(50).sum()),
        "filter_count": int(len(filtered)),
        "filter_sum": float(filtered["double_col_10"].sum()),
        "groupby_count": int(len(grouped)),
        "groupby_sum": {str(k): float(v) for k, v in sorted(grouped.items())},
        "merge_rows": int(len(merged)),
        "merge_amount_sum": float(merged["amount"].sum()),
        "corr_double_10_11": float(df["double_col_10"].corr(df["double_col_11"])),
        "normalize_first": float(normalized.iloc[0]),
        "normalize_last": float(normalized.iloc[-1]),
    }
    expected = {
        "dataset": "consistency_5000_c15.csv",
        "generator": "pandas",
        "pandas_version": pd.__version__,
        "dtypes": {c: str(df[c].dtype) for c in df.columns},
        "metrics": metrics,
    }
    RESULTS.mkdir(parents=True, exist_ok=True)
    ASSETS.mkdir(parents=True, exist_ok=True)
    text = json.dumps(expected, ensure_ascii=False, indent=2)
    (RESULTS / "consistency_pandas.json").write_text(text + "\n", encoding="utf-8")
    (ASSETS / "consistency_pandas.json").write_text(text + "\n", encoding="utf-8")
    print(text)


if __name__ == "__main__":
    main()
