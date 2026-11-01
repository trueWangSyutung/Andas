# 第三章：DataFrame API

## 3.1 DataFrame 概述

DataFrame 是 Andas SDK 中最核心的数据结构，类似于二维表格，具有行和列的结构。它提供了强大的数据操作和分析能力，是数据处理的主要工具。

**核心特性：**
- 二维表格结构：支持行和列的数据组织
- 多数据类型：每列可以有不同的数据类型
- 高性能操作：通过 JNI 集成 C++ 原生代码加速
- 强大索引：支持多种索引和选择方式
- 统计分析：内置丰富的统计函数
- 链式操作：支持流畅的函数式 API
- 灵活IO：支持CSV、Assets、私有存储等多种数据源

## 3.2 创建 DataFrame

### 3.2.1 从列数据创建（使用工厂函数）

```kotlin
import cn.ac.oac.libs.andas.factory.andasDataFrame

val df = andasDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie", "David"),
        "age" to listOf(25, 30, 35, 40),
        "salary" to listOf(50000, 60000, 70000, 80000),
        "department" to listOf("IT", "HR", "IT", "Finance")
    )
)

println(df)
// 输出:
// name    age    salary    department
// Alice   25     50000     IT
// Bob     30     60000     HR
// Charlie 35     70000     IT
// David   40     80000     Finance
```

### 3.2.2 从行数据创建（使用工厂函数）

```kotlin
import cn.ac.oac.libs.andas.factory.andasDataFrameFromRows

val rows = listOf(
    mapOf("name" to "Alice", "age" to 25, "score" to 85.0),
    mapOf("name" to "Bob", "age" to 30, "score" to 92.0),
    mapOf("name" to "Charlie", "age" to 35, "score" to 78.0)
)

val df = andasDataFrameFromRows(rows)
println(df)
// 输出:
// name    age    score
// Alice   25     85.0
// Bob     30     92.0
// Charlie 35     78.0
```

### 3.2.3 使用 DataFrame 构造函数

```kotlin
import cn.ac.oac.libs.andas.entity.DataFrame

// 从列数据创建
val df1 = DataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie"),
        "age" to listOf(25, 30, 35)
    )
)

// 从行数据创建
val df2 = DataFrame(
    listOf(
        mapOf("name" to "Alice", "age" to 25),
        mapOf("name" to "Bob", "age" to 30)
    )
)
```

## 3.3 数据查看

### 3.3.1 查看基本信息

```kotlin
val df = andasDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie", "David"),
        "age" to listOf(25, 30, 35, 40),
        "salary" to listOf(50000, 60000, 70000, 80000)
    )
)

// 查看前几行
val head = df.head()  // 默认前5行
println("前5行:\n$head")

val head3 = df.head(3)  // 前3行
println("前3行:\n$head3")

// 查看后几行
val tail = df.tail()  // 默认后5行
println("后5行:\n$tail")

// 查看形状
val shape = df.shape()  // (4, 3)
println("形状: $shape")

// 查看列名
val columns = df.columns()  // [name, age, salary]
println("列名: $columns")

// 查看数据类型
val dtypes = df.dtypes()
println("数据类型:\n$dtypes")

// 查看完整数据（自动格式化）
println(df)
```

### 3.3.2 检查原生库状态

```kotlin
// 检查原生库是否可用
val isNativeAvailable = df.isNativeAvailable()
println("原生库可用: $isNativeAvailable")

// 性能基准测试
val time = df.benchmarkOperation(1, 1000000)  // 操作类型1，数据量100万
println("基准测试耗时: ${time}ms")
```

## 3.4 数据选择

### 3.4.1 列选择

```kotlin
val df = andasDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie"),
        "age" to listOf(25, 30, 35),
        "salary" to listOf(50000, 60000, 70000)
    )
)

// 选择单列 - 返回 Series
val nameColumn = df["name"]
println("姓名列: $nameColumn")

// 选择多列 - 使用 List
val selected = df[listOf("name", "salary")]
println("选择多列:\n$selected")

// 选择多列 - 使用 selectColumns 方法
val selected2 = df.selectColumns("name", "age")
println("使用 selectColumns:\n$selected2")
```

### 3.4.2 行选择

