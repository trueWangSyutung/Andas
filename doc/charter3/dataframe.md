## 第三章：DataFrame API

### 3.1 DataFrame 概述

DataFrame 是 Andas SDK 中最核心的数据结构，类似于二维表格，具有行和列的结构。它提供了强大的数据操作和分析能力，是数据处理的主要工具。

**核心特性：**
- 📊 二维表格结构：支持行和列的数据组织
- 🎯 多数据类型：每列可以有不同的数据类型
- ⚡ 高性能操作：通过 JNI 集成 C++ 原生代码加速
- 🔍 强大索引：支持多种索引和选择方式
- 📈 统计分析：内置丰富的统计函数
- 🔄 链式操作：支持流畅的函数式 API

### 3.2 创建 DataFrame

#### 3.2.1 从列数据创建

```kotlin
val df = Andas.getInstance().createDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie", "David"),
        "age" to listOf(25, 30, 35, 40),
        "salary" to listOf(50000, 60000, 70000, 80000),
        "department" to listOf("IT", "HR", "IT", "Finance")
    )
)

println(df)
// 输出:
//    name  age  salary  department
// 0  Alice   25   50000          IT
// 1    Bob   30   60000          HR
// 2 Charlie  35   70000          IT
// 3   David   40   80000      Finance
```

#### 3.2.2 从行数据创建

```kotlin
val rows = listOf(
    mapOf("name" to "Alice", "age" to 25, "score" to 85.0),
    mapOf("name" to "Bob", "age" to 30, "score" to 92.0),
    mapOf("name" to "Charlie", "age" to 35, "score" to 78.0)
)

val df = Andas.getInstance().createDataFrameFromRows(rows)
println(df)
// 输出:
//    name  age  score
// 0  Alice   25   85.0
// 1    Bob   30   92.0
// 2 Charlie  35   78.0
```

### 3.3 数据查看

#### 3.3.1 查看基本信息

```kotlin
val df = Andas.getInstance().createDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie", "David"),
        "age" to listOf(25, 30, 35, 40),
        "salary" to listOf(50000, 60000, 70000, 80000)
    )
)

// 查看前几行
val head = df.head()
println("前5行:\n$head")

val head3 = df.head(3)
println("前3行:\n$head3")

// 查看后几行
val tail = df.tail()
println("后5行:\n$tail")

// 查看形状
val shape = df.shape()
println("形状: $shape")  // 输出: (4, 3)

// 查看列名
val columns = df.columns()
println("列名: $columns")  // 输出: [name, age, salary]

// 查看数据类型
val dtypes = df.dtypes()
println("数据类型:\n$dtypes")
```


### 3.4 数据选择

#### 3.4.1 列选择

```kotlin
val df = Andas.getInstance().createDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie"),
        "age" to listOf(25, 30, 35),
        "salary" to listOf(50000, 60000, 70000)
    )
)

// 选择单列
val nameColumn = df["name"]
println("姓名列: $nameColumn")

// 选择多列（新增功能）
val selected = df[listOf("name", "salary")]
println("选择多列:\n$selected")

// 使用 selectColumns 方法（新增功能）
val selected2 = df.selectColumns("name", "age")
println("使用 selectColumns:\n$selected2")
```

#### 3.4.2 行选择

```kotlin
// 选择单行
val row = df[0]
println("第0行: $row")

// 选择多行
val rows = df[listOf(0, 2)]
println("第0行和第2行:\n$rows")

// 切片选择
val slice = df.slice(1, 3)
println("切片 [1, 3):\n$slice")
```

#### 3.4.3 条件选择

```kotlin
// 筛选年龄大于28的行
val filtered = df.filter { row ->
    row.getInt("age") > 28
}
println("年龄 > 28:\n$filtered")

// 多条件筛选
val filtered2 = df.filter { row ->
    row.getInt("age") > 25 && row.getInt("salary") > 55000
}
println("年龄 > 25 且 薪资 > 55000:\n$filtered2")
```

### 3.5 数据操作

#### 3.5.1 新增列

```kotlin
val df = Andas.getInstance().createDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie"),
        "age" to listOf(25, 30, 35),
        "salary" to listOf(50000, 60000, 70000)
    )
)

// 新增计算列
val dfWithBonus = df.addColumn("bonus") { row ->
    row.getInt("salary") * 0.1
}
println("新增奖金列:\n$dfWithBonus")

// 新增分类列
val dfWithCategory = df.addColumn("category") { row ->
    when (row.getInt("age")) {
        in 20..29 -> "青年"
        in 30..39 -> "中年"
        else -> "其他"
    }
}
println("新增分类列:\n$dfWithCategory")
```

#### 3.5.2 删除列

```kotlin
// 删除单列
val dfWithoutAge = df.dropColumn("age")
println("删除年龄列:\n$dfWithoutAge")

// 删除多列
val dfSimple = df.dropColumns(listOf("age", "salary"))
println("删除多列:\n$dfSimple")
```

#### 3.5.3 重命名列

```kotlin
// 重命名单列
val dfRenamed = df.renameColumn("salary", "income")
println("重命名后:\n$dfRenamed")

// 重命名多列
val dfRenamed2 = df.renameColumns(
    mapOf("name" to "姓名", "age" to "年龄", "salary" to "薪资")
)
println("批量重命名:\n$dfRenamed2")
```

