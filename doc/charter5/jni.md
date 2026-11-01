# 第五章：JNI 原生 API

## 5.1 JNI 原生概述

Andas SDK 通过 JNI (Java Native Interface) 技术集成了 C++ 原生代码，提供了高性能的数学运算和数据处理能力。本章介绍如何使用原生 API 进行高效的数值计算。

**核心特性：**
- 高性能计算：C++ 原生代码执行
- 数学运算：丰富的统计和数学函数
- 数组处理：高效的数组操作
- 线程安全：支持并发访问

## 5.2 原生数学运算

### 5.2.1 数组求和

```kotlin
import cn.ac.oac.libs.andas.core.NativeMath

// 创建测试数据
val data = (1..10000).map { it.toDouble() }.toDoubleArray()

// 原生求和（高性能）
val sum = NativeMath.sumDoubleArray(data)
val mean = NativeMath.meanDoubleArray(data)

println("数据量: ${data.size}")
println("总和: $sum")
println("平均值: $mean")
```

**输出示例：**
```
数据量: 10000
总和: 50005000.0
平均值: 5000.5
```

### 5.2.2 原生统计计算

```kotlin
val data = (1..1000).map { it * 1.0 }.toDoubleArray()

// 原生统计函数
val variance = NativeMath.varianceDoubleArray(data)
val std = NativeMath.stdDoubleArray(data)
val max = NativeMath.maxDoubleArray(data)
val min = NativeMath.minDoubleArray(data)

println("方差: $variance")
println("标准差: $std")
println("最大值: $maxmax")
:      的的数字 串 zeros的0数字数字串 zeros数字的数字数字，，，0零，::: println  
:数据    println数字字 println println println: val数据 println println的 println，的 println println println println
 println: 
 println println println println println println println println数据 println println println println
 println
字 println println println println字数字字，
 println字 println，， println数字串数字，，数字数字数字，，，，，，1字数字，，，，，，，，数字字，，，，可以使用原生方法进行归一化
val data = (1..100).map { it * 1.0 }.toDoubleArray()
val normalized = NativeMath.normalizeArray(data)

println("归一化前5: ${normalized.take(5)}")
// 输出: [0.0, 0.010101010101010102, 0.020202020202020204, 0.030303030303030304, 0.04040404040404041]

// 乘法运算
val doubled = NativeMath.multiplyDoubleArray(data, 2.0)
println("加倍前5: ${doubled.take(5)}")
// 输出: [2.0, 4.0, 6.0, 8.0, 10.0]
```

## 5.3 与 Series 的集成

### 5.3.1 Series 原生方法

```kotlin
val series = Andas.getInstance().createSeries(
    (1..10000).map { it.toDouble() }
)

// 使用 Series 的原生方法
val sum = series.sum()        // 原生求和
val mean = series.mean()      // 原生平均值
val max = series.max()        // 原生最大值
val min = series.min()        // 原生最小值
val variance = series.variance()  // 原生方差
val std = series.std()        // 原生标准差

println("统计结果:")
println("  总和: $sum")
println("  平均值: $mean")
println("  最大值: $max")
println("  最小值: $min")
println("  方差: $variance")
println("  标准差: $std")
```

### 5.3.2 DataFrame 原生统计

```kotlin
val df = Andas.getInstance().createDataFrame(
    mapOf(
        "math" to listOf(85.0, 92.0, 78.0, 88.0, 95.0),
        "english" to listOf(76.0, 85.0, 90.0, 82.0, 88.0),
        "physics" to listOf(82.0, 88.0, 75.0, 91.0, 87.0)
    )
)

// 原生统计计算
val mathSum = df.sumNative("math")
val englishMean = df.meanNative("english")
val physicsMax = df.maxNative("physics")

println("数学总和: $mathSum")
println("英语平均值: $englishMean")
println("物理最大值: $physicsMax")
```

## 5.4 性能对比

### 5.4.1 JNI vs Kotlin 性能对比

