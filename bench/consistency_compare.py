#!/usr/bin/env python3
"""Compare pandas reference metrics with JVM/Android Andas results."""
from __future__ import annotations

import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
RESULTS = ROOT / "results"
TOL = 1e-10


def load_json(path: Path):
    if not path.exists():
        return None
    return json.loads(path.read_text(encoding="utf-8"))


def android_from_log(path: Path):
    if not path.exists():
        return None
    for line in path.read_text(encoding="utf-8").splitlines():
        marker = "ANDAS_CONSISTENCY: "
        if marker in line:
            payload = line.split(marker, 1)[1]
            try:
                return json.loads(payload)
            except json.JSONDecodeError:
                continue
    return None


def flatten_metrics(metrics: dict) -> dict:
    values = {}
    for key, value in metrics.items():
        if key == "groupby_sum":
            for group, sum_value in value.items():
                values[f"groupby_sum.{group}"] = sum_value
        else:
            values[key] = value
    return values


def compare(expected: dict, actual: dict | None):
    if actual is None:
        return [], None
    expected_values = flatten_metrics(expected)
    actual_values = flatten_metrics(actual.get("metrics", actual))
    rows = []
    max_rel = 0.0
    for key, expected_value in expected_values.items():
        actual_value = actual_values.get(key)
        if isinstance(expected_value, int) and not isinstance(expected_value, bool):
            # Integer metrics are exact.
            ok = actual_value is not None and float(actual_value) == float(expected_value)
            rows.append((key, expected_value, actual_value, 0.0, ok))
            continue
        if actual_value is None:
            rows.append((key, expected_value, None, None, False))
            continue
        rel = abs(float(actual_value) - float(expected_value)) / max(1.0, abs(float(expected_value)))
        max_rel = max(max_rel, rel)
        rows.append((key, expected_value, actual_value, rel, rel <= TOL))
    return rows, max_rel


def main() -> None:
    expected_data = load_json(RESULTS / "consistency_pandas.json")
    if not expected_data:
        raise SystemExit("run bench/consistency_pandas.py first")
    expected = expected_data["metrics"]

    jvm = load_json(RESULTS / "consistency_jvm.json")
    android = android_from_log(RESULTS / "android_consistency.log")

    jvm_rows, jvm_max = compare(expected, jvm)
    android_rows, android_max = compare(expected, android)

    summary = {
        "dataset": expected_data.get("dataset"),
        "pandas_version": expected_data.get("pandas_version"),
        "tolerance_relative": TOL,
        "jvm": {
            "present": jvm is not None,
            "max_relative_error": jvm_max,
            "passed": sum(1 for row in jvm_rows if row[4]),
            "total": len(jvm_rows),
            "all_pass": bool(jvm_rows) and all(row[4] for row in jvm_rows),
        },
        "android": {
            "present": android is not None,
            "max_relative_error": android_max,
            "passed": sum(1 for row in android_rows if row[4]),
            "total": len(android_rows),
            "all_pass": bool(android_rows) and all(row[4] for row in android_rows),
            "device": android.get("device") if android else None,
        },
    }
    (RESULTS / "consistency_summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    with (RESULTS / "consistency_summary.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(
            ["metric", "pandas", "jvm", "jvm_rel_error", "android", "android_rel_error", "status"]
        )
        jvm_map = {row[0]: row for row in jvm_rows}
        android_map = {row[0]: row for row in android_rows}
        for key in flatten_metrics(expected):
            j = jvm_map.get(key)
            a = android_map.get(key)
            status = "PASS"
            if j and not j[4]:
                status = "FAIL(jvm)"
            if a and not a[4]:
                status = "FAIL(android)"
            writer.writerow(
                [
                    key,
                    flatten_metrics(expected)[key],
                    "" if not j else j[2],
                    "" if not j else j[3],
                    "" if not a else a[2],
                    "" if not a else a[3],
                    status,
                ]
            )

    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
