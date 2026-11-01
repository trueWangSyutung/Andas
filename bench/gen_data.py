#!/usr/bin/env python3
"""Generate deterministic synthetic CSV datasets for the Andas benchmark.

The schema intentionally mirrors app/src/main/assets/largest.csv:
3 strings, 3 longs, 3 ints, 3 doubles, 3 floats (15 columns total).
"""
from __future__ import annotations

import random
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "data"
ROWS = [10_000, 25_000, 50_000, 100_000, 200_000]
HEADER = (
    "string_col_1,string_col_2,string_col_3,long_col_4,long_col_5,long_col_6,"
    "int_col_7,int_col_8,int_col_9,double_col_10,double_col_11,double_col_12,"
    "float_col_13,float_col_14,float_col_15"
)


def gen(rows: int, rng: random.Random) -> None:
    path = OUT / f"bench_{rows}_c15.csv"
    alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    def text() -> str:
        return "".join(alphabet[rng.randrange(len(alphabet))] for _ in range(10))

    with path.open("w", buffering=1 << 20) as handle:
        handle.write(HEADER + "\n")
        buffer: list[str] = []
        for i in range(rows):
            buffer.append(
                ",".join(
                    [
                        text(),
                        text(),
                        f"cat_{i % 10}",
                        str(rng.randrange(100_000_000_000, 999_999_999_999)),
                        str(rng.randrange(100_000_000_000, 999_999_999_999)),
                        str(rng.randrange(100_000_000_000, 999_999_999_999)),
                        str(rng.randint(-100, 100)),
                        str(rng.randint(-100, 100)),
                        str(rng.randint(-100, 100)),
                        repr(rng.uniform(-100, 100)),
                        repr(rng.uniform(-100, 100)),
                        repr(rng.uniform(-100, 100)),
                        repr(rng.uniform(-100, 100)),
                        repr(rng.uniform(-100, 100)),
                        repr(rng.uniform(-100, 100)),
                    ]
                )
            )
            if len(buffer) >= 4096:
                handle.write("\n".join(buffer) + "\n")
                buffer.clear()
        if buffer:
            handle.write("\n".join(buffer) + "\n")
    print(f"{path}  {path.stat().st_size / 1024 / 1024:.1f} MB")


def gen_join(rng: random.Random) -> None:
    left = OUT / "join_left_5000.csv"
    right = OUT / "join_right_5000.csv"
    with left.open("w") as handle:
        handle.write("id,category,amount\n")
        for i in range(5000):
            handle.write(f"{i},left_{i % 10},{rng.randrange(1000)}\n")
    with right.open("w") as handle:
        handle.write("id,category,score\n")
        for i in range(5000):
            handle.write(f"{i},right_{i % 7},{rng.random() * 100}\n")
    print(left, right)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    rng = random.Random(20261101)
    for rows in ROWS:
        gen(rows, rng)
    gen_join(rng)


if __name__ == "__main__":
    main()