### 3.6 统计计算

#### 3.6.1 基础统计

```kotlin
val df = Andas.getInstance().createDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie", "David"),
        "age" to listOf(25, 30, 35, 40),
        "salary" to listOf(50000, 60000, 70000, 80000),
        "math" to listOf(85.0, 92.0, 78.0, 88.0),
        "english" to listOf(76.0, 85.0, 90.0, 82.0)
    )
)

// 原生统计计算
val mathMean = df.meanNative("math")
val salarySum = df.sumNative("salary")
val ageMax = df.maxNative("age")

println("数学平均值: $mathMean")
println("工资总和: $salarySum")
println("最大年龄: $ageMax")
```

#### 3.6.2 相关系数矩阵（新增功能）

```kotlin
// 计算相关系数矩阵
val correlation = df.corr()
println("相关系数矩阵:\n$correlation")
// 输出:
//            age    salary   math   english
// age       1.0000   1.0000  1.0000   1.0000
// salary    1.0000   1.0000  1.0000   1.0000
// math      1.0000   1.0000  1.0000   1.0000
// english   1.0000   1.0000  1.0000   1.0000
```

#### 3.6.3 描述性统计

```kotlin
// 对数值列进行描述性统计
val mathStats = df["math"]?.describe()
println("数学成绩统计:\n$mathStats")

// 多列统计
val stats = df.describe()
println("描述性统计:\n$stats")
```

### 3.7 空值处理

#### 3.7.1 检测空值

```kotlin
val df = Andas.getInstance().createDataFrame(
    mapOf(
        "name" to listOf("Alice", null, "Charlie", "David"),
        "age" to listOf(25, 30, null, 40),
        "salary" to listOf(50000, 60000, 70000, null)
    )
)

// 检测空值
val nullCounts = df.nullCounts()
println("各列空值数量: $nullCounts")
// 输出: {name=1, age=1, salary=1}

// 检查是否有空值
val hasNull = df.hasNull()
println("是否包含空值: $hasNull")
```

#### 3.7.2 处理空值

```kotlin
// 删除包含空值的行
val cleaned = df.dropRowsWithNull()
println("删除空值行后:\n$cleaned")

// 填充空值
val filled = df.fillNull(mapOf(
    "name" to "Unknown",
    "age" to 0,
    "salary" to 0
))
println("填充空值后:\n$filled")

// 原生空值处理
val filledNative = df.fillNullWithConstant(0.0)
println("原生填充:\n$filledNative")
```

### 3.8 数据导出

#### 3.8.1 导出到 CSV

```kotlin
val df = Andas.getInstance().createDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie"),
        "age" to listOf(25, 30, 35),
        "salary" to listOf(50000, 60000, 70000)
    )
)

// 同步导出
val file = File(context.filesDir, "output.csv")
df.toCSV(file)
println("导出成功: ${file.absolutePath}")

// 异步导出
Andas.getInstance().exportCSVAsync(
    df, file,
    onSuccess = {
        println("异步导出成功")
    },
    onError = { error ->
        println("导出失败: ${error.message}")
    }
)
```

#### 3.8.2 保存到存储

```kotlin
// 保存到私有存储（异步）
Andas.getInstance().saveToPrivateStorageAsync(
    "data.csv", df,
    onSuccess = {
        println("保存成功")
    },
    onError = { error ->
        println("保存失败: ${error.message}")
    }
)
```

### 3.9 高性能操作

#### 3.9.1 原生计算

```kotlin
// 原生求和
val sum = df.sumNative("salary")

// 原生平均值
val mean = df.meanNative("age")

// 原生最大值
val max = df.maxNative("salary")

// 原生最小值
val min = df.minNative("age")

// 原生标准差
val std = df.stdNative("math")

// 原生方差
val variance = df.varianceNative("english")
```

#### 3.9.2 批量处理

```kotlin
// 批量操作示例
val largeDf = Andas.getInstance().createDataFrame(
    mapOf(
        "id" to (1..10000).toList(),
        "value" to (1..10000).map { it * 1.1 }
    )
)

// 批量计算
val batchResult = largeDf.processBatch(batchSize = 1000) { batch ->
    // 对每批数据进行处理
    batch.map { row ->
        mapOf(
            "id" to row.getInt("id"),
            "doubled" to row.getDouble("value") * 2
        )
    }
}
```

### 3.10 小结

本章详细介绍了 DataFrame API 的使用方法，包括：

1. **创建 DataFrame**：支持列数据和行数据两种方式
2. **数据查看**：查看基本信息、详细信息、形状、列名等
3. **数据选择**：列选择、行选择、条件选择
4. **数据操作**：新增列、删除列、重命名列
5. **统计计算**：基础统计、相关系数矩阵、描述性统计
6. **空值处理**：检测和处理空值
7. **数据导出**：导出到 CSV 和保存到存储
8. **高性能操作**：原生计算和批量处理

DataFrame 是 Andas SDK 的核心数据结构，提供了丰富的数据操作功能，是进行数据分析的主要工具。