```kotlin
// 选择单行 - 返回 Map<String, Series<Any>?>
val row = df[0]
println("第0行: $row")

// 按标签获取行
val rowByLabel = df.loc(0)  // 假设索引是数字
println("标签为0的行: $rowByLabel")

// 按位置获取行
val rowByPos = df.iloc(0)
println("位置0的行: $rowByPos")

// 切片选择（需要手动实现）
// val slice = df.slice(1, 3)
```

### 3.4.3 条件筛选

```kotlin
// 筛选年龄大于28的行
val filtered = df.filter { row ->
    val ageSeries = row["age"]
    val age = ageSeries?.get(0) as? Int
    age != null && age > 28
}
println("年龄 > 28:\n$filtered")

// 多条件筛选
val filtered2 = df.filter { row ->
    val age = row["age"]?.get(0) as? Int
    val salary = row["salary"]?.get(0) as? Int
    age != null && salary != null && age > 25 && salary > 55000
}
println("年龄 > 25 且 薪资 > 55000:\n$filtered2")
```

### 3.4.4 单元格值获取

```kotlin
// 获取指定单元格值
val value1 = df.at(0, "name")  // "Alice"
val value2 = df.at(1, "age")   // 30

// 按标签获取单元格
val value3 = df.at(0, "salary")  // 50000

println("单元格值: $value1, $value2, $value3")
```

## 3.5 数据操作

### 3.5.1 新增列

```kotlin
val df = andasDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie"),
        "age" to listOf(25, 30, 35),
        "salary" to listOf(50000, 60000, 70000)
    )
)

// 新增计算列
val dfWithBonus = df.addColumn("bonus") { row ->
    val salary = row["salary"]?.get(0) as? Int
    salary?.times(0.1) ?: 0.0
}
println("新增奖金列:\n$dfWithBonus")

// 新增分类列
val dfWithCategory = df.addColumn("category") { row ->
    val age = row["age"]?.get(0) as? Int
    when (age) {
        in 20..29 -> "青年"
        in 30..39 -> "中年"
        else -> "其他"
    }
}
println("新增分类列:\n$dfWithCategory")

// 直接添加数据列
val newData = listOf(1000, 2000, 3000)
val dfWithNewCol = df.addColumn("bonus_amount", newData)
println("新增数据列:\n$dfWithNewCol")
```

### 3.5.2 删除列

```kotlin
// 删除单列
val dfWithoutAge = df.dropColumns("age")
println("删除年龄列:\n$dfWithoutAge")

// 删除多列
val dfSimple = df.dropColumns("age", "salary")
println("删除多列:\n$dfSimple")
```

### 3.5.3 重命名列

```kotlin
// 重命名列
val dfRenamed = df.rename(
    mapOf(
        "name" to "姓名",
        "age" to "年龄",
        "salary" to "薪资"
    )
)
println("重命名后:\n$dfRenamed")
```

### 3.5.4 复制 DataFrame

```kotlin
// 复制 DataFrame
val dfCopy = df.copy()
println("复制的 DataFrame:\n$dfCopy")

// 复制并修改部分属性
val dfModified = df.copy(
    columns = listOf("name", "age"),  // 只保留部分列
    index = df.index().take(2)  // 只保留前2行
)
println("修改后的复制:\n$dfModified")
```

## 3.6 统计计算

### 3.6.1 基础统计（优先使用原生方法）

```kotlin
val df = andasDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie", "David"),
        "age" to listOf(25, 30, 35, 40),
        "salary" to listOf(50000, 60000, 70000, 80000),
        "math" to listOf(85.0, 92.0, 78.0, 88.0),
        "english" to listOf(76.0, 85.0, 90.0, 82.0)
    )
)

// 基础统计（自动选择原生或Kotlin实现）
val mathSum = df.sum("math")        // 求和
val ageMean = df.mean("age")        // 平均值
val salaryMax = df.max("salary")    // 最大值
val ageMin = df.min("age")          // 最小值
val mathStd = df.std("math")        // 标准差
val englishVar = df.variance("english")  // 方差

println("数学总和: $mathSum")
println("年龄平均值: $ageMean")
println("工资最大值: $salaryMax")
println("年龄最小值: $ageMin")
println("数学标准差: $mathStd")
println("英语方差: $englishVar")
```

