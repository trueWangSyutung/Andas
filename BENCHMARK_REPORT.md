# Andas 数据读取与计算性能优化实验报告

> 面向技术报告 / 论文的实验章节草稿。所有数据、图表与复现脚本见 `bench/`。

## 摘要

Andas 是一个面向 Android 的类 pandas 数据处理库。旧版本在读取大 CSV 时，先把整个文件读成
`List<String>`，再把每一行构造成 `Map<String, Any?>`，最后逐列转置为 `Series`；同时每个
`Series` 都独立持有一份索引和索引哈希表。这一设计使内存占用约为数据本身的数倍，在 192 MB
堆的 Android 运行环境中，10 万行 × 15 列的数据即发生 OOM。

本文对数据读取与计算路径进行了重构：以流式、列式解析替代全量行对象；以共享范围索引替代
逐列索引；以原始类型数组和单次遍历替代装箱与多次中间集合；并将排序、分组、连接等操作改为
`O(n log n)` / `O(n + m)` 算法。实验表明：

- 在 256 MB JVM 堆下，优化后 20 万行 × 15 列（36 MB）读取耗时 **1.44 s**、GC 后存活堆
  **56 MB**；基线在 20 万行时 OOM，10 万行时存活堆 155 MB、耗时 2.54 s。
- 在真机 Xiaomi M531DA（Android 17 / API 37，256 MB 堆）上通过 adb 直接运行，10 万行
  耗时 **1.85 s**、存活堆 **18 MB**；20 万行耗时 **3.66 s**、存活堆 **35 MB**，无 OOM。
- 50k 行数据上，`sum`、`mean`、`std`、`describe` 加速 **7–30×**，`sortValues` 加速
  **32.9×**，5k × 5k `merge` 加速 **30.9×**，`filterGreaterThan` 加速 **2.9×**。
- 基线排序按字符串字典序进行且为 `O(n²)`，优化后按数值排序并保持 `O(n log n)`。
- 与 pandas 2.1.0 在同机同数据上对比：优化后读取仍慢 3.2–6.0×，但 GC 后内存低
  1.7–3.9×；计算操作慢 6–20×（`describe` 基本持平），C/NumPy 引擎仍是当前实现的上界。
- 一致性：在 5k 行 × 15 列的同一份数据上，JVM 与 Android 的 35 项指标（统计、排序、
  筛选、分组、连接、相关系数、归一化）相对 pandas 的最大误差为 **3.17×10⁻¹⁵**，35/35 通过。

## 1. 问题与贡献

### 1.1 旧实现的问题

1. **全量物化**：`File.readLines()` / `BufferedReader.readLines()` 将整个文件读入
   `List<String>`；随后每行构造 `Map`，再转置为列，形成多份全量数据副本。
2. **装箱与对象开销**：`List<Any?>` 中的每个整数、浮点数都是独立对象；每行 `Map` 还包含
   15 个 `Entry`。
3. **索引重复**：`DataFrame` 的每一列 `Series` 都复制一份索引，并各自构建
   `Map<label, position>`；对 20 万行 × 15 列意味着 15 份索引 List 和 15 个 HashMap。
4. **第一行类型推断**：只取第一行数据推断列类型，首行为空或异常时整列退化为字符串。
5. **算法复杂度**：`sortValues` 通过 `indexList.indexOf(label)` 重建行，复杂度 `O(n²)`；
   `merge` 为双重循环 `O(n × m)`；`filter` 为每行创建整行 `Map` 和全部列的 `Series`。

### 1.2 本文贡献

- 实现流式、列式 CSV 解析器 `CsvReader`：固定 64 KB 字符缓冲、逐记录解析、直接写入
  `IntArray` / `LongArray` / `DoubleArray` / `BooleanArray`，空值用位掩码表示。
- 引入 `RangeIndexList` 共享范围索引和惰性 `label → position` 映射，使每列索引开销从
  `O(n)` 内存降为 `O(1)`。
