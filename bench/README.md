# 实验复现指南

本文档说明如何复现《Andas 数据读取与计算性能优化》中的全部实验，包括 JVM 基准、
pandas 2.1.0 对比、Android instrumentation 测试与图表生成。原始数据、图表和脚本均随仓库提供。

## 1. 实验范围

| 实验 | 内容 | 产出 |
| --- | --- | --- |
| CSV 读取扩展性 | 10k–200k 行 × 15 列，固定 256 MB 堆 | 时间、GC 后堆、峰值堆、OOM |
| 计算内核 | 50k 行上的统计、排序、筛选、分组、连接 | 操作耗时与加速比 |
| pandas 对比 | 同机、同数据、CPython 3.10 + pandas 2.1.0 | 读取/计算对比表 |
| Android | 真机 Xiaomi M531DA，Android 17 / API 37，256 MB 堆，adb | 读取时间与存活堆 |
| 图表 | 论文用折线图、柱状图 | `results/fig_*.png` |

## 2. 环境

- Apple M2 / macOS 27.0.1 / JDK 21.0.12 / Gradle 8.13
- Python 3.10.11 / pandas 2.1.0 / psutil
- Android 真机：Xiaomi M531DA，Android 17 / API 37，`Runtime.maxMemory() = 256 MB`，USB adb
- 基线与优化版使用同一台机器、同一 JVM 参数、同一数据集；每档 3 次独立进程取中位数

## 3. 目录

```
bench/
├── gen_data.py                 # 生成 15 列、10k–200k 行的合成 CSV
├── prepare_baseline.sh         # 检出优化前版本、编译两套 class 与 Java harness
├── run_benchmarks.py           # JVM 基准矩阵
├── pandas_bench.py             # 单次 pandas 测量
├── run_pandas_benchmarks.py    # pandas 对比矩阵
├── compare_pandas.py           # pandas vs Andas 对比表
├── plot_results.py             # 生成图表
├── java/Bench.java             # 独立 JVM 基准入口
├── results/                    # raw.json / summary.csv / 图表
└── data/                       # 生成的 CSV（已 gitignore）
```

## 4. 生成数据

```bash
python3 bench/gen_data.py
```

生成 `bench_10000_c15.csv` … `bench_200000_c15.csv` 以及 5k × 5k 的连接数据集。
随机种子固定，所有实现读取完全相同的字节。

## 5. 基线与 harness

```bash
# 优化前基线：commit 9bb6715（可替换为任意优化前 commit）
./bench/prepare_baseline.sh 9bb6715
```

脚本会：

1. 在 `.baseline/` 检出优化前代码；
2. 让 `NativeMath/NativeData/NativeBatch` 在 JVM 上加载不到 `libandas_native.so` 时安全降级；
   Android 上仍使用真实 JNI；
3. 编译基线与当前版本的 Kotlin class；
4. 编译 `bench/java/Bench.java` 到 `bench/classes/{baseline,new}/`。

## 6. JVM 实验

```bash
python3 bench/run_benchmarks.py
```

- 读取：`-Xmx256m`，10k/25k/50k/100k/200k 行，各 3 次独立 JVM；
- 计算：`-Xmx1g`，50k 行，各 3 次；连接为 5k × 5k；
- 结果：`bench/results/raw.json`、`summary.csv`、`read_scaling.csv`、`operations.csv`。

## 7. pandas 对比

```bash
python3 bench/run_pandas_benchmarks.py
python3 bench/compare_pandas.py
```

pandas 2.1.0 / CPython 3.10，`read_csv(engine="c")`；每次测量使用独立 Python 进程，
读取 10k 预热后读取目标文件，3 次取中位数。结果写入
`bench/results/pandas_summary.csv` 与 `pandas_comparison.csv`。

## 8. 图表

```bash
python3 bench/plot_results.py
```

输出 `bench/results/fig_*.png`；报告中的双面板图由 `report/make_report_figures.py` 生成。

## 9. Android 真机实验（adb）

