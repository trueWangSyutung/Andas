## 第二章：Series API

### 2.1 Series 概述

Series 是 Andas SDK 中的核心数据结构之一，类似于一维数组，可以存储任何数据类型，并通过标签（索引）访问元素。它是构建 DataFrame 的基础，提供了丰富的数据操作和分析功能。

**核心特性：**
- 📊 灵活的数据类型：支持泛型，可存储任意类型数据
- 🔍 多索引访问：支持位置索引和标签索引两种方式
- ⚡ 高性能计算：通过 JNI 集成 C++ 原生代码加速
- 🛡️ 空值安全：完整的空值检测和处理机制
- 🔄 链式操作：支持函数式编程风格的链式调用

### 2.2 创建 Series

#### 2.2.1 使用工厂函数创建

```kotlin
import cn.ac.oac.libs.andas.factory.andasSeries

// 创建数值 Series
val numbers = andasSeries(listOf(1, 2, 3, 4, 5))
println(numbers)
// 输出:
// 0	1
// 1	2
// 2	3
// 3	4
// 4	5
// Name: null, dtype: INT32

// 创建带索引的 Series
val indexed = andasSeries(
    data = listOf(10, 20, 30, 40, 50),
    index = listOf("a", "b", "c", "d", "e"),
    name = "scores"
)
println(indexed)
// 输出:
// a	10
// b	20
// c	30
// d	40
// e	50
// Name: scores, dtype: INT32
```

#### 2.2.2 使用 Map 创建

```kotlin
import cn.ac.oac.libs.andas.factory.andasSeriesFromMap

val grades = andasSeriesFromMap(
    map = mapOf(
        "Alice" to 85,
        "Bob" to 92,
        "Charlie" to 78
    ),
    name = "grades"
)
println(grades)
// 输出:
// Alice	85
// Bob	92
// Charlie	78
// Name: grades, dtype: INT32
```

#### 2.2.3 使用 Series 构造函数

```kotlin
import cn.ac.oac.libs.andas.entity.Series

// 直接使用构造函数
val series = Series(
    data = listOf(1, 2, 3, 4, 5),
    index = listOf(0, 1, 2, 3, 4),
    name = "numbers"
)
```

### 2.3 数据访问

#### 2.3.1 位置访问

```kotlin
val series = andasSeries(listOf(10, 20, 30, 40, 50))

// 获取指定位置的值
val value = series[2]  // 输出: 30
println("位置2的值: $value")
```

#### 2.3.2 标签访问

```kotlin
val series = andasSeries(
    data = listOf(10, 20, 30),
    index = listOf("a", "b", "c")
)

// 通过标签访问
val value = series["b"]  // 输出: 20
println("标签'b'的值: $value")
```

#### 2.3.3 查看数据

```kotlin
val series = andasSeries((1..100).toList())

// 获取前5个元素（默认）
val head = series.head()
println("前5个元素: $head")

// 获取前3个元素
val head3 = series.head(3)
println("前3个元素: $head3")

// 获取后5个元素
val tail = series.tail()
println("后5个元素: $tail")

// 获取后10个元素
val tail10 = series.tail(10)
println("后10个元素: $tail10")

// 查看基本信息
val size = series.size()
val name = series.name()
val index = series.index()
println("大小: $size, 名称: $name, 索引: $index")
```

### 2.4 空值处理

#### 2.4.1 检测空值

```kotlin
val series = andasSeries(listOf(1, null, 3, null, 5))

// 检查空值
val isNull = series.isnull()
println("空值检测: $isNull")
// 输出: [false, true, false, true, false]

// 检查非空值
val notNull = series.notnull()
println("非空检测: $notNull")
// 输出: [true, false, true, false, true]
```

#### 2.4.2 处理空值

```kotlin
val series = andasSeries(listOf(1, null, 3, null, 5))

// 删除空值
val cleaned = series.dropna()
println("删除空值后: $cleaned")
// 输出: [1, 3, 5]

// 填充空值
val filled = series.fillna(0)
println("填充空值后: $filled")
// 输出: [1, 0, 3, 0, 5]
```