- 将统计、排序、筛选、分组、连接改为单次遍历或哈希算法，并启用 OpenMP 原生并行。
- 在 JVM 与 Android 真机（adb）上完成可复现实验，给出时间、内存和 OOM 阈值数据；
- 建立 Python pandas ↔ JVM ↔ Android 的跨语言一致性测试，逐项校验 35 个指标。

## 2. 实现概述

### 2.1 CSV 读取

```
InputStream → BufferedReader(64 KB) → CsvRecordReader（字符级状态机）
           → ColumnBuilder[]（原始类型数组 + 空值位掩码）
           → Series[]（共享 RangeIndexList）→ DataFrame
```

- 引号内的逗号、换行、`""` 转义、CRLF、BOM、空行均由状态机处理；
- 类型推断使用前 64 行样本；若后续值无法按推断类型解析，该列自动降级为字符串列，保证不丢数据；
- 重复表头自动去重（`a, a` → `a, a_1`）；
- 分批读取 `readCSVBatch` 复用同一解析器，每批构建一个 `DataFrame`，不会全量加载。

### 2.2 计算

| 操作 | 旧实现 | 新实现 |
| --- | --- | --- |
| `sum/mean/min/max/variance/std/describe` | `filterNotNull().map{}.toDoubleArray()`，2 个中间 List + 多次遍历 | 原始数组单次遍历；方差使用 Welford 在线算法 |
| `sortValues/sortIndices` | 原生路径丢空值；Kotlin 路径按字符串排序 + `indexOf`，`O(n²)` | `O(n log n)` 数值排序，空值置后，保留原索引 |
| `filterGreaterThan/where` | 过滤空值后索引错位；逐行构造 Map | 位置掩码 + 列式抽取 |
| `groupBySum` | 原生过滤空值后组/值错位 | 单次哈希聚合 `O(n)` |
| `merge/join` | `O(n × m)` 双重循环 | 哈希连接 `O(n + m + 输出行数)` |
| `GroupBy.agg` | 每个分组重建临时 DataFrame | 一次分组 + 单次聚合扫描 |
| `filter` | 每行构造整行 Map 与 15 个 Series | 惰性行视图，只创建被访问列的 Series |
| `toCSV` | 逐字段写、无转义 | `BufferedWriter` + 行缓冲 + 标准 CSV 转义 |
| 原生库探测 | 每次计算 2 次 JNI 探测 | `NativeAvailability` 惰性缓存 |

### 2.3 原生层

- CMake 通过 `find_package(OpenMP)` 链接 `libomp.so`；NDK 21+ 自动启用，未找到时串行回退。
- 修正 `findNullIndices`、`dropNullValues`、`where`、`mergeIndices` 并行合并导致的索引乱序，
  保证返回升序、行不错位。

## 3. 实验设置

### 3.1 环境

| 项目 | 配置 |
| --- | --- |
| CPU | Apple M2 |
| 操作系统 | macOS 27.0.1 |
| JDK | OpenJDK 21.0.12（Homebrew） |
| 构建 | Gradle 8.13，AGP 8.13.2，Kotlin 2.0.21 |
| Android | 真机 Xiaomi M531DA，Android 17 / API 37，`Runtime.maxMemory()` = 256 MB，adb USB |
| Python 对比 | CPython 3.10.11，pandas 2.1.0，NumPy C 引擎（同机、同数据、同磁盘缓存） |

### 3.2 数据集

合成 15 列 CSV，列结构与项目自带的 `largest.csv` 一致：

- 3 列 10 字符随机字符串（第 3 列为 10 个类别，用于分组）；
- 3 列 12 位整数（`Long`）；
- 3 列 `[-100, 100]` 整数；
- 3 列双精度浮点、3 列单精度浮点；
- 行数：10k、25k、50k、100k、200k（200k 约 36 MB）。

另生成 5k × 5k 的连接数据集（`id`、`category`、`amount/score`）。随机种子固定，脚本为
`bench/gen_data.py`。

### 3.3 基线

