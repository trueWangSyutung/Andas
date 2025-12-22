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

#### 2.2.1 使用列表创建

```kotlin
// 创建数值 Series
val numbers = Andas.getInstance().createSeries(listOf(1, 2, 3, 4, 5))
println(numbers)
// 输出:
// 0	1
// 1	2
// 2	3
// 3	4
// 4	5
// Name: null, dtype: INT32

// 创建带索引的 Series
val indexed = Andas.getInstance().createSeries(
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
val grades = Andas.getInstance().createSeriesFromMap(
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

### 2.3 数据访问

#### 2.3.1 位置访问

```kotlin
val series = Andas.getInstance().createSeries(listOf(10, 20, 30, 40, 50))

// 获取指定位置的值
val value = series[2]  // 输出: 30
println("位置2的值: $value")
```

#### 2.3.2 标签访问

```kotlin
val series = Andas.getInstance().createSeries(
    data = listOf(10, 20, 30),
    index = listOf("a", "b", "c")
)

// 通过标签访问
val value = series["b"]  // 输出: 20
println("标签'b'的值: $value")
```

#### 2.3.3 查看数据

```kotlin
val series = Andas.getInstance().createSeries((1..100).toList())

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
```

### 2.4 空值处理

#### 2.4.1 检测空值

```kotlin
val series = Andas.getInstance().createSeries(listOf(1, null, 3, null, 5))

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
val series = Andas.getInstance().createSeries(listOf(1, null, 3, null, 5))

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
val series = Andas.getInstance().createSeries(listOf(1.0, null, 3.0, null, 5.0))

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
val fruits = Andas.getInstance().createSeries(listOf("apple", "banana", "apple", "cherry", "banana"))

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
val numbers = Andas.getInstance().createSeries(listOf(1.0, 2.0, 3.0, 4.0, 5.0))

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
val series = Andas.getInstance().createSeries(listOf(1, 2, 3, 4, 5))

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
val series = Andas.getInstance().createSeries(listOf(1, 2, 3, 4, 5))

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
val unsorted = Andas.getInstance().createSeries(
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
val series = Andas.getInstance().createSeries(listOf(1.0, 2.0, 3.0, 4.0, 5.0))

// 累计求和
val cumsum = series.cumsum()
println("累计求和: $cumsum")
// 输出: [1.0, 3.0, 6.0, 10.0, 15.0]
```

#### 2.7.2 乘法运算

```kotlin
val series = Andas.getInstance().createSeries(listOf(1, 2, 3, 4, 5))

// 乘以2
val multiplied = series * 2
println("乘以2: $multiplied")
// 输出: [2, 4, 6, 8, 10]
```

#### 2.7.3 归一化（原生）

```kotlin
val series = Andas.getInstance().createSeries(listOf(1.0, 2.0, 3.0, 4.0, 5.0))

// 归一化
val normalized = series.normalize()
println("归一化: $normalized")
// 输出: [0.0, 0.25, 0.5, 0.75, 1.0]
```

### 2.8 格式转换

```kotlin
val series = Andas.getInstance().createSeries(
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
```

### 2.9 高性能原生操作

#### 2.9.1 批量处理

```kotlin
val series = Andas.getInstance().createSeries((1..10000).map { it.toDouble() })

// 批量处理（自动分块）
val processed = series.processBatch(batchSize = 1000)
println("批量处理完成，大小: ${processed.size()}")
```

#### 2.9.2 采样操作

```kotlin
val series = Andas.getInstance().createSeries((1..10000).toList())

// 随机采样100个元素
val sample = series.sample(100)
println("采样结果，大小: ${sample.size()}")
```

### 2.10 小结

本章详细介绍了 Series API 的使用方法，包括：

1. **创建 Series**：支持列表和 Map 两种方式
2. **数据访问**：位置访问和标签访问
3. **空值处理**：检测、删除、填充以及原生加速
4. **统计功能**：唯一值统计和数值统计
5. **数据变换**：映射、筛选、排序
6. **数值计算**：累计计算和归一化
7. **格式转换**：转换为 List 和 Map
8. **高性能操作**：批量处理和采样

Series 是 Andas SDK 的基础数据结构，掌握 Series 的使用对于后续学习 DataFrame 非常重要。