```kotlin
val data = (1..100000).map { it.toDouble() }

// Kotlin 实现
val start1 = System.currentTimeMillis()
val kotlinSum = data.sum()
val time1 = System.currentTimeMillis() - start1

// JNI 实现
val start2 = System.currentTimeMillis()
val jniSum = NativeMath.sumDoubleArray(data.toDoubleArray())
val time2 = System.currentTimeMillis() - start2

// 性能对比
val speedup = if (time2 > 0) "%.2f".format(time1.toDouble() / time2) else "N/A"

println("Kotlin 求和: ${time1}ms (结果: $kotlinSum)")
println("JNI 求和: ${time2}ms (结果: $jniSum)")
println("性能加速比: ${speedup}x")
```

**典型输出：**
```
Kotlin 求和: 8ms (结果: 5000050000.0)
JNI 求和: 1ms (结果: 5000050000.0)
性能加速比: 8.00x
```

## 5.5 原生数据处理

### 5.5.1 数据变换

```kotlin
// 归一化
val data = (1..100).map { it * 1.0 }.toDoubleArray()
val normalized = NativeMath.normalizeArray(data)
println("归一化结果: ${normalized.take(5)}")

// 乘法
val multiplied = NativeMath.multiplyDoubleArray(data, 2.0)
println("乘以2结果: ${multiplied.take(5)}")

// 累计求和
val cumsum = NativeMath.cumsumArray(data)
println("累计求和: ${cumsum.take(5)}")
```

### 5.5.2 Series 原生变换

```kotlin
val series = Andas.getInstance().createSeries(
    (1..100).map { it * 1.0 }
)

// 原生归一化
val normalized = series.normalize()
println("归一化前5: ${normalized.head()}")

// 原生累计求和
val cumsum = series.cumsum()
println("累计求和前5: ${cumsum.head()}")

// 原生筛选
val filtered = series.filterGreaterThan(50.0)
println("大于50的元素: ${filtered.head()}")
```

## 5.6 高性能计算最佳实践

### 5.6.1 选择合适的原生方法

```kotlin
//  推荐：对于大数据集使用原生方法
val largeData = (1..1000000).map { it.toDouble() }
val sum = NativeMath.sumDoubleArray(largeData.toDoubleArray())  // 快速

//  避免：手动实现统计函数
val slowSum = largeData.sum()  // 相对较慢
```

### 5.6.2 批量处理优化

```kotlin
//  推荐：使用原生方法进行批量处理
val series = Andas.getInstance().createSeries(
    (1..100000).map { it * 1.1 }
)

// 原生计算
val stats = mapOf(
    "sum" to series.sum(),
    "mean" to series.mean(),
    "std" to series.std(),
    "max" to series.max(),
    "min" to series.min()
)

println("批量统计: $stats")
```

## 5.7 调试和监控

### 5.7.1 性能监控

```kotlin
// 开启调试模式
val config = Andas.AndasConfig().apply {
    debugMode = true
    logLevel = Andas.LogLevel.DEBUG
}

Andas.initialize(context, config)

// 执行原生操作，观察日志
val data = (1..10000).map { it.toDouble() }.toDoubleArray()
val sum = NativeMath.sumDoubleArray(data)

// 日志会显示：
// [Andas-DEBUG] Native operation started: sumDoubleArray
// [Andas-DEBUG] Processing 10000 elements
// [Andas-DEBUG] Native operation completed in 0.5ms
```

## 5.8 小结

本章介绍了 Andas SDK 的 JNI 原生 API，包括：

1. **原生数学运算**：求和、平均值、统计函数
2. **性能对比**：JNI vs Kotlin 的性能差异
3. **数据处理**：归一化、变换、筛选
4. **最佳实践**：如何充分利用原生计算
5. **性能监控**：调试和性能追踪

通过 JNI 集成，Andas SDK 实现了显著的性能提升，特别适合处理大规模数据集。