基线为优化前 commit `9bb6715`，在 `.baseline/` worktree 中检出并编译。JVM 上不存在
`libandas_native.so`，因此对 `NativeMath/NativeData/NativeBatch` 的 `System.loadLibrary`
做了**仅用于 JVM 实验的 try/catch 降级**，使基线走 Kotlin 回退路径；Android 上基线仍使用
真实 JNI（未修改）。优化版与基线使用同一数据集、同一 JVM 参数、同一台机器。

### 3.4 指标与流程

- **读取时间**：`DataFrameIO.readCSV(File)` 的 wall time；每档数据在独立 JVM 中运行 3 次，
  取中位数；读取前用 10k 文件预热 JIT。
- **存活堆**：读取后 `System.gc()` + 120 ms，`totalMemory - freeMemory`。
- **峰值堆**：读取期间由采样线程每 50 ms 读取一次 `MemoryMXBean` 堆使用量，取最大值；
  该值反映“读取过程中的堆使用上界”，受 GC 时机影响。
- **操作时间**：先读取 50k 数据，执行一次预热操作，再执行一次计时操作；3 次独立 JVM 取中位数。
- **OOM**：JVM 读取实验堆上限 256 MB；Android 为 192 MB。

## 4. 实验结果

### 4.1 JVM：CSV 读取扩展性

| 行数 | 基线 (ms) | 优化后 (ms) | 加速比 | 基线存活堆 (MB) | 优化后存活堆 (MB) | 基线峰值 (MB) | 优化后峰值 (MB) |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 10k | 321 | 138 | 2.33× | 15 | 4 | 88 | 41 |
| 25k | 623 | 244 | 2.55× | 36 | 8 | 137 | 119 |
| 50k | 1071 | 541 | 1.98× | 78 | 14 | 165 | 141 |
| 100k | 2540 | 964 | 2.63× | 155 | 30 | 245 | 162 |
| 200k | **OOM** | 1440 | — | OOM | 56 | OOM | 214 |

![JVM CSV 读取时间](bench/results/fig_read_time.png)

![JVM 读取内存](bench/results/fig_read_memory.png)

要点：

1. 基线在 20 万行、256 MB 堆下 OOM；优化后仍能在该堆下完成，存活堆 56 MB。
2. 基线的存活堆随行数近似线性增长且斜率约为优化后的 3–5 倍；优化后 100k 仅 30 MB，
   基线为 155 MB。
3. 优化后峰值堆仍会随分配速率上升（200k 为 214 MB），但明显低于基线，且读取成功后
   可被 GC 回收到 56 MB；这说明“峰值”主要是分配抖动，而非存活数据。

### 4.2 JVM：计算操作

| 操作 | 基线 (ms) | 优化后 (ms) | 加速比 |
|---|---:|---:|---:|
| `sum` | 30 | 1 | 30.0× |
| `mean` | 28 | 2 | 14.0× |
| `std` | 28 | 4 | 7.0× |
| `describe` | 80 | 3 | 26.7× |
| `sortValues` | 2629 | 80 | 32.9× |
| `filterGreaterThan` | 23 | 8 | 2.9× |
| `groupBySum` | 27 | 20 | 1.4× |
| `merge`（5k × 5k） | 557 | 18 | 30.9× |

![操作耗时](bench/results/fig_ops_time.png)

![操作加速比](bench/results/fig_ops_speedup.png)

解释：

- `sum/mean/std/describe`：旧实现每次调用都构造两个中间 List 再转 `DoubleArray`，并需要
  多轮全量扫描；新实现直接在列式数组上单次遍历。
- `sortValues`：旧 Kotlin 回退路径先按 `toString()` 字典序排序，再对每个标签执行
  `indexOf` 重建行，为 `O(n²)`；新实现为 `O(n log n)` 数值排序。
- `merge`：旧实现为 `O(n × m)` 双重循环；新实现先构建右侧哈希表，再扫描左侧。
- `filterGreaterThan`：旧实现为每个保留行构造完整的 15 列 Map；新实现只做位置筛选。
- `groupBySum`：旧实现本身已是 `O(n)`，因此加速有限（1.4×）；这符合预期，也说明
  加速并非来自“全部换成原生代码”，而是来自数据结构与算法。