### 3.6.2 相关系数矩阵

```kotlin
// 计算相关系数矩阵
val correlation = df.corr()
println("相关系数矩阵:\n$correlation")
```

### 3.6.3 统计描述

```kotlin
// 对单列进行描述性统计
val mathStats = df.describe("math")
println("数学成绩统计:")
mathStats.forEach { (key, value) ->
    println("  $key: $value")
}

// 输出示例:
// count: 4.0
// mean: 85.75
// std: 5.56
// min: 78.0
// max: 92.0
```

### 3.6.4 归一化

```kotlin
// 对数学成绩进行标准归一化
val normalizedDF = df.normalize("math")
println("归一化后的数学成绩:\n$normalizedDF")
```

## 3.7 向量化运算

### 3.7.1 向量化加法

```kotlin
// 创建两个数值列
val df = andasDataFrame(
    mapOf(
        "col1" to listOf(1.0, 2.0, 3.0, 4.0),
        "col2" to listOf(10.0, 20.0, 30.0, 40.0)
    )
)

// 向量化加法
val resultDF = df.vectorizedAdd("col1", "col2", "sum")
println("向量化加法结果:\n$resultDF")
// 输出: col1, col2, sum
//      1.0, 10.0, 11.0
//      2.0, 20.0, 22.0
//      3.0, 30.0, 33.0
//      4.0, 40.0, 44.0
```

### 3.7.2 向量化乘法

```kotlin
val resultDF = df.vectorizedMultiply("col1", "col2", "product")
println("向量化乘法结果:\n$resultDF")
// 输出: col1, col2, product
//      1.0, 10.0, 10.0
//      2.0, 20.0, 40.0
//      3.0, 30.0, 90.0
//      4.0, 40.0, 160.0
```

### 3.7.3 点积计算

```kotlin
val dotProduct = df.dotProduct("col1", "col2")
println("点积: $dotProduct")  // 1*10 + 2*20 + 3*30 + 4*40 = 300
```

### 3.7.4 范数计算

```kotlin
val norm = df.norm("col1")
println("范数: $norm")  // sqrt(1^2 + 2^2 + 3^2 + 4^2) = sqrt(30) ≈ 5.477
```

## 3.8 空值处理

### 3.8.1 检测空值

```kotlin
val df = andasDataFrame(
    mapOf(
        "name" to listOf("Alice", null, "Charlie", "David"),
        "age" to listOf(25, 30, null, 40),
        "salary" to listOf(50000, 60000, 70000, null)
    )
)

// 检查空值
val nullDF = df.isnull()
println("空值检测:\n$nullDF")

// 检查非空值
val notNullDF = df.notnull()
println("非空值检测:\n$notNullDF")

// 查找空值索引
val nullIndices = df.findNullIndices("name")
println("name列空值索引: $nullIndices")
```

### 3.8.2 处理空值

```kotlin
// 丢弃包含空值的行
val cleanedDF = df.dropna()
println("删除空值行:\n$cleanedDF")

// 填充空值
val filledDF = df.fillna("Unknown")
println("填充空值:\n$filledDF")

// 原生方法填充空值（数值类型）
val filledNativeDF = df.fillNull("salary", 0.0)
println("原生填充空值:\n$filledNativeDF")

// 丢弃指定列的空值
val droppedDF = df.dropNullValues("age")
println("丢弃age列空值:\n$droppedDF")
```

## 3.9 分组与聚合

### 3.9.1 分组求和

```kotlin
val df = andasDataFrame(
    mapOf(
        "department" to listOf("IT", "HR", "IT", "Finance", "HR"),
        "salary" to listOf(50000, 60000, 70000, 80000, 65000)
    )
)

// 分组求和
val groupedSum = df.groupBySum("department", "salary")
println("部门薪资总和:\n$groupedSum")
```

### 3.9.2 通用分组聚合