#### 2.4.3 原生空值处理

```kotlin
val series = andasSeries(listOf(1.0, null, 3.0, null, 5.0))

// 查找空值索引（高性能）
val nullIndices = series.findNullIndices()
println("空值索引: $nullIndices")
// 输出: [1, 3]

// 原生丢弃空值
val dropped = series.dropNullValues()
println("原生丢弃空值: $dropped")

// 原生填充空值
val filled = series.fillNullWithConstant(0.0)
println("原生填充空值: $filled")
```

### 2.5 统计功能

#### 2.5.1 唯一值统计

```kotlin
val fruits = andasSeries(listOf("apple", "banana", "apple", "cherry", "banana"))

// 获取唯一值
val unique = fruits.unique()
println("唯一值: $unique")
// 输出: [apple, banana, cherry]

// 统计频次
val counts = fruits.valueCounts()
println("频次统计: $counts")
// 输出: {apple=2, banana=2, cherry=1}
```

#### 2.5.2 数值统计（原生加速）

```kotlin
val numbers = andasSeries(listOf(1.0, 2.0, 3.0, 4.0, 5.0))

// 基础统计（原生）
val sum = numbers.sum()
val mean = numbers.mean()
val max = numbers.max()
val min = numbers.min()
val variance = numbers.variance()
val std = numbers.std()

println("总和: $sum")        // 15.0
println("平均值: $mean")      // 3.0
println("最大值: $max")        // 5.0
println("最小值: $min")        // 1.0
println("方差: $variance")  // 2.5
println("标准差: $std")        // 1.5811

// 统计描述（原生）
val desc = numbers.describe()
println("统计描述: $desc")
// 输出: {count=5.0, mean=3.0, std=1.5811, min=1.0, max=5.0}
```

### 2.6 数据变换

#### 2.6.1 映射变换

```kotlin
val series = andasSeries(listOf(1, 2, 3, 4, 5))

// 乘以2
val doubled = series.map { it?.times(2) }
println("乘以2: $doubled")
// 输出: [2, 4, 6, 8, 10]

// 类型转换
val strings = series.map { it.toString() }
println("转换为字符串: $strings")
// 输出: ["1", "2", "3", "4", "5"]
```

#### 2.6.2 筛选过滤

```kotlin
val series = andasSeries(listOf(1, 2, 3, 4, 5))

// 大于3的元素
val filtered = series.filter { it?.compareTo(3) ?: -1 > 0 }
println("大于3的元素: $filtered")
// 输出: [4, 5]

// 原生高性能筛选
val filteredNative = series.filterGreaterThan(3.0)
println("原生筛选: $filteredNative")
```

#### 2.6.3 排序

```kotlin
val unsorted = andasSeries(
    data = listOf(3, 1, 4, 2, 5),
    index = listOf("c", "a", "d", "b", "e")
)

// 按索引排序
val byIndex = unsorted.sortIndex()
println("按索引排序: $byIndex")

// 按值排序
val byValue = unsorted.sortValues()
println("按值排序: $byValue")

// 降序排序
val descending = unsorted.sortValues(descending = true)
println("降序排序: $descending")

// 原生高性能排序
val sortedNative = unsorted.sortValuesNative(descending = true)
println("原生排序: $sortedNative")
```

### 2.7 数值计算

#### 2.7.1 累计计算

```kotlin
val series = andasSeries(listOf(1.0, 2.0, 3.0, 4.0, 5.0))

// 累计求和
val cumsum = series.cumsum()
println("累计求和: $cumsum")
// 输出: [1.0, 3.0, 6.0, 10.0, 15.0]
```

#### 2.7.2 乘法运算

```kotlin
val series = andasSeries(listOf(1, 2, 3, 4, 5))

// 乘以2
val multiplied = series * 2
println("乘以2: $multiplied")
// 输出: [2, 4, 6, 8, 10]
```

#### 2.7.3 归一化（原生）