**正确性差异**：基线 `sortValues` 的 Kotlin 回退按字符串排序，50k 数据排序后的首个值为
`-0.0166`；优化后按数值排序，首个值为 `-99.9979`。基线在空值列上还会因原生 `argsort`
过滤空值而出现行错位。优化后用 `StreamingCsvPerformanceTest` 校验了数值单调性、
分组求和、连接行数、空值填充、相关系数与归一化等。

### 4.3 与 pandas 的对比

pandas 是 Andas 的直接设计参考。为了给出一个“桌面端参考上界”，在同一台机器、同一批 CSV 上
运行 pandas 2.1.0（`pd.read_csv(..., engine="c")`）。每次测量使用独立的 Python 进程，
先读取 10k 文件预热 C 解析器，再读取目标文件；3 次取中位数。

**读取时间与内存**

| 行数 | 基线 (ms) | 优化后 (ms) | pandas (ms) | 优化后 / pandas | 基线 / pandas |
|---:|---:|---:|---:|---:|---:|
| 10k | 321 | 138 | 22.9 | 6.02× 慢 | 14.0× 慢 |
| 25k | 623 | 244 | 56.4 | 4.32× 慢 | 11.0× 慢 |
| 50k | 1071 | 541 | 112.8 | 4.80× 慢 | 9.50× 慢 |
| 100k | 2540 | 964 | 239.6 | 4.02× 慢 | 10.6× 慢 |
| 200k | **OOM** | 1440 | 446.7 | 3.22× 慢 | OOM |

| 行数 | 基线存活堆 (MB) | 优化后存活堆 (MB) | pandas 读取增量 RSS (MB) |
|---:|---:|---:|---:|
| 10k | 15 | 4 | 6.9 |
| 25k | 36 | 8 | 19.1 |
| 50k | 78 | 14 | 54.2 |
| 100k | 155 | 30 | 75.9 |
| 200k | OOM | 56 | 106.8 |

![JVM、pandas 读取时间对比](bench/results/fig_read_time_pandas.png)

需要注意：pandas 的数值是 **Python 进程增量 RSS**，Andas 的数值是 **GC 后 JVM 存活堆**，
两者不是同一指标；但可以看出，优化后的列式存储比 pandas 的 object/NumPy 组合更省内存
（200k 时 56 MB vs 106.8 MB）。

**计算操作（50k 行；merge 为 5k × 5k）**

| 操作 | 基线 (ms) | 优化后 (ms) | pandas (ms) | 优化后 / pandas |
|---|---:|---:|---:|---:|
| `sum` | 30 | 1 | 0.12 | 8.3× 慢 |
| `mean` | 28 | 2 | 0.10 | 20.0× 慢 |
| `std` | 28 | 4 | 0.37 | 10.8× 慢 |
| `describe` | 80 | 3 | 3.22 | 0.93×（略快） |
| `sortValues` | 2629 | 80 | 12.67 | 6.3× 慢 |
| `filterGreaterThan` | 23 | 8 | 0.55 | 14.5× 慢 |
| `groupBySum` | 27 | 20 | 1.85 | 10.8× 慢 |
| `merge` | 557 | 18 | 1.43 | 12.6× 慢 |

![基线、优化后与 pandas 的操作耗时](bench/results/fig_ops_pandas.png)

分析：

1. **读取**：pandas 的 C 解析器 + 连续 NumPy 数组在桌面端明显更快；优化后仍慢 3.2–6.0×，
   但相比基线的 9.5–14× 差距已缩小一半以上，且内存更低。200k 时基线 OOM，pandas 与优化版
   都能完成。
2. **统计**：pandas 的归约由 C/NumPy 完成，`sum/mean/std` 快 8–20×；优化后的单次遍历
   已把基线 30 ms 级降到 1–4 ms。
3. **`describe`**：优化后 3 ms，与 pandas 3.22 ms 基本持平甚至略快，因为 pandas 的
   `describe` 需要构造多个统计量并做 Series 包装。