`android-pandas/src/androidTest/.../CsvBenchInstrumentedTest.kt` 在设备上生成 15 列 CSV
（10k–200k 行），通过 logcat 标签 `ANDAS_BENCH` 输出读取耗时和 GC 后存活堆。

真机脚本**不经过 Gradle connected test**，而是构建 test APK 后直接 `adb install` +
`am instrument`；默认拒绝模拟器（`ro.kernel.qemu == 1`）：

```bash
export ANDROID_SERIAL=R8U8R8TGPZKFT8JB   # 你的手机 serial
./bench/run_android_adb.sh
```

脚本产出：

- `bench/results/android_<MODEL>_adb.log`：原始 logcat；
- `bench/results/android_physical.csv`：解析后的表；
- `bench/results/android_adb_instrument.txt`：instrumentation 输出。

本次设备（adb 读取）：Xiaomi M531DA / MediaTek MT6899 / Android 17 (API 37) /
11.6 GB RAM / 1268×2756 @520 dpi / 256 MB test heap。

本次结果（M531DA / Android 17 / API 37 / 256 MB）：

| rows | ms | retained MB |
|---:|---:|---:|
| 10k | 346 | 1 |
| 25k | 484 | 4 |
| 50k | 957 | 8 |
| 100k | 1851 | 18 |
| 200k | 3661 | 35 |

基线的 Android 数据：把同一个测试文件复制到 `.baseline/android-pandas/src/androidTest/`
后，在 `.baseline/` 目录执行同样的 Gradle 命令。

## 10. 一致性测试（pandas ↔ JVM ↔ Android）

一致性数据集为 `bench/data/consistency_5000_c15.csv`（从 50k 数据集截取前 5000 行），
并作为 `androidTest` asset 打包，保证三端读取完全相同的字节。

```bash
python3 bench/make_consistency_data.py      # 生成 5k 数据与 Android assets
python3 bench/consistency_pandas.py         # pandas 期望值 JSON

# JVM：先设置 KOTLIN_STDLIB 指向 kotlin-stdlib-2.0.21.jar
KOTLIN_STDLIB=/path/to/kotlin-stdlib-2.0.21.jar
javac -encoding UTF-8 -cp "android-pandas/build/tmp/kotlin-classes/debug:$KOTLIN_STDLIB" \
  -d bench/classes/consistency bench/java/Consistency.java
java -cp "bench/classes/consistency:android-pandas/build/tmp/kotlin-classes/debug:$KOTLIN_STDLIB" \
  Consistency bench/data/consistency_5000_c15.csv \
  bench/data/join_left_5000.csv bench/data/join_right_5000.csv \
  > bench/results/consistency_jvm.json

# Android：运行 instrumentation test 并抓取 logcat
adb logcat -c
./gradlew :android-pandas:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=cn.ac.oac.libs.andas.ConsistencyInstrumentedTest
adb logcat -d -s ANDAS_CONSISTENCY:I > bench/results/android_consistency.log

python3 bench/consistency_compare.py
```

比较 35 项指标：行数/列数/计数等整数要求精确相等，浮点指标采用相对误差 `≤ 1e-10`。
输出：

- `bench/results/consistency_summary.csv`：逐项 pandas/JVM/Android 与误差；
- `bench/results/consistency_summary.json`：通过率与最大误差；
- `bench/results/android_consistency.log`：Android 原始 JSON。

当前结果：JVM 与 Android 均 35/35 通过，最大相对误差 `3.17e-15`。

## 11. 结果与注意事项

完整中文报告见 [BENCHMARK_REPORT.md](../BENCHMARK_REPORT.md)，英文 PDF 见
[report/andas-technical-report.pdf](../report/andas-technical-report.pdf)。

注意事项：

- JVM 基线经 try/catch 降级到 Kotlin 路径；Android 读取实验使用未修改的基线。
- pandas 运行在 CPython/C，Andas 运行在 JVM/ART；内存指标分别为 RSS 增量与 GC 后堆。
- 峰值堆由 50 ms 采样线程近似，受 GC 时机影响。
- 每次实验前请确认磁盘缓存已预热，避免 I/O 抖动影响读取时间。