```kotlin
// 使用 GroupBy 类
val groupBy = df.groupBy("department")

// 求和
val sumResult = groupBy.sum()
println("分组求和:\n$sumResult")

// 平均值
val meanResult = groupBy.mean()
println("分组平均:\n$meanResult")

// 计数
val countResult = groupBy.count()
println("分组计数:\n$countResult")

// 自定义聚合
val aggResult = df.agg(
    mapOf(
        "salary" to "sum",
        "age" to "mean"
    )
)
println("自定义聚合:\n$aggResult")
```

## 3.10 排序与筛选

### 3.10.1 排序

```kotlin
val df = andasDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie", "David"),
        "age" to listOf(25, 30, 35, 40),
        "salary" to listOf(50000, 60000, 70000, 80000)
    )
)

// 按年龄升序排序
val sortedAsc = df.sortValues("age", descending = false)
println("按年龄升序:\n$sortedAsc")

// 按工资降序排序
val sortedDesc = df.sortValues("salary", descending = true)
println("按工资降序:\n$sortedDesc")

// 获取排序索引
val indices = df.sortIndices("age")
println("年龄排序索引: $indices")
```

### 3.10.2 高级筛选

```kotlin
// 大于阈值筛选
val filtered = df.filterGreaterThan("salary", 65000)
println("工资 > 65000:\n$filtered")

// 布尔索引
val whereResult = df.where("age", 30)
println("年龄 > 30:\n$whereResult")
```

## 3.11 数据合并与连接

### 3.11.1 数据合并

```kotlin
val df1 = andasDataFrame(
    mapOf(
        "id" to listOf(1, 2, 3),
        "name" to listOf("Alice", "Bob", "Charlie")
    )
)

val df2 = andasDataFrame(
    mapOf(
        "id" to listOf(2, 3, 4),
        "salary" to listOf(60000, 70000, 80000)
    )
)

// 合并
val merged = df1.merge(df2, "id")
println("合并结果:\n$merged")
```

### 3.11.2 SQL风格连接

```kotlin
// 内连接（默认）
val innerJoin = df1.join(df2, "id", "inner")

// 左连接
val leftJoin = df1.join(df2, "id", "left")

// 右连接
val rightJoin = df1.join(df2, "id", "right")

// 全外连接
val outerJoin = df1.join(df2, "id", "outer")

println("内连接:\n$innerJoin")
println("左连接:\n$leftJoin")
println("右连接:\n$rightJoin")
println("全外连接:\n$outerJoin")
```

### 3.11.3 多表合并

```kotlin
val df3 = andasDataFrame(
    mapOf(
        "id" to listOf(1, 2, 3),
        "department" to listOf("IT", "HR", "Finance")
    )
)

// 合并多个DataFrame
val multiMerged = df1.mergeMultiple(listOf(df2, df3), "id")
println("多表合并:\n$multiMerged")
```

## 3.12 数据采样与批量处理

### 3.12.1 数据采样

```kotlin
val df = andasDataFrame(
    mapOf(
        "id" to (1..1000).toList(),
        "value" to (1..1000).map { it * 1.5 }
    )
)

// 随机采样
val sample = df.sample(100)  // 采样100条
println("采样结果:\n$sample")
println("采样形状: ${sample.shape()}")
```

### 3.12.2 批量处理

```kotlin
// 批量处理（适用于大数据集）
val processedDF = df.processBatch("value", batchSize = 1000)
println("批量处理完成，形状: ${processedDF.shape()}")
```

## 3.13 数据导出与导入

### 3.13.1 CSV 导出

```kotlin
import java.io.File

// 导出到文件
val file = File(context.filesDir, "output.csv")
df.toCSV(file)
println("导出成功: ${file.absolutePath}")
```

### 3.13.2 使用 DataFrameIO 类

```kotlin
import cn.ac.oac.libs.andas.entity.DataFrameIO

// 从Assets读取
val dfFromAssets = DataFrameIO.readFromAssets(
    context.assets,
    "data.csv"
)

// 从私有存储读取
val dfFromPrivate = DataFrameIO.readFromPrivateStorage(
    context,
    "data.csv"
)

// 从外部存储读取（需要权限）
val dfFromExternal = DataFrameIO.readFromExternalStorage(
    "/sdcard/data.csv"
)

// 保存到私有存储
DataFrameIO.saveToPrivateStorage(context, "output.csv", df)

// 保存到外部存储（需要权限）
DataFrameIO.saveToExternalStorage("/sdcard/output.csv", df)
```