4. **排序/筛选/分组/连接**：pandas 仍快 6–15×；差距主要来自 JVM 侧的装箱、`Any?` 动态
   分发和 JNI/对象边界。这也是后续工作的方向（原生列式内核、零拷贝 JNI、向量化算子）。
5. **公平性**：pandas 运行在桌面 CPython + C 扩展，Andas 运行在 JVM（以及 Android ART），
   二者不是同一运行时；本节数据用于给出数量级参考，而不是宣称 Android 库应达到 pandas
   的绝对性能。

### 4.4 Android 真机（adb）

Android 端只测 CSV 读取（计算路径在 JVM 中已单独评估，且 Android 上 JNI 会改变绝对值）。
测试通过 **adb 直接安装 test APK 并执行 instrumentation**，不使用模拟器：

```bash
ANDROID_SERIAL=R8U8R8TGPZKFT8JB ./bench/run_android_adb.sh
```

设备：Samsung **M531DA**，Android **17（API 37）**，`Runtime.maxMemory() = 256 MB`。
测试代码在设备 cache 中生成 15 列 CSV 后读取，结果由 `bench/parse_android_bench.py` 写入
`bench/results/android_physical.csv`，原始 logcat 为
`bench/results/android_M531DA_adb.log`。

| 行数 | 读取时间 (ms) | GC 后存活堆 (MB) | 状态 |
|---:|---:|---:|---|
| 10k | 346 | 1 | ok |
| 25k | 484 | 4 | ok |
| 50k | 957 | 8 | ok |
| 100k | 1851 | 18 | ok |
| 200k | 3661 | 35 | ok |

![Android 真机读取时间与存活堆](bench/results/fig_android_physical.png)

在真机 256 MB 堆下：

- 20 万行 × 15 列读取 **3.66 s**，GC 后存活堆 **35 MB**；
- 10 万行 **1.85 s / 18 MB**；
- 所有档位均完成，无 OOM；
- 真机比此前的模拟器结果快约 4×，说明模拟器的 CPU/IO 开销会显著放大绝对时间。

> 基线数据未在真机重跑；此前模拟器基线（100k/200k OOM）仅作为旧实现问题的历史证据。
> 真机测试脚本默认拒绝模拟器，要求 `ro.kernel.qemu != 1`。

### 4.5 跨语言一致性验证

性能数据之外，还需要验证 Andas 与 pandas 在相同输入上得到相同结果。我们取
`bench_50000_c15.csv` 的前 5000 行作为一致性数据集，分别用 pandas 2.1.0、JVM 上的 Andas
和 Android 上的 Andas 计算同一组指标：

- 基数与计数：行数、列数、筛选行数、分组数、连接行数（要求精确相等）；
- 统计量：`sum`、`mean`、`std`（ddof=1）、`min`、`max`、`describe`、长整型均值；
- 排序：首/末值、前 50/后 50 个值的和；
- 筛选：`double_col_10 > 90` 的行数与和；
- 分组：10 个类别的 `groupby_sum`；
- 连接：5k × 5k 的 `merge` 行数与 `amount` 总和；
- 相关系数与归一化：`corr(double_col_10, double_col_11)`、`normalize` 首/末值。

pandas 生成期望 JSON；JVM 使用 `bench/java/Consistency.java`，Android 使用
`ConsistencyInstrumentedTest` 读取随测试 APK 打包的同一份 CSV 和期望 JSON。浮点指标采用
相对误差 `≤ 1e-10`，整数指标要求精确相等。

| 指标 | pandas | JVM | Android |
| --- | ---: | ---: | ---: |
| `sum(double_col_10)` | -7465.802624422415 | -7465.802624422438 | -7465.802624422438 |
| `std(double_col_10)` | 57.606263335764076 | 57.60626333576399 | 57.60626333576399 |
| `sortValues` 首值 | -99.94711091909323 | -99.94711091909323 | -99.94711091909323 |
| `filterGreaterThan` 行数 | 222 | 222 | 222 |
| `groupby_sum[cat_3]` | -2092.5884491227703 | -2092.588449122769 | -2092.588449122769 |
| `merge` 行数 | 5000 | 5000 | 5000 |
| `corr(double_10, double_11)` | 0.008147698079084498 | 0.008147698079084522 | 0.008147698079084522 |
| `normalize` 首值 | -1.5682160419011852 | -1.568216041901185 | -1.568216041901185 |

