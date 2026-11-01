#!/usr/bin/env python3
"""Create the deterministic 5k-row consistency dataset and Android test assets."""
from __future__ import annotations

import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent
DATA = ROOT / "data"
ASSETS = ROOT.parent / "android-pandas" / "src" / "androidTest" / "assets"
SOURCE = DATA / "bench_50000_c15.csv"
TARGET = DATA / "consistency_5000_c15.csv"


def main() -> None:
    if not SOURCE.exists():
        raise SystemExit(f"missing {SOURCE}; run bench/gen_data.py first")
    with SOURCE.open(encoding="utf-8") as src, TARGET.open("w", encoding="utf-8") as dst:
        dst.write(src.readline())
        for _ in range(5000):
            line = src.readline()
            if not line:
                break
            dst.write(line)
    ASSETS.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(TARGET, ASSETS / TARGET.name)
    for name in ["join_left_5000.csv", "join_right_5000.csv"]:
        shutil.copyfile(DATA / name, ASSETS / name)
    print(f"wrote {TARGET} and assets -> {ASSETS}")


if __name__ == "__main__":
    main()
