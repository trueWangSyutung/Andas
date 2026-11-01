# Andas 性能实验复现

本目录包含技术报告《Andas 数据读取与计算性能优化》的全部实验脚本、原始结果和图表。

## 目录

```
bench/
├── gen_data.py                 # 生成 15 列、1w~20w 行的合成 CSV
├── prepare_baseline.sh         # 检出优化前版本、编译两套 class 与 Java harness
├── run_benchmarks.py           # 运行 JVM 基准矩阵（每档 3 次，取中位数）
├── plot_results.py             # 生成论文图表
├── java/Bench.java             # 独立 JVM 基准入口（不依赖 JUnit/Android）
├── results/                    # raw.json / summary.csv / 图表
└── data/                       # 生成的 CSV（已 gitignore）
```

## 1. 生成数据

```bash
python3 bench/gen_data.py
```

生成 `bench_10000_c15.csv` … `bench_200000_c15.csv`，以及 5k × 5k 的
`join_left_5000.csv` / `join_right_5000.csv`。数据生成使用固定随机种子，可复现。

## 2. 编译基线与优化版

```bash
# 优化前基线：commit 9bb6715（可替换为任意优化前 commit）
./bench/prepare_baseline.sh 9bb6715
```

脚本会：

1. 在 `.baseline/` 检出优化前代码；
2. 让 `NativeMath/NativeData/NativeBatch` 在 JVM 上加载不到 `libandas_native.so`
   时安全降级（Android 上仍正常使用 JNI）；
3. 分别编译基线与当前版本的 Kotlin class；
4. 编译 `bench/java/Bench.java` 到 `bench/classes/{baseline,new}/`。

## 3. 运行 JVM 实验

```bash
python3 bench/run_benchmarks.py
```

- 读取实验：`-Xmx256m`，10k/25k/50k/100k/200k 行，各 3 次独立 JVM；
- 计算实验：`-Xmx1g`，50k 行，各 3 次；merge 为 5k × 5k；
- 结果写入 `bench/results/raw.json`、`summary.csv`、
  `read_scaling.csv`、`operations.csv`。

## 4. 生成图表

```bash
python3 bench/plot_results.py
```

输出 `bench/results/fig_*.png`。

## 5. Android 真机/模拟器实验

`android-pandas/src/androidTest/.../CsvBenchInstrumentedTest.kt` 会在设备上生成同样的
15 列 CSV（10k~200k 行），通过 logcat 标签 `ANDAS_BENCH` 输出读取耗时和 GC 后存活堆。

```bash
export ANDROID_USER_HOME="$PWD/.android-home"   # 避免写入 ~/.android
adb logcat -c
./gradlew :android-pandas:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=cn.ac.oac.libs.andas.CsvBenchInstrumentedTest
adb logcat -d -s ANDAS_BENCH:I
```

基线的 Android 数据：把同一个测试文件复制到 `.baseline/android-pandas/src/androidTest/`
后，在 `.baseline/` 目录执行同样的 Gradle 命令。

## 6. 环境

- Apple M2 / macOS 27.0.1 / JDK 21.0.12 / Gradle 8.13
- Android emulator：API 36 arm64，`Runtime.maxMemory()` = 192 MB
- 基线与优化版使用同一台机器、同一 JVM 参数、同一数据集

详细数据与结论见根目录 [BENCHMARK_REPORT.md](../BENCHMARK_REPORT.md)。
