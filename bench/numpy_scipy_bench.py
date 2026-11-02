#!/usr/bin/env python3
"""NumPy performance reference for Andas NDArray/SciPy benchmarks."""
import json
import math
import statistics
import time
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[1]
RESULTS = ROOT / "bench/results"
RESULTS.mkdir(parents=True, exist_ok=True)
SEED = 20261102


def median_time(operation, repeats=5, warmup=2):
    for _ in range(warmup):
        operation()
    samples = []
    for _ in range(repeats):
        start = time.perf_counter()
        operation()
        samples.append((time.perf_counter() - start) * 1000.0)
    return statistics.median(samples)


rng = np.random.default_rng(SEED)
left = rng.random(1_000_000)
right = rng.random(1_000_000)
sum_data = rng.random(1_000_000)
matrix = rng.random((300, 300))
fft_data = rng.random(1024)
quantile_data = rng.random(1_000_000)
conv_data = rng.random(100_000)
kernel = rng.random(16)
reg_x = rng.random((20_000, 5))
reg_y = reg_x @ np.array([1.5, -2.0, 0.5, 3.0, -1.0]) + 2.0

results = {
    "add_1m": median_time(lambda: np.add(left, right)),
    "sum_1m": median_time(lambda: np.sum(sum_data)),
    "matmul_300": median_time(lambda: matrix @ matrix.T),
    "fft_1024": median_time(lambda: np.fft.fft(fft_data)),
    "quantile_1m": median_time(lambda: np.quantile(quantile_data, 0.5)),
    "regression_20k_x5": median_time(lambda: np.linalg.lstsq(reg_x, reg_y, rcond=None)),
    "convolve_100k_x16": median_time(lambda: np.convolve(conv_data, kernel, mode="full")),
}

payload = {
    "environment": {
        "python": __import__("sys").version.split()[0],
        "numpy": np.__version__,
        "seed": SEED,
    },
    "median_ms": results,
}
with (RESULTS / "numpy_scipy_python.json").open("w", encoding="utf-8") as handle:
    json.dump(payload, handle, indent=2, sort_keys=True)
print(json.dumps(payload, indent=2, sort_keys=True))
