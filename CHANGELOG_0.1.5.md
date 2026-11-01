# Andas 0.1.5 性能与部署更新

## 发布日期

2026 年 11 月 1 日

## 一、CSV 读取重构（解决大文件无法读取）

旧实现的问题：

1. `File.readLines()` / `BufferedReader.readLines()` 先把整个文件读成 `List<String>`；
2. 每行再构造成 `Map<String, Any?>`，产生大量 `Map.Entry`、装箱对象和临时 List；
3. `DataFrame(rows)` 再逐列转置，内存峰值约为文件字符量的数倍；
4. 每个 `Series` 各自持有一份索引 List 和一个索引 → 位置 HashMap；
5. 类型推断只使用第一行数据，首行为空时整列退化为字符串。

新实现 `CsvReader`：

- **流式解析**：固定 64K 字符缓冲，逐条记录读取，内存占用与文件大小无关；
- **列式构建**：直接写入 `IntArray` / `LongArray` / `DoubleArray` / `BooleanArray`，
  只有字符串列才创建对象；空值用位掩码表示；
- **共享范围索引**：`RangeIndexList` 以 O(1) 内存表示 `0..n-1`，所有列共享同一个索引；
- **惰性索引映射**：只有按标签访问时才构建 `label -> position` 的 HashMap；
- **多行采样类型推断**：最多 64 行样本，遇到后续无法解析的值自动降级为字符串列；
- **健壮的 CSV 语法**：支持引号内逗号、换行、`""` 转义、CRLF、BOM、空行；
- **重复表头自动去重**（`a, a` → `a, a_1`），避免数据被覆盖。

`DataFrameIO.readCSV` / `readFromAssets` / `BatchCSVUtils.readCSVBatch` 全部复用同一套流式解析器。

## 二、计算性能优化

| 操作 | 旧实现 | 新实现 |
| --- | --- | --- |
| `sum/mean/min/max/variance/std/describe` | `filterNotNull().map{...}.toDoubleArray()` 两次分配 + 多次遍历 | 原始数组单次遍历，Welford 方差 |
| `sortValues/sortIndices` | 原生路径丢掉空值导致行错位；Kotlin 路径 `indexOf` 造成 O(n²) | O(n log n) 排序 + 保序抽取，保留原索引 |
| `filterGreaterThan/where` | 原生路径过滤空值后索引错位 | 位置掩码 + 列式抽取 |
| `groupBySum` | 原生过滤空值后组/值错位 | O(n) 哈希聚合 |
| `merge/join` | O(n × m) 双重循环 | O(n + m + 结果行数) 哈希连接 |
| `GroupBy.agg` | 每个分组重建临时 DataFrame，O(分组数 × 行数 × 列数) | 一次分组 + 单次聚合扫描 |
| `filter` | 每行构造整行 Map 和全部列的 Series | 惰性行视图，按访问列创建单值 Series |
| `dropna/isnull/notnull/fillna` | 行 Map + 装箱 | 位置/位掩码 + 原始数组 |
| `toCSV` | 逐字段 `FileWriter.write`，不转义 | `BufferedWriter` + 行缓冲 + 标准 CSV 转义 |
| 原生库探测 | 每次计算调用两次 JNI 探测 | `NativeAvailability` 惰性缓存一次 |

## 三、原生层

- CMake 启用 OpenMP（`find_package(OpenMP)`，NDK 21+ 自动链接 `libomp.so`）；
  找不到时自动回退串行，不影响构建。
- 修正 `findNullIndices`、`dropNullValues`、`where`、`mergeIndices` 的并行合并顺序问题，
  保证返回索引升序、行不错位。
- `NativeMath.isAvailable` / `NativeData.isAvailable` 捕获 `Throwable`，JVM 单测可安全降级。

## 四、实测数据（含 pandas 对比）

测试环境：macOS + JDK 21，`largest.csv`（37 MB，200000 行 × 15 列），
单元测试 JVM 堆上限 **256 MB**：

```
largest.csv 读取: (200000, 15)，耗时 1559 ms，堆增长 96.8 MB
largest.csv 分批读取: 200000 行 / 4 批，耗时 2117 ms
```

旧实现在同样 37 MB 文件上需要先把文件读成 20 万个字符串并构造 20 万个
15 键 Map，再转置为列，256 MB 堆下无法稳定完成。

与 pandas 2.1.0（CPython 3.10，`read_csv(engine="c")`）同机对比：优化后读取仍慢
3.2–6.0×，但 GC 后内存更低（200k：56 MB vs 106.8 MB 增量 RSS）；`sum/mean/std`
慢 8–20×，`describe` 基本持平，排序/筛选/分组/连接慢 6–15×。完整数据见
`BENCHMARK_REPORT.md` 与 `bench/results/pandas_comparison.csv`。

复现命令：

```bash
./gradlew :android-pandas:testDebugUnitTest \
  --tests "cn.ac.oac.libs.andas.StreamingCsvPerformanceTest"
```

## 五、行为变化说明

- 数值列按数值大小排序，不再按字符串排序；空值始终排在最后。
- `sortValues` / `filter` / `sample` / `dropna` 保留原始索引标签（更接近 pandas）。
- `variance` / `std` 使用样本方差（ddof=1），`describe` 的 `std` 同样为样本标准差。
- CSV 空值判定仍支持 `""`、`null`、`NULL`、`NA`、`N/A`，可通过参数自定义。
- 单元格中的单引号不再被当作 CSV 引号，只有双引号参与引号转义。
- 单测堆上限设为 256 MB，用于防止 CSV 读取回退为全量加载。

## 六、GitHub Pages 默认域名

- 已移除 `trueWangSyutung/trueWangSyutung.github.io` 仓库的 `keyanle.me` 自定义域名绑定
  （CNAME 文件与 Pages 设置均已清除）。
- Andas 文档站恢复默认地址：<https://truewangsyutung.github.io/Andas/>。
- 由于自定义域名是账号级（用户站点）设置，移除后该账号下所有项目 Pages 均使用默认域名。
