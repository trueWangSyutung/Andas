#!/usr/bin/env python3
"""Generate NumPy/SciPy reference values for Andas JVM/Android consistency checks."""
import csv
import json
from pathlib import Path

import math
import numpy as np
import mpmath as mp

ROOT = Path(__file__).resolve().parents[1]
RESULT = ROOT / "bench/results"
RESULT.mkdir(parents=True, exist_ok=True)

data = np.arange(1000, dtype=np.float64) * 0.5 - 10.0
quantiles = np.quantile(data, [0.25, 0.5, 0.75])
a = np.array([[4.0, 1.0, 2.0], [1.0, 3.0, 0.0], [2.0, 0.0, 5.0]])
b = np.array([1.0, 2.0, 3.0])
solution = np.linalg.solve(a, b)
matrix = np.array([[1.0, 2.0, 3.0], [4.0, 5.0, 6.0], [7.0, 8.0, 10.0]])
matrix_product = matrix @ matrix.T
fft_input = np.arange(1.0, 9.0)
spectrum = np.fft.fft(fft_input)
conv_a = np.array([1.0, 2.0, 3.0])
conv_b = np.array([4.0, 5.0])
convolution = np.convolve(conv_a, conv_b)
reg_x = np.arange(1.0, 11.0)
reg_y = 2.0 * reg_x + 1.0
slope, intercept = np.polyfit(reg_x, reg_y, 1)
t_stat = float(np.mean(data) / (np.std(data, ddof=1) / math.sqrt(data.size)))
df = data.size - 1
p_value = float(mp.betainc(df / 2.0, 0.5, 0.0, df / (df + t_stat * t_stat), regularized=True))
normal_cdf = 0.5 * (1.0 + math.erf(1.5 / math.sqrt(2.0)))

values = {
    "size": float(data.size),
    "sum": float(np.sum(data)),
    "mean": float(np.mean(data)),
    "std": float(np.std(data, ddof=1)),
    "variance": float(np.var(data, ddof=1)),
    "quantile_25": float(quantiles[0]),
    "quantile_50": float(quantiles[1]),
    "quantile_75": float(quantiles[2]),
    "det": float(np.linalg.det(a)),
    "normal_cdf": float(normal_cdf),
    "t_stat": float(t_stat),
    "t_p": float(p_value),
    "regression_slope": float(slope),
    "regression_intercept": float(intercept),
}
for i in range(3):
    values[f"matmul_{i}"] = float(matrix_product[i, 0])
    values[f"matmul_{i}_1"] = float(matrix_product[i, 1])
    values[f"solution_{i}"] = float(solution[i])
for i in range(8):
    values[f"fft_real_{i}"] = float(spectrum.real[i])
    values[f"fft_imag_{i}"] = float(spectrum.imag[i])
for i in range(4):
    values[f"conv_{i}"] = float(convolution[i])

with (RESULT / "numpy_consistency.json").open("w", encoding="utf-8") as handle:
    json.dump(values, handle, indent=2, sort_keys=True)

rows = sorted(values.items())
csv_paths = [
    RESULT / "numpy_consistency.csv",
    ROOT / "android-pandas/src/test/resources/numpy_consistency.csv",
    ROOT / "android-pandas/src/androidTest/assets/numpy_consistency.csv",
]
for path in csv_paths:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        for key, value in rows:
            writer.writerow([key, repr(value)])
print(f"wrote {len(rows)} reference values")
