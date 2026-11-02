
# Andas SDK API 参考文档

## 目录

1. [SDK 初始化](#sdk-初始化)
2. [Series API](#series-api)
3. [DataFrame API](#dataframe-api)
4. [异步操作 API](#异步操作-api)
5. [JNI 原生 API](#jni-原生-api)
6. [线程池管理 API](#线程池管理-api)

---

## SDK 初始化

### Andas.initialize()

初始化 Andas SDK，必须在使用任何功能之前调用。

```kotlin
fun initialize(
    context: Context,
    configuration: AndasConfig.() -> Unit
)
```

**参数说明：**
- `context`: Android Context，用于获取应用上下文
- `configuration`: 配置块，用于设置 SDK 参数

**配置选项：**
```kotlin
Andas.initialize(this) {
    debugMode = true              // 是否开启调试模式
    logLevel = Andas.LogLevel.DEBUG  // 日志级别
    timeoutSeconds = 60L          // 异步操作超时时间（秒）
    cachePath = "andas_cache"     // 缓存路径（相对路径）
}
```

**日志级别：**
- `DEBUG`: 调试级别，输出详细日志
- `INFO`: 信息级别，输出关键信息
- `WARN`: 警告级别，输出警告信息
- `ERROR`: 错误级别，只输出错误信息
- `NONE`: 不输出任何日志

### Andas.getInstance()

获取 SDK 单例实例。

```kotlin
fun getInstance(): Andas
```

**返回值：** Andas SDK 单例实例

### destroy()

销毁 SDK，释放所有资源。

```kotlin
fun destroy()
```

**说明：** 
- 停止所有线程池
- 清理缓存文件
- 释放 Native 内存

---

## Series API

### 创建 Series

#### createSeries()

从列表创建 Series。

```kotlin
fun <T> createSeries(
    data: List<T>,
    name: String? = null
): Series<T>
```

**参数：**
- `data`: 数据列表，支持泛型类型
- `name`: Series 名称（可选）

**返回值：** Series 实例

**示例：**
```kotlin
val numbers = Andas.getInstance().createSeries(
    listOf(1, 2, 3, 4, 5),
    name = "numbers"
)
```

#### createSeriesFromMap()

从 Map 创建 Series。

```kotlin
fun <K, V> createSeriesFromMap(
    data: Map<K, V>,
    name: String? = null
): Series<V>
```

**参数：**
- `data`: Map 数据，键作为索引，值作为数据
- `name`: Series 名称（可选）

**返回值：** Series 实例

**示例：**
```kotlin
val series = Andas.getInstance().createSeriesFromMap(
    mapOf("a" to 10, "b" to 20, "c" to 30),
    name = "mapped"
)
```

### Series 属性

#### size()

获取 Series 长度。

```kotlin
fun size(): Int
```

#### name()

获取 Series 名称。

```kotlin
fun name(): String?
```

#### values()

获取所有值。

```kotlin
fun values(): List<T?>
```

#### indices()

获取所有索引。

```kotlin
fun indices(): List<Int>
```

### Series 数据访问

#### get()

通过索引访问数据。

```kotlin
operator fun get(index: Int): T?
```

**参数：**
- `index`: 索引位置

**返回值：** 对应位置的值，可能为 null

#### get()

通过键访问数据（适用于 Map 创建的 Series）。

```kotlin
operator fun get(key: Any): T?
```

**参数：**
- `key`: 键值

**返回值：** 对应键的值，可能为 null

#### head()

获取前 n 个元素。

```kotlin
fun head(n: Int = 5): Series<T>
```

**参数：**
- `n`: 元素数量，默认 5

**返回值：** 新的 Series，包含前 n 个元素

#### tail()

获取后 n 个元素。

```kotlin
fun tail(n: Int = 5): Series<T>
```

**参数：**
- `n`: 元素数量，默认 5

**返回值：** 新的 Series，包含后 n 个元素

### Series 数学运算

#### 乘法运算

```kotlin
operator fun times(other: Number): Series<Double>
```

**参数：**
- `other`: 乘数

**返回值：** 新的 Series，每个元素都乘以指定数值

**示例：**
```kotlin
val series = Andas.getInstance().createSeries(listOf(1, 2, 3))
val result = series * 2  // [2.0, 4.0, 6.0]
```

#### 加法运算

```kotlin
operator fun plus(other: Number): Series<Double>
```

**参数：**
- `other`: 加数

**返回值：** 新的 Series，每个元素都加上指定数值

### Series 空值处理

#### dropna()

移除所有空值。

```kotlin
fun dropna(): Series<T>
```

**返回值：** 不包含空值的新 Series

#### fillna()

用指定值填充空值。

```kotlin
fun fillna(value: T): Series<T>
```

**参数：**
- `value`: 填充值

**返回值：** 填充后的新 Series

**示例：**
```kotlin
val series = Andas.getInstance().createSeries(
    listOf(1, null, 3, null, 5)
)
val cleaned = series.fillna(0)  // [1, 0, 3, 0, 5]
```

### Series 统计功能

#### unique()

获取唯一值。

```kotlin
fun unique(): List<T>
```

**返回值：** 唯一值列表

#### valueCounts()

统计每个值的出现次数。

```kotlin
fun valueCounts(): Map<T, Int>
```

**返回值：** 值到出现次数的映射

**示例：**
```kotlin
val series = Andas.getInstance().createSeries(
    listOf("apple", "banana", "apple", "cherry")
)
val counts = series.valueCounts()
// {"apple" to 2, "banana" to 1, "cherry" to 1}
```

#### sum()

求和（仅适用于数值类型）。

```kotlin
fun sum(): Double
```

**返回值：** 所有元素的和

#### mean()

求平均值（仅适用于数值类型）。

```kotlin
fun mean(): Double
```

**返回值：** 平均值

#### min()

求最小值（仅适用于数值类型）。

```kotlin
fun min(): Double
```

**返回值：** 最小值

#### max()

求最大值（仅适用于数值类型）。

```kotlin
fun max(): Double
```

**返回值：** 最大值

### Series 转换

#### toList()

转换为 List。

```kotlin
fun toList(): List<T?>
```

**返回值：** 数据列表

#### toString()

转换为字符串表示。

```kotlin
override fun toString(): String
```

**返回值：** 格式化的字符串

---

## DataFrame API

### 创建 DataFrame

#### createDataFrame()

从 Map 创建 DataFrame。

```kotlin
fun createDataFrame(data: Map<String, List<Any?>>): DataFrame
```

**参数：**
- `data`: 列名到列数据的映射

**返回值：** DataFrame 实例

**示例：**
```kotlin
val df = Andas.getInstance().createDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie"),
        "age" to listOf(25, 30, 35),
        "salary" to listOf(50000, 60000, 70000)
    )
)
```

### DataFrame 属性

#### shape()

获取 DataFrame 形状（行数，列数）。

```kotlin
fun shape(): Pair<Int, Int>
```

**返回值：** (行数, 列数) 的 Pair

#### columns()

获取所有列名。

```kotlin
fun columns(): List<String>
```

**返回值：** 列名列表

#### dtypes()

获取每列的数据类型。

```kotlin
fun dtypes(): Map<String, String>
```

**返回值：** 列名到类型的映射

### DataFrame 数据访问

#### get()

通过行索引访问数据。

```kotlin
operator fun get(index: Int): Map<String, Series<Any>?>
```

**参数：**
- `index`: 行索引

**返回值：** 该行的列名到 Series 的映射，每个单元格都是一个单元素 Series

#### get()

通过列名访问数据。

```kotlin
operator fun get(column: String): Series<Any>
```

**参数：**
- `column`: 列名

**返回值：** 该列的 Series 对象

**示例：**
```kotlin
val df = Andas.getInstance().createDataFrame(/* ... */)
val row0 = df[0]        // 获取第0行: Map<String, Series<Any>?>
val ages = df["age"]    // 获取age列: Series<Any>
val age0 = ages[0]      // 获取第一个年龄值
```

#### set()

设置列数据。

```kotlin
operator fun set(key: String, value: Series<Any>)
```

**参数：**
- `key`: 列名
- `value`: Series 数据

**说明：** 新列长度必须与现有行数一致

#### set()

设置行数据。

```kotlin
operator fun set(key: Int, value: Map<String, Any?>)
```

**参数：**
- `key`: 行索引
- `value`: 行数据映射

**说明：** 可以更新现有列或添加新列

#### head()

获取前 n 行。

```kotlin
fun head(n: Int = 5): DataFrame
```

**参数：**
- `n`: 行数，默认 5

**返回值：** 新的 DataFrame，包含前 n 行

#### tail()

获取后 n 行。

```kotlin
fun tail(n: Int = 5): DataFrame
```

**参数：**
- `n`: 行数，默认 5

**返回值：** 新的 DataFrame，包含后 n 行

### DataFrame 列操作

#### selectColumns()

选择指定列。

```kotlin
fun selectColumns(vararg columns: String): DataFrame
```

**参数：**
- `columns`: 要选择的列名

**返回值：** 包含指定列的新 DataFrame

**示例：**
```kotlin
val selected = df.selectColumns("name", "salary")
```

#### addColumn()

添加新列。

```kotlin
fun addColumn(columnName: String, data: List<Any?>): DataFrame
```

**参数：**
- `columnName`: 新列名
- `data`: 列数据

**返回值：** 包含新列的 DataFrame

**示例：**
```kotlin
val withBonus = df.addColumn("bonus", listOf(5000, 6000, 7000))
```

#### dropColumns()

删除指定列。

```kotlin
fun dropColumns(vararg columns: String): DataFrame
```

**参数：**
- `columns`: 要删除的列名

**返回值：** 删除列后的 DataFrame

**示例：**
```kotlin
val dropped = df.dropColumns("department", "temp")
```

#### rename()

重命名列。

```kotlin
fun rename(renames: Map<String, String>): DataFrame
```

**参数：**
- `renames`: 旧列名到新列名的映射

**返回值：** 重命名后的 DataFrame

**示例：**
```kotlin
val renamed = df.rename(mapOf("salary" to "income"))
```

### DataFrame 筛选和排序

#### filter()

条件筛选行。

```kotlin
fun filter(predicate: (Map<String, Series<Any>?>) -> Boolean): DataFrame
```

**参数：**
- `predicate`: 筛选条件函数，接收行数据（Map<String, Series<Any>?>），返回布尔值

**返回值：** 满足条件的行组成的新 DataFrame

**示例：**
```kotlin
val filtered = df.filter { row ->
    val ageSeries = row["age"]
    val salarySeries = row["salary"]
    val age = ageSeries?.get(0) as? Int
    val salary = salarySeries?.get(0) as? Int
    age != null && salary != null && age > 30 && salary > 55000
}
```

**注意：** 当前 API（0.2.0）中 filter 的参数是 `Map<String, Series<Any>?>`，需要通过 `series?.get(0)` 访问单元格值。

#### sortValues()

按指定列排序。

```kotlin
fun sortValues(
    column: String,
    descending: Boolean = false
): DataFrame
```

**参数：**
- `column`: 排序列名
- `descending`: 是否降序，默认 false

**返回值：** 排序后的 DataFrame

**示例：**
```kotlin
val sorted = df.sortValues("age", descending = true)
```

### DataFrame 分组聚合

#### groupBy()

分组操作。

```kotlin
fun groupBy(column: String): GroupedDataFrame
```

**参数：**
- `column`: 分组列名

**返回值：** GroupedDataFrame 对象，支持后续聚合操作

**示例：**
```kotlin
val grouped = df.groupBy("department")
```

#### GroupedDataFrame 聚合方法

GroupedDataFrame 提供以下聚合方法：

```kotlin
// 求和
fun sum(): DataFrame

// 平均值
fun mean(): DataFrame

// 最大值
fun max(): DataFrame

// 最小值
fun min(): DataFrame

// 计数
fun count(): DataFrame
```

**示例：**
```kotlin
val result = df.groupBy("department").mean()
```

#### agg()

多聚合操作。

```kotlin
fun agg(aggregations: Map<String, String>): DataFrame
```

**参数：**
- `aggregations`: 列名到聚合函数名的映射

**支持的聚合函数：**
- `"sum"`: 求和
- `"mean"`: 平均值
- `"max"`: 最大值
- `"min"`: 最小值
- `"count"`: 计数

**返回值：** 聚合结果 DataFrame

**示例：**
```kotlin
val result = df.agg(mapOf(
    "avg_age" to "mean",
    "max_salary" to "max",
    "count" to "count"
))
```

### DataFrame 空值处理

#### dropna()

移除包含空值的行。

```kotlin
fun dropna(): DataFrame
```

**返回值：** 不包含空值的新 DataFrame

#### fillna()

用指定值填充空值。

```kotlin
fun fillna(value: Any): DataFrame
```

**参数：**
- `value`: 填充值

**返回值：** 填充后的新 DataFrame

**示例：**
```kotlin
val filled = df.fillna(0)  // 所有空值填充为0
```

### DataFrame 合并

#### merge()

合并两个 DataFrame。

```kotlin
fun merge(other: DataFrame, on: String): DataFrame
```

**参数：**
- `other`: 要合并的 DataFrame
- `on`: 合并键（列名）

**返回值：** 合并后的 DataFrame

**示例：**
```kotlin
val df1 = Andas.getInstance().createDataFrame(
    mapOf("id" to listOf(1, 2, 3), "value" to listOf(10, 20, 30))
)
val df2 = Andas.getInstance().createDataFrame(
    mapOf("id" to listOf(1, 2, 4), "extra" to listOf(100, 200, 400))
)
val merged = df1.merge(df2, "id")
```

### DataFrame 转换

#### toList()

转换为行列表。

```kotlin
fun toList(): List<Map<String, Any?>>
```

**返回值：** 每行作为 Map 的列表

#### toString()

转换为字符串表示。

```kotlin
override fun toString(): String
```

**返回值：** 格式化的表格字符串

---

## 异步操作 API

### 异步 CSV 操作

#### exportCSVAsync()

异步导出 CSV 文件。

```kotlin
fun exportCSVAsync(
    df: DataFrame,
    file: File,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
)
```

**参数：**
- `df`: 要导出的 DataFrame
- `file`: 目标文件
- `onSuccess`: 成功回调
- `onError`: 失败回调

**示例：**
```kotlin
Andas.getInstance().exportCSVAsync(
    df,
    File(cacheDir, "data.csv"),
    onSuccess = {
        Log.i("CSV", "导出成功")
    },
    onError = { e ->
        Log.e("CSV", "导出失败", e)
    }
)
```

#### readCSVAsync()

异步读取 CSV 文件。

```kotlin
fun readCSVAsync(
    file: File,
    onSuccess: (DataFrame) -> Unit,
    onError: (Exception) -> Unit
)
```

**参数：**
- `file`: CSV 文件
- `onSuccess`: 成功回调，接收 DataFrame
- `onError`: 失败回调

**示例：**
```kotlin
Andas.getInstance().readCSVAsync(
    File(cacheDir, "data.csv"),
    onSuccess = { df ->
        Log.i("CSV", "读取成功，${df.shape().first}行")
    },
    onError = { e ->
        Log.e("CSV", "读取失败", e)
    }
)
```

### 批处理支持

#### BatchProcessor

批量处理器，用于处理大量数据。

```kotlin
class BatchProcessor<T>(
    private val batchSize: Int = 1000,
    private val processor: (List<T>) -> List<T>
)
```

**使用示例：**
```kotlin
val processor = BatchProcessor<Int>(1000) { batch ->
    batch.map { it * 2 }
}

val result = processor.process(dataList)
```

---

## JNI 原生 API

### NativeMath

提供高性能数学运算。

#### isAvailable()

检查原生库是否可用。

```kotlin
fun isAvailable(): Boolean
```

#### multiplyDoubleArray()

数组乘法。

```kotlin
fun multiplyDoubleArray(data: DoubleArray, multiplier: Double): DoubleArray
```

#### sumDoubleArray()

数组求和。

```kotlin
fun sumDoubleArray(data: DoubleArray): Double
```

#### meanDoubleArray()

数组平均值。

```kotlin
fun meanDoubleArray(data: DoubleArray): Double
```

#### maxDoubleArray()

数组最大值。

```kotlin
fun maxDoubleArray(data: DoubleArray): Double
```

#### minDoubleArray()

数组最小值。

```kotlin
fun minDoubleArray(data: DoubleArray): Double
```

#### argsort()

排序索引。

```kotlin
fun argsort(data: DoubleArray): IntArray
```

#### normalize()

归一化。

```kotlin
fun normalize(data: DoubleArray): DoubleArray
```

#### greaterThan()

大于比较。

```kotlin
fun greaterThan(data: DoubleArray, value: Double): BooleanArray
```

### NativeData

提供数据处理功能。

#### isAvailable()

检查原生库是否可用。

```kotlin
fun isAvailable(): Boolean
```

#### findNullIndices()

查找空值索引。

```kotlin
fun findNullIndices(data: DoubleArray): IntArray
```

#### dropNullValues()

移除空值。

```kotlin
fun dropNullValues(data: DoubleArray): DoubleArray
```

#### where()

根据掩码获取索引。

```kotlin
fun where(mask: BooleanArray): IntArray
```

#### describe()

统计描述。

```kotlin
fun describe(data: DoubleArray): DoubleArray
```

**返回值：** [count, mean, std, min, max]

#### sample()

随机采样。

```kotlin
fun sample(data: DoubleArray, size: Int): DoubleArray
```

### NativeMath.Benchmark

性能基准测试。

```kotlin
object Benchmark {
    const val OP_ARRAY_CREATE = 0
    const val OP_MATH_OPERATION = 1
    const val OP_STATISTICS = 2
    const val OP_FILTER = 3
    
    fun measureOperationTime(opType: Int, dataSize: Int): Long
}
```

**使用示例：**
```kotlin
val time = NativeMath.Benchmark.measureOperationTime(
    NativeMath.Benchmark.OP_MATH_OPERATION, 
    10000
)
```

---

## 线程池管理 API

### AndaThreadPool

#### execute()

提交任务到线程池。

```kotlin
fun <T> execute(
    task: () -> T,
    callback: (AsyncResult<T>) -> Unit
)
```

**参数：**
- `task`: 任务函数
- `callback`: 结果回调

#### shutdown()

关闭线程池。

```kotlin
fun shutdown()
```

#### getStats()

获取线程池统计信息。

```kotlin
fun getStats(): Map<String, Any>
```

**返回值：** 包含以下信息的 Map：
- `active_threads`: 活跃线程数
- `queue_size`: 队列大小
- `completed_tasks`: 完成任务数
- `total_tasks`: 总任务数

### AsyncResult

异步操作结果封装。

```kotlin
sealed class AsyncResult<T> {
    data class Success<T>(val data: T) : AsyncResult<T>()
    data class Error<T>(val exception: Exception) : AsyncResult<T>()
}
```

**使用示例：**
```kotlin
AndaThreadPool.execute({
    // 耗时操作
    heavyCalculation()
}) { result ->
    when (result) {
        is AsyncResult.Success -> {
            // 处理成功结果
            val data = result.data
        }
        is AsyncResult.Error -> {
            // 处理错误
            val error = result.exception
        }
    }
}
```

---

## 类型系统

### 支持的数据类型

- **数值类型**: Int, Long, Float, Double
- **字符串类型**: String
- **布尔类型**: Boolean
- **空值**: null

### 类型推断

SDK 会自动推断数据类型，并在编译时进行类型检查。

---

## 错误处理

### 常见异常

#### InitializationException

SDK 未初始化时抛出。

```kotlin
class InitializationException : Exception("Andas SDK not initialized")
```

#### InvalidDataException

数据格式错误时抛出。

```kotlin
class InvalidDataException(message: String) : Exception(message)
```

#### TimeoutException

异步操作超时时抛出。

```kotlin
class TimeoutException : Exception("Operation timed out")
```

#### NativeException

JNI 操作失败时抛出。

```kotlin
class NativeException(message: String) : Exception(message)
```

### 错误处理最佳实践

```kotlin
try {
    val df = Andas.getInstance().createDataFrame(data)
    // 处理 DataFrame
} catch (e: InitializationException) {
    Log.e("Andas", "SDK 未初始化", e)
} catch (e: InvalidDataException) {
    Log.e("Andas", "数据格式错误", e)
} catch (e: Exception) {
    Log.e("Andas", "未知错误", e)
}
```

---

## 性能优化建议

### 1. 大数据量处理

- 使用异步操作避免阻塞主线程
- 合理设置 batch size
- 及时释放不再使用的资源

### 2. 内存管理

- 及时调用 `destroy()` 释放资源
- 避免创建大量临时对象
- 使用原生方法处理大数据

### 3. 并发控制

- 合理配置线程池大小
- 避免过多并发任务
- 使用适当的同步机制

---


## 更新日志

### v0.1.5 (2026-11-01)

**性能与大数据读取**

- 流式列式 CSV 解析：不再把整个文件读入 `List<String>`，20 万行 × 15 列（约 36 MB）
  可在 256 MB JVM 堆和 192 MB Android 堆完成读取。
- 原始类型列存储、共享范围索引、惰性标签映射。
- 统计单次遍历、排序 `O(n log n)`、分组/连接哈希化。
- 新增 pandas 2.1.0 对比实验与英文技术报告；详见 `BENCHMARK_REPORT.md` 和 `report/`。

**兼容性说明**

- `sortValues` / `filter` / `sample` / `dropna` 保留原索引标签。
- `variance` / `std` 使用样本标准差（ddof=1）。
- CSV 仅将双引号视为引号字符；单引号按普通字符处理。

### v0.0.1.rc3 (2025-12-19)

**重大变更：DataFrame API 统一化**

- **API 统一**：`getRow()`, `loc()`, `iloc()`, `operator get()` 返回类型改为 `Map<String, Series<Any>?>`
- **filter 方法更新**：谓词函数参数类型改为 `Map<String, Series<Any>?>`
- **类型安全**：每个单元格封装为单元素 Series 对象
- **空值处理**：更好的 null 值支持和类型推断
- **兼容性修复**：确保 GroupBy 与新 API 兼容
- **文档更新**：完整的 API 参考和迁移指南

**迁移示例：**
```kotlin
// 旧版本
val row: Map<String, Any?> = df.getRow(0)
val age = row["age"]  // Any?

// 新版本 (0.0.1.rc3)
val row: Map<String, Series<Any>?> = df.getRow(0)
val age = row["age"]?.get(0)  // Any?，通过 Series 访问
```

### v0.0.1.rc1 (2025-12-19)

- 初始版本发布
- 支持 Series 和 DataFrame
- 支持异步操作
- 支持 JNI 原生加速
- 支持线程池管理


## 数值计算 API（0.2.0）

新增包：

- `cn.ac.oac.libs.andas.numpy`：`NDArray`、`DType`、`Linalg`、`FFT`、`NumpyRandom`、`PandasBridge`；
- `cn.ac.oac.libs.andas.scipy`：`Stats`、回归、`Optimize`、`Integrate`、插值、`Signal`、`Special`；
- `cn.ac.oac.libs.andas.scipy.sparse`：`SparseMatrix`；
- `cn.ac.oac.libs.andas.scipy.image`：`ImageOps`；
- `cn.ac.oac.libs.andas.plot`：`Figure`、`Axes`；
- `cn.ac.oac.libs.andas.annotation`：`@AndasApi` 等注解与 `AndasExtensions`。

完整签名与示例见 `doc/numpy/`、`doc/scipy/`、`doc/plot/` 与 `doc/annotation/`。
