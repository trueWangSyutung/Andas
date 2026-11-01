# 实验复现指南

本文档说明如何复现《Andas 数据读取与计算性能优化》中的全部实验，包括 JVM 基准、
pandas 2.1.0 对比、Android instrumentation 测试与图表生成。原始数据、图表和脚本均随仓库提供。

## 1. 实验范围

| 实验 | 内容 | 产出 |
| --- | --- | --- |
| CSV 读取扩展性 | 10k–200k 行 × 15 列，固定 256 MB 堆 | 时间、GC 后堆、峰值堆、OOM |
| 计算内核 | 50k 行上的统计、排序、筛选、分组、连接 | 操作耗时与加速比 |
| pandas 对比 | 同机、同数据、CPython 3.10 + pandas 2.1.0 | 读取/计算对比表 |
| Android | API 36 ARM64 模拟器，192 MB 堆 | 读取时间与存活堆 |
| 图表 | 论文用折线图、柱状图 | `results/fig_*.png` |

## 2. 环境

- Apple M2 / macOS 27.0.1 / JDK 21.0.12 / Gradle 8.13
- Python 3.10.11 / pandas 2.1.0 / psutil
- Android emulator：API 36 ARM64，`Runtime.maxMemory() = 192 MB`
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

## 9. Android 实验

`android-pandas/src/androidTest/.../CsvBenchInstrumentedTest.kt` 会在设备上生成同样的
15 列 CSV（10k–200k 行），通过 logcat 标签 `ANDAS_BENCH` 输出读取耗时和 GC 后存活堆。

```bash
export ANDROID_USER_HOME="$PWD/.android-home"   # 避免写入 ~/.android
adb logcat -c
./gradlew :android-pandas:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=cn.ac.oac.libs.andas.CsvBenchInstrumentedTest
adb logcat -d -s ANDAS_BENCH:I
```

基线的 Android 数据：把同一个测试文件复制到 `.baseline/android-pandas/src/androidTest/`
后，在 `.baseline/` 目录执行同样的 Gradle 命令。

## 10. 结果与注意事项

完整中文报告见 [BENCHMARK_REPORT.md](../BENCHMARK_REPORT.md)，英文 PDF 见
[report/andas-technical-report.pdf](../report/andas-technical-report.pdf)。

注意事项：

- JVM 基线经 try/catch 降级到 Kotlin 路径；Android 读取实验使用未修改的基线。
- pandas 运行在 CPython/C，Andas 运行在 JVM/ART；内存指标分别为 RSS 增量与 GC 后堆。
- 峰值堆由 50 ms 采样线程近似，受 GC 时机影响。
- 每次实验前请确认磁盘缓存已预热，避免 I/O 抖动影响读取时间。