### 3.13.3 异步IO操作

```kotlin
// 异步读取Assets
DataFrameIO.readFromAssetsAsync(
    context.assets,
    "large_data.csv",
    onSuccess = { df ->
        println("异步读取成功: ${df.shape()}")
    },
    onError = { error ->
        println("读取失败: ${error.message}")
    }
)

// 异步保存
DataFrameIO.saveToPrivateStorageAsync(
    context,
    "output.csv",
    df,
    onSuccess = {
        println("异步保存成功")
    },
    onError = { error ->
        println("保存失败: ${error.message}")
    }
)
```

## 3.14 扩展函数

### 3.14.1 便捷IO操作

```kotlin
import cn.ac.oac.libs.andas.entity.saveToPrivateStorage
import cn.ac.oac.libs.andas.entity.saveToExternalStorage

// 使用扩展函数保存
df.saveToPrivateStorage(context, "data.csv")
df.saveToExternalStorage("/sdcard/data.csv")
```

## 3.15 性能优化建议

### 3.15.1 原生方法优先

```kotlin
//  推荐：自动选择最优实现
val sum = df.sum("salary")
val mean = df.mean("age")

//  避免：手动实现复杂逻辑
val manualSum = df["salary"]?.values()
    ?.filterNotNull()
    ?.sumOf { (it as Number).toDouble() }
```

### 3.15.2 批量操作

```kotlin
//  推荐：批量处理大数据集
val largeDF = andasDataFrame(
    mapOf(
        "data" to (1..1000000).map { it * 1.0 }
    )
)
val result = largeDF.processBatch("data", 10000)

//  避免：逐行处理
// 不推荐的逐行处理方式
```

### 3.15.3 链式操作

```kotlin
//  推荐：链式操作
val result = df
    .filterGreaterThan("age", 25)
    .addColumn("category") { row ->
        val age = row["age"]?.get(0) as? Int
        when (age) {
            in 20..29 -> "青年"
            in 30..39 -> "中年"
            else -> "其他"
        }
    }
    .groupBy("category")
    .mean()

//  避免：创建大量临时变量
```

## 3.16 错误处理

### 3.16.1 常见异常处理

```kotlin
try {
    val df = andasDataFrame(
        mapOf(
            "name" to listOf("Alice", "Bob"),
            "age" to listOf(25, 30)
        )
    )
    
    // 列不存在
    val value = df.at(0, "nonexistent")  // 抛出 IllegalArgumentException
    
} catch (e: IllegalArgumentException) {
    println("列不存在: ${e.message}")
} catch (e: IndexOutOfBoundsException) {
    println("索引越界: ${e.message}")
} catch (e: Exception) {
    println("其他错误: ${e.message}")
}
```

### 3.16.2 原生库可用性检查

```kotlin
if (df.isNativeAvailable()) {
    println("使用原生方法加速")
    val sum = df.sum("salary")
} else {
    println("使用Kotlin实现")
    val sum = df.sum("salary")  // 自动回退到Kotlin实现
}
```

## 3.17 小结

本章详细介绍了 DataFrame API 的使用方法，包括：

1. **创建 DataFrame**：支持工厂函数和构造函数两种方式
2. **数据查看**：基本信息、形状、列名、数据类型
3. **数据选择**：列选择、行选择、条件筛选、单元格获取
4. **数据操作**：新增列、删除列、重命名、复制
5. **统计计算**：基础统计、相关系数、描述性统计、归一化
6. **向量化运算**：加法、乘法、点积、范数
7. **空值处理**：检测、丢弃、填充
8. **分组聚合**：分组求和、通用聚合
9. **排序筛选**：排序、高级筛选
10. **数据合并**：合并、SQL风格连接、多表合并
11. **采样批量**：数据采样、批量处理
12. **数据导出**：CSV导出、异步IO、扩展函数
13. **性能优化**：原生方法、批量操作、链式操作
14. **错误处理**：异常捕获、可用性检查

DataFrame 是 Andas SDK 的核心数据结构，提供了丰富的数据操作功能，是进行数据分析的主要工具。通过合理使用原生方法和批量操作，可以显著提升数据处理性能。
