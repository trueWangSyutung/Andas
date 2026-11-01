# English Technical Report

> **Andas: A Columnar, Streaming Data Processing Library for Android**<br>
> Technical Report on CSV Ingestion, Computation, and Performance Evaluation<br>
> Andas Engineering Team · November 1, 2026

This page is the web edition of the English technical report. The full paper is available as a
typeset PDF:

- [Download the PDF](../assets/andas-technical-report.pdf)
- [LaTeX source](../assets/andas-technical-report.tex)

## Abstract

Andas is an Android library that brings a pandas-like data-analysis API to Kotlin and Java. The
previous implementation materialized an entire CSV file as a list of strings, constructed a map per
row, and then transposed the rows into columns; each column also kept a private copy of the index
and a label-to-position hash map. On a 192 MB Android heap this design ran out of memory at
100k rows × 15 columns. We redesigned ingestion and computation around streaming parsing, columnar
primitive storage, a shared range index, and algorithmically improved operators.

On the JVM, the optimized reader completes 200k rows × 15 columns (36 MB) in 1.44 s with 56 MB of
retained heap, whereas the baseline fails with an out-of-memory error at the same size. On an
Android ARM64 emulator, the baseline fails at 100k rows and the optimized reader completes 200k rows
with 35 MB of retained heap. At 50k rows, statistical, sorting, and join operations improve by
7–33× over the baseline; filtering and grouped aggregation improve by 2.9× and 1.4×, respectively.
Compared with pandas 2.1.0, the optimized reader is still 3.2–6.0× slower on the desktop JVM but uses
less memory, while computation kernels remain 6–20× slower, pointing to native columnar kernels as
future work.

## Design in brief

- **Streaming CSV reader.** A 64 KB character buffer feeds a record state machine that handles
  quoted fields, escaped quotes, embedded commas/newlines, CRLF, UTF-8 BOM, and blank lines. Fields
  are appended to reusable buffers and written directly into primitive column arrays.
- **Shared index.** All columns share one index. The default `0..n-1` index is a virtual
  `RangeIndexList` with O(1) memory; the label-to-position map is built lazily.
- **Primitive storage.** `IntArray`, `LongArray`, `DoubleArray`, and `BooleanArray` hold numeric
  columns; a bitmap is allocated only when nulls occur. The public `List` view boxes on demand.
- **Algorithms.** Statistics use a single pass and Welford's variance; sorting is O(n log n) and
  numeric; grouping and joins use hash tables; filtering projects columns once.

![JVM read scaling](../assets/images/fig_report_jvm.png)

*Figure: JVM CSV read time and retained heap. The baseline fails at 200k rows under a 256 MB heap.*

## Key measurements

### JVM CSV read (36 MB, 15 columns, `-Xmx256m`)

| Rows | Baseline | Optimized | Speedup | Baseline heap | Optimized heap |
|---:|---:|---:|---:|---:|---:|
| 10k | 321 ms | 138 ms | 2.33× | 15 MB | 4 MB |
| 50k | 1071 ms | 541 ms | 1.98× | 78 MB | 14 MB |
| 100k | 2540 ms | 964 ms | 2.63× | 155 MB | 30 MB |
| 200k | OOM | 1440 ms | — | OOM | 56 MB |

### Computation (50k rows; merge 5k × 5k)

| Operation | Baseline | Optimized | Speedup |
|---|---:|---:|---:|
| sum | 30 ms | 1 ms | 30.0× |
| mean | 28 ms | 2 ms | 14.0× |
| std | 28 ms | 4 ms | 7.0× |
| describe | 80 ms | 3 ms | 26.7× |
| sortValues | 2629 ms | 80 ms | 32.9× |
| filterGreaterThan | 23 ms | 8 ms | 2.9× |
| groupBySum | 27 ms | 20 ms | 1.4× |
| merge | 557 ms | 18 ms | 30.9× |

### pandas 2.1.0 comparison

| Metric | Baseline | Optimized | pandas |
|---|---:|---:|---:|
| Read 200k rows | OOM | 1440 ms | 446.7 ms |
| Read 200k memory | OOM | 56 MB (heap) | 106.8 MB (RSS) |
| sum 50k | 30 ms | 1 ms | 0.12 ms |
| sort 50k | 2629 ms | 80 ms | 12.67 ms |
| merge 5k × 5k | 557 ms | 18 ms | 1.43 ms |

pandas runs on CPython with a C parser and NumPy arrays; Andas runs on the JVM/ART. The memory
figures are a GC-retained heap for Andas and an RSS increment for pandas, so the comparison is an
order-of-magnitude reference rather than a claim that one library is universally faster.

### Cross-language consistency

Performance alone is not sufficient: the same input must produce the same answer in pandas,
on the JVM, and on Android. We extracted the first 5000 rows of the 50k dataset, generated
reference metrics with pandas 2.1.0, and compared them with the JVM harness and an Android
instrumentation test. Integer metrics (row/column counts, filter/group/merge counts) must match
exactly; floating-point metrics use a relative tolerance of 1e-10.

| Metric | pandas | JVM | Android |
| --- | ---: | ---: | ---: |
| `sum(double_col_10)` | -7465.802624422415 | -7465.802624422438 | -7465.802624422438 |
| `std(double_col_10)` | 57.606263335764076 | 57.60626333576399 | 57.60626333576399 |
| `sortValues` first | -99.94711091909323 | -99.94711091909323 | -99.94711091909323 |
| `filterGreaterThan` count | 222 | 222 | 222 |
| `groupby_sum[cat_3]` | -2092.5884491227703 | -2092.588449122769 | -2092.588449122769 |
| `merge` rows | 5000 | 5000 | 5000 |
| `corr(double_10, double_11)` | 0.008147698079084498 | 0.008147698079084522 | 0.008147698079084522 |

**Result: 35/35 metrics pass on both the JVM and Android; the maximum relative error is
3.17e-15**, well below the 1e-10 threshold. The remaining difference comes from floating-point
summation order (NumPy pairwise summation vs sequential accumulation), not from a semantic
difference. The full table is in
[`consistency_summary.csv`](../assets/data/consistency_summary.csv).

### Android emulator (API 36 ARM64, 192 MB heap)

| Rows | Baseline | Optimized | Baseline heap | Optimized heap |
|---:|---:|---:|---:|---:|
| 10k | 1787 ms | 1192 ms | 10 MB | 1 MB |
| 50k | 6431 ms | 3468 ms | 56 MB | 8 MB |
| 100k | OOM (18.1 s) | 7317 ms | — | 18 MB |
| 200k | OOM (64.5 s) | 16042 ms | — | 35 MB |

![Android read scaling](../assets/images/fig_report_android.png)

*Figure: Android CSV read time and retained heap. The baseline fails at 100k rows; the optimized
reader completes 200k rows with 35 MB of retained heap.*

## Reproducibility

The repository contains the generator, baseline worktree script, JVM harness, pandas comparison,
Android instrumentation test, raw data, and figures. See [the experiment guide](reproduce.md) for
the full commands.

## Conclusion

The legacy reader was limited by memory amplification, not CPU speed. Streaming columnar ingestion,
a shared index, primitive storage, and algorithmic rewrites raise the readable size from a
100k-row out-of-memory failure to 200k rows with 35 MB of retained heap on a 192 MB Android heap.
pandas remains faster on the desktop because of its C/NumPy engine, but the optimized library
narrows the read gap and uses less memory. The remaining 6–20× kernel gap motivates native columnar
kernels and zero-copy buffers as future work.