```kotlin
val series = andasSeries(listOf(1.0, 2.0, 3.0, 4.0, 5.0))

// 归一化
val normalized = series.normalize()
println("归一化: $normalized")
// 输出: [0.0, 0.25, 0.5, 0.75, 1.0]
```

### 2.8 格式转换

```kotlin
val series = andasSeries(
    data = listOf(10, 20, 30),
    index = listOf("a", "b", "c"),
    name = "values"
)

// 转换为List
val list = series.toList()
println("转换为List: $list")
// 输出: [10, 20, 30]

// 转换为Map
val map = series.toMap()
println("转换为Map: $map")
// 输出: {a=10, b=20, c=30}

// 转换为数组
val array = series.values()
println("转换为数组: $array")
// 输出: [10, 20, 30]
```

### 2.9 高性能原生操作

#### 2.9.1 批量处理

```kotlin
val series = andasSeries((1..10000).map { it.toDouble() })

// 批量处理（自动分块）
val processed = series.processBatch(batchSize = 1000)
println("批量处理完成，大小: ${processed.size()}")
```

#### 2.9.2 采样操作

```kotlin
val series = andasSeries((1..10000).toList())

// 随机采样100个元素
val sample = series.sample(100)
println("采样结果，大小: ${sample.size()}")
```

#### 2.9.3 向量化运算

```kotlin
// 向量化加法
val series1 = andasSeries(listOf(1.0, 2.0, 3.0))
val series2 = andasSeries(listOf(10.0, 20.0, 30.0))
val sum = series1 + series2  // [11.0, 22.0, 33.0]

// 点积
val dotProduct = series1.dot(series2)  // 140.0

// 范数
val norm = series1.norm()  // sqrt(14) ≈ 3.74
```

### 2.10 检查原生库状态

```kotlin
val series = andasSeries(listOf(1.0, 2.0, 3.0))

// 检查原生库是否可用
val isNativeAvailable = series.isNativeAvailable()
println("原生库可用: $isNativeAvailable")

// 性能基准测试
val time = series.benchmarkOperation(1, 1000000)  // 操作类型1，数据量100万
println("基准测试耗时: ${time}ms")
```

### 2.11 扩展函数

```kotlin
import cn.ac.oac.libs.andas.entity.Series

// 使用扩展函数进行常见操作
val series = andasSeries(listOf(1, 2, 3, 4, 5))

// 检查是否包含某个值
val contains = series.contains(3)  // true

// 获取元素个数
val count = series.count { it != null && it > 2 }  // 3

// 最大最小值
val maxValue = series.maxOrNull()  // 5
val minValue = series.minOrNull()  // 1
```

### 2.12 错误处理

```kotlin
val series = andasSeries(listOf(1, 2, 3))

try {
    // 访问不存在的索引
    val value = series[10]
} catch (e: IndexOutOfBoundsException) {
    println("索引越界: ${e.message}")
}

try {
    // 对非数值类型调用统计方法
    val stringSeries = andasSeries(listOf("a", "b", "c"))
    val sum = stringSeries.sum()  // 会抛出异常
} catch (e: UnsupportedOperationException) {
    println("不支持的操作: ${e.message}")
}
```

### 2.13 小结

本章详细介绍了 Series API 的使用方法，包括：

1. **创建 Series**：支持工厂函数、Map 和构造函数三种方式
2. **数据访问**：位置访问和标签访问
3. **空值处理**：检测、删除、填充以及原生加速
4. **统计功能**：唯一值统计和数值统计
5. **数据变换**：映射、筛选、排序
6. **数值计算**：累计计算和归一化
7. **格式转换**：转换为 List、Map 和数组
8. **高性能操作**：批量处理、采样和向量化运算
9. **原生库状态**：检查可用性和性能基准
10. **扩展函数**：便捷操作
11. **错误处理**：异常捕获和处理

Series 是 Andas SDK 的基础数据结构，掌握 Series 的使用对于后续学习 DataFrame 非常重要。通过合理使用原生方法，可以显著提升数据处理性能。