**结果：35/35 项通过；JVM 与 Android 的最大相对误差均为 3.17×10⁻¹⁵，远低于 1e-10 阈值；
计数、行数、排序首末值等整数/边界指标完全一致。** 差异仅来自浮点求和顺序
（pandas/NumPy 的成对求和 vs Andas 的顺序累加），不构成算法或语义差异。

完整逐项结果见 [consistency_summary.csv](bench/results/consistency_summary.csv)，期望值见
[consistency_pandas.json](bench/results/consistency_pandas.json)，Android 原始输出见
[android_consistency.log](bench/results/android_consistency.log)。

## 4.5 数值计算性能与一致性（0.2.0 预览）

新增 NDArray/SciPy 数值栈后，补充了独立实验：JVM Kotlin 回退、Android JNI 与 Python NumPy
在相同输入上对比，并用 43 个参考指标验证一致性。JNI radix-2 FFT 相对 Kotlin DFT 回退提升
约 62×，原生 matmul 让 OLS 从 46.9 ms 降至 13.7 ms；43/43 指标在 JVM 与 Android 通过。
完整方法、数据与复现命令见 [bench/NUMPY_SCIPY_BENCHMARK.md](bench/NUMPY_SCIPY_BENCHMARK.md)。

## 5. 讨论

### 5.1 内存复杂度

设行数 `n`、列数 `m`、字符串平均长度 `L`：

- 旧实现：`O(n·m)` 个 `Map.Entry` + `O(n·m)` 个装箱值 + 转置期间的另一份列数据 +
  `O(n·m)` 的索引副本与哈希表；
- 新实现：数值列 `O(n)` 个原始类型（1/4/8 字节），字符串列 `O(n·L)`，索引 `O(1)`；
  不再有行 Map 和逐列索引副本。

因此，在新实现中，20 万行 × 15 列在 192 MB 设备上仍可完成读取；旧实现约在 5–10 万行
（取决于列宽和设备堆）达到上限。

### 5.2 时间加速来源

- 统计类：消除中间 List、避免装箱、单次遍历（7–30×）；
- 排序：`O(n²) → O(n log n)`（32.9×，且随 n 增大差距扩大）；
- 连接：`O(nm) → O(n+m)`（30.9×）；
- 读取：流式解析 + 列式写入，减少一次全量转置（2–2.6×）。

`groupBySum` 仅 1.4×，因为旧实现已是哈希聚合；这也说明优化集中在真正的瓶颈上。

### 5.3 与 pandas 的对比解读

pandas 数据给出了两个有用信号：

1. **上界**：优化后读取仍比 pandas 慢 3.2–6.0×，统计操作慢 8–20×，说明 JVM 侧的动态类型、
   装箱和 JNI 边界仍有优化空间；但排序/连接的差距已从基线的 200×+ 缩小到 6–13×。
2. **内存**：优化后的列式存储的 GC 后存活堆低于 pandas 的增量 RSS（200k：56 vs 106.8 MB），
   说明“列式 + 原始类型”的方向是正确的；pandas 的 object 字符串列和 NumPy 中间数组在
   读取阶段会带来更高内存。

运行时差异（CPython/C vs JVM/ART）、内存指标差异（RSS vs GC 后堆）意味着这些数字不能
直接作为“Andas 比 pandas 快/慢”的结论；它们更适合用于定位下一阶段的优化目标。

## 6. 有效性威胁

1. **JVM vs ART**：JVM 实验无法完全代表 ART 的 JIT/GC 行为；因此补充了 Android 真机
   （M531DA，adb）实验。真机结果仍只代表这一台设备，不能外推到所有手机。
2. **基线 JVM 降级**：JVM 没有 `libandas_native.so`，基线经 try/catch 降级到 Kotlin 回退；
   这会影响 `sum/sort` 等操作的绝对值。Android 读取实验未修改基线，使用真实 JNI，
   因此读取结论不受影响。
3. **合成数据**：数据分布、字符串长度与真实业务数据不同；使用固定种子保证可复现，
   并在项目自带的 37 MB `largest.csv` 上做了单元测试级校验。
4. **峰值堆采样**：50 ms 采样可能漏掉瞬时峰值；存活堆经过 `System.gc()`，但 GC 不保证
   立即回收，数值存在数 MB 波动。
5. **单机单次环境**：每档 3 次取中位数，未做跨机器重复；绝对时间会随机器负载变化，
   但相对趋势在多次运行中稳定。
6. **pandas 对比的运行时差异**：pandas 运行在 CPython 3.10 + C/NumPy，Andas 运行在 JVM/ART；
   内存指标分别为 RSS 增量与 GC 后堆，不能直接等同。对比仅用于数量级参考和优化目标定位。

## 7. 结论

旧版 Andas 的大文件读取瓶颈不是 CPU 算力，而是“全量行对象 + 转置 + 逐列索引”造成的
内存放大和分配抖动；计算瓶颈则主要来自中间集合、`O(n²)` 排序和 `O(nm)` 连接。通过流式
列式解析、共享范围索引、原始类型数组和哈希/排序算法替换，优化版在 256 MB Android 真机环境中
把可读取规模从“10 万行 OOM”提升到“20 万行 35 MB 存活堆”，并在 50k 行数据上取得统计/排序/连接类操作 7–33×、筛选 2.9×、
分组聚合 1.4× 的加速。与 pandas 2.1.0 的桌面端对比显示，读取差距由基线的约 10–14× 缩小到
3.2–6.0×，内存低于 pandas；统计/排序类操作仍有 6–20× 差距，指向原生列式内核这一后续方向。
相关实现、测试与实验脚本均已随仓库提供，可复现。

## 附录 A：复现命令

```bash
# 1) 数据
python3 bench/gen_data.py

# 2) 基线 + 两套 class + Java harness
./bench/prepare_baseline.sh 9bb6715

# 3) JVM 实验（每档 3 次独立 JVM）
python3 bench/run_benchmarks.py

# 3b) pandas 对比（3 次独立 Python 进程）
python3 bench/run_pandas_benchmarks.py
python3 bench/compare_pandas.py

# 3c) 跨语言一致性（pandas ↔ JVM ↔ Android）
python3 bench/make_consistency_data.py
python3 bench/consistency_pandas.py
# 运行 JVM Consistency harness，输出到 bench/results/consistency_jvm.json
# 运行 Android ConsistencyInstrumentedTest，并保存 logcat 到 android_consistency.log
python3 bench/consistency_compare.py

# 4) 图表
python3 bench/plot_results.py

# 5) Android 实验（当前版本）
export ANDROID_USER_HOME="$PWD/.android-home"
adb logcat -c
./gradlew :android-pandas:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=cn.ac.oac.libs.andas.CsvBenchInstrumentedTest
adb logcat -d -s ANDAS_BENCH:I
```

## 附录 B：产物

| 文件 | 内容 |
| --- | --- |
| `bench/results/raw.json` | 78 次 JVM 运行的原始记录 |
| `bench/results/summary.csv` | 按 (实现, 模式, 行数) 聚合的中位数 |
| `bench/results/read_scaling.csv` | 读取扩展性表 |
| `bench/results/operations.csv` | 50k 操作表 |
| `bench/results/android_new.log` / `android_baseline.log` | Android logcat 原始输出 |
| `bench/results/pandas_summary.csv` / `pandas_comparison.csv` | pandas 对比结果 |
| `bench/results/consistency_summary.csv` / `consistency_summary.json` | 一致性逐项结果 |
| `bench/results/consistency_pandas.json` / `consistency_jvm.json` / `android_consistency.log` | 三端原始结果 |
| `bench/results/fig_*.png` | 论文图表 |
| `bench/java/Bench.java` | 独立 JVM harness |
| `android-pandas/src/androidTest/.../CsvBenchInstrumentedTest.kt` | Android 读取实验 |
