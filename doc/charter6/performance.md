## 第六章：性能优化和最佳实践

### 6.1 性能优化概述

Andas SDK 通过多种技术手段实现了高性能数据处理，包括 JNI 原生代码、OpenMP 并行计算、内存优化等。本章将介绍如何充分利用这些特性，以及在实际应用中的最佳实践。

**优化目标：**
- ⚡ 最大化计算性能
- 📊 最小化内存占用
- 🔄 提供流畅的用户体验
- 🛡️ 确保稳定性和可靠性

### 6.2 Series 创建性能

#### 6.2.1 高效创建 Series

```kotlin
// ✅ 推荐：直接创建大数据 Series
val data = (1..100000).map { it }
val start = System.currentTimeMillis()
val series = Andas.getInstance().createSeries(data)
val doubled = series * 2
val time = System.currentTimeMillis() - start

println("数据量: ${data.size}")
println("耗时: ${time}ms")
```

**输出示例：**
```
数据量: 100000
耗时: 15ms
```

#### 6.2.2 批量操作优化

```kotlin
// ✅ 推荐：使用原生方法进行批量处理
val largeData = (1..500000).map { it * 1.0 }
val start = System.currentTimeMillis()

val series = Andas.getInstance().createSeries(largeData)
val processed = series.normalize()  // 原生归一化

val time = System.currentTimeMillis() - start
println("批量处理 ${largeData.size} 条数据，耗时: ${time}ms")
```

### 6.3 JNI vs Kotlin 性能对比

#### 6.3.1 求和性能对比

```kotlin
 val data = (1..100000).map { it.toDouble() }
val series = Andas.getInstance().createSeries(
    data
)
// Kotlin 实现
val start1 = System.currentTimeMillis()
val kotlinSum = series.sumKt()
val time1 = System.currentTimeMillis() - start1

// JNI 实现
val start2 = System.currentTimeMillis()
val jniSum = series.sum()
val time2 = System.currentTimeMillis() - start2
// 性能对比
val speedup = if (time2 > 0) "%.2f".format(time1.toDouble() / time2) else "N/A"

println("=== 性能对比 ===")
println("Kotlin 求和: ${time1}ms (结果: $kotlinSum)")
println("JNI 求和: ${time2}ms (结果: $jniSum)")
println("性能加速比: ${speedup}x")
```

**典型输出：**
```
=== 性能对比 ===
Kotlin 求和: 92ms (结果: 5000050000.0)
JNI 求和: 65ms (结果: 5000050000.0)
性能加速比: 1.51x
```

#### 6.3.2 不同数据规模的性能表现

```kotlin
fun benchmarkDifferentSizes() {
    val sizes = listOf(1000, 10000, 100000, 1000000)
    
    for (size in sizes) {
        val data = (1..size).map { it.toDouble() }
        
        // Kotlin
        val kotlinStart = System.currentTimeMillis()
        val kotlinSum = data.sum()
        val kotlinTime = System.currentTimeMillis() - kotlinStart
        
        // JNI
        val jniStart = System.currentTimeMillis()
        val jniSum = NativeMath.sumDoubleArray(data.toDoubleArray())
        val jniTime = System.currentTimeMillis() - jniStart
        
        val speedup = if (jniTime > 0) "%.2f".format(kotlinTime.toDouble() / jniTime) else "N/A"
        
        println("大小: $size | Kotlin: ${kotlinTime}ms | JNI: ${jniTime}ms | 加速: ${speedup}x")
    }
}
```

### 6.4 内存管理优化

#### 6.4.1 及时释放资源

```kotlin
// ✅ 推荐：使用 try-finally 确保资源释放
fun processLargeDataset() {
    val largeDf = Andas.getInstance().readCSVFromAssets("large_dataset.csv")
    
    try {
        // 处理数据
        val result = largeDf.corr()
        // 使用结果
        processResult(result)
    } finally {
        // 帮助垃圾回收
        // 在适当时候，可以显式地清理引用
    }
}

// ✅ 推荐：在 ViewModel 中正确管理生命周期
class DataViewModel : ViewModel() {
    private var cachedDf: DataFrame? = null
    
    fun loadData() {
        // 清理旧数据
        cachedDf = null
        
        // 加载新数据
        Andas.getInstance().readCSVFromAssetsAsync(
            "data.csv",
            onSuccess = { df ->
                cachedDf = df
                // 处理数据
            },
            onError = { error ->
                println("加载失败: ${error.message}")
            }
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        // 确保清理
        cachedDf = null
    }
}
```

#### 6.4.2 分批处理大文件

```kotlin
// ✅ 推荐：分批处理避免内存溢出
fun processLargeCSV() {
    Andas.getInstance().readCSVFromAssetsAsync(
        "very_large.csv",
        onSuccess = { df ->
            val batchSize = 1000
            val totalRows = df.shape().first
            
            for (i in 0 until totalRows step batchSize) {
                val end = minOf(i + batchSize, totalRows)
                val batch = df.slice(i, end)
                
                // 处理这一批数据
                processBatch(batch)
                
                // 定期建议 GC
                if (i % (batchSize * 10) == 0) {
                    System.gc()
                }
            }
        },
        onError = { error ->
            println("处理失败: ${error.message}")
        }
    )
}
```

### 6.5 计算性能优化

#### 6.5.1 充分利用原生计算

```kotlin
// ✅ 推荐：使用原生统计方法
val df = Andas.getInstance().createDataFrame(
    mapOf(
        "data" to (1..100000).map { it * 1.1 }
    )
)

// 原生方法（快速）
val sum = df.sumNative("data")
val mean = df.meanNative("data")
val std = df.stdNative("data")

// ❌ 避免：手动实现统计函数
val slowMean = df["data"]?.toList()?.average()  // 慢很多
```

#### 6.5.2 并行计算配置

```kotlin
// ✅ 推荐：根据设备配置优化线程池
val cores = Runtime.getRuntime().availableProcessors()
val config = Andas.AndasConfig().apply {
    // 通常设置为 CPU 核心数的 1-2 倍
    maxConcurrentTasks = cores * 2
    memoryOptimization = true
    timeoutSeconds = 120L  // 大数据集需要更长超时
}

Andas.initialize(context, config)
```

### 6.6 异步操作优化

#### 6.6.1 选择合适的异步方法

```kotlin
// ✅ 推荐：CPU 密集型任务使用 executeAsync
Andas.getInstance().executeAsync(
    task = {
        val df = createLargeDataFrame()
        df.corr()  // 计算密集型
    },
    onSuccess = { result ->
        println("计算完成")
    },
    onError = { error ->
        println("计算失败")
    }
)

// ✅ 推荐：IO 密集型任务使用 executeIO
Andas.getInstance().executeIO(
    task = {
        val file = File(context.filesDir, "data.csv")
        Andas.getInstance().readCSV(file)
    },
    onSuccess = { df ->
        println("文件读取完成")
    },
    onError = { error ->
        println("读取失败")
    }
)
```

#### 6.6.2 链式异步操作

```kotlin
// ✅ 推荐：使用链式操作减少回调嵌套
fun processDataFlow() {
    Andas.getInstance().readCSVFromAssetsAsync(
        "input.csv",
        onSuccess = { df ->
            // 数据预处理
            val cleaned = df.dropRowsWithNull()
            
            // 计算统计信息
            val stats = mapOf(
                "mean" to cleaned.meanNative("value"),
                "std" to cleaned.stdNative("value")
            )
            
            // 保存结果
            Andas.getInstance().saveToPrivateStorageAsync(
                "result.csv", cleaned,
                onSuccess = {
                    println("完整流程完成: $stats")
                },
                onError = { error ->
                    println("保存失败: ${error.message}")
                }
            )
        },
        onError = { error ->
            println("读取失败: ${error.message}")
        }
    )
}
```

### 6.7 数据处理优化

#### 6.7.1 高效的数据选择

```kotlin
val df = Andas.getInstance().createDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie", "David"),
        "age" to listOf(25, 30, 35, 40),
        "salary" to listOf(50000, 60000, 70000, 80000)
    )
)

// ✅ 推荐：使用多列选择
val selected = df[listOf("name", "salary")]  // 高效

// ✅ 推荐：使用条件筛选
val filtered = df.filter { row ->
    row.getInt("age") > 28
}

// ❌ 避免：循环逐行处理
val slowResult = df.toList().filter { it["age"] as Int > 28 }  // 慢
```

#### 6.7.2 批量操作优化

```kotlin
// ✅ 推荐：批量处理大数据集
val largeDf = Andas.getInstance().createDataFrame(
    mapOf(
        "id" to (1..100000).toList(),
        "value" to (1..100000).map { it * 1.5 }
    )
)

// 使用批量处理
val result = largeDf.processBatch(batchSize = 5000) { batch ->
    // 对每批进行处理
    batch.map { row ->
        mapOf(
            "id" to row.getInt("id"),
            "doubled" to row.getDouble("value") * 2
        )
    }
}
```

### 6.8 错误处理和稳定性

#### 6.8.1 完整的错误处理

```kotlin
// ✅ 推荐：全面的错误处理
fun safeDataProcessing() {
    try {
        Andas.getInstance().readCSVFromAssetsAsync(
            "data.csv",
            onSuccess = { df ->
                try {
                    // 数据验证
                    if (df.shape().first == 0) {
                        println("数据为空")
                        return@readCSVFromAssetsAsync
                    }
                    
                    // 执行计算
                    val result = df.corr()
                    println("计算成功: $result")
                    
                } catch (e: Exception) {
                    println("数据处理错误: ${e.message}")
                }
            },
            onError = { error ->
                when (error) {
                    is FileNotFoundException -> {
                        println("文件不存在")
                    }
                    is SecurityException -> {
                        println("权限不足")
                    }
                    else -> {
                        println("未知错误: ${error.message}")
                    }
                }
            }
        )
    } catch (e: Exception) {
        println("初始化错误: ${e.message}")
    }
}
```

#### 6.8.2 资源清理

```kotlin
// ✅ 推荐：在适当时候清理资源
class DataProcessor : ViewModel() {
    private var currentDf: DataFrame? = null
    
    fun processNewData() {
        // 清理旧数据
        currentDf = null
        
        // 加载新数据
        Andas.getInstance().readCSVFromAssetsAsync(
            "new_data.csv",
            onSuccess = { df ->
                currentDf = df
                // 处理数据
            },
            onError = { error ->
                println("加载失败: ${error.message}")
            }
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        // 确保清理
        currentDf = null
    }
}
```

### 6.9 性能监控和调试

#### 6.9.1 性能基准测试

```kotlin
// ✅ 推荐：进行性能基准测试
fun benchmarkPerformance() {
    val sizes = listOf(1000, 10000, 100000, 1000000)
    
    for (size in sizes) {
        val series = Andas.getInstance().createSeries(
            (1..size).map { it.toDouble() }
        )
        
        // 测试原生操作
        val nativeStart = System.currentTimeMillis()
        val nativeSum = series.sum()
        val nativeTime = System.currentTimeMillis() - nativeStart
        
        // 测试 Kotlin 操作
        val kotlinStart = System.currentTimeMillis()
        val kotlinSum = series.toList().sum()
        val kotlinTime = System.currentTimeMillis() - kotlinStart
        
        println("大小: $size")
        println("  原生: ${nativeTime}ms")
        println("  Kotlin: ${kotlinTime}ms")
        println("  加速比: ${"%.2f".format(kotlinTime.toDouble() / nativeTime)}x")
    }
}
```

#### 6.9.2 调试模式

```kotlin
// ✅ 推荐：开发阶段开启调试模式
val config = Andas.AndasConfig().apply {
    debugMode = BuildConfig.DEBUG
    logLevel = if (BuildConfig.DEBUG) Andas.LogLevel.DEBUG else Andas.LogLevel.INFO
    maxConcurrentTasks = if (BuildConfig.DEBUG) 1 else Runtime.getRuntime().availableProcessors() * 2
}

Andas.initialize(context, config)

// 在调试模式下，可以查看详细统计
if (BuildConfig.DEBUG) {
    val stats = Andas.getInstance().getStats()
    println("SDK 统计: $stats")
}
```

### 6.10 平台特定优化

#### 6.10.1 Android 特定优化

```kotlin
// ✅ 推荐：在 Application 中初始化
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 在应用启动时初始化
        val cores = Runtime.getRuntime().availableProcessors()
        Andas.initialize(this) {
            debugMode = BuildConfig.DEBUG
            logLevel = if (BuildConfig.DEBUG) Andas.LogLevel.DEBUG else Andas.LogLevel.INFO
            maxConcurrentTasks = cores * 2
            timeoutSeconds = 120L
            memoryOptimization = true
        }
    }
}

// ✅ 推荐：在后台线程处理大数据
class DataRepository {
    suspend fun loadData(): DataFrame = withContext(Dispatchers.IO) {
        // IO 操作在后台线程
        Andas.getInstance().readCSVFromAssets("data.csv")
    }
    
    suspend fun processData(df: DataFrame): DataFrame = withContext(Dispatchers.Default) {
        // CPU 密集型操作在默认线程池
        df.corr()
    }
}
```

#### 6.10.2 内存敏感优化

```kotlin
// ✅ 推荐：根据设备内存调整策略
fun getOptimalBatchSize(): Int {
    val maxMemory = Runtime.getRuntime().maxMemory()
    val availableMemory = Runtime.getRuntime().freeMemory()
    
    return when {
        maxMemory > 512 * 1024 * 1024 -> 5000  // 512MB+，大批次
        maxMemory > 256 * 1024 * 1024 -> 2000  // 256MB+，中等批次
        else -> 1000  // 小批次
    }
}

// 使用优化的批次大小
val batchSize = getOptimalBatchSize()
```

### 6.11 最佳实践总结

#### 6.11.1 性能优化检查清单

```kotlin
/**
 * Andas SDK 性能优化检查清单
 * 
 * 1. 数据结构选择
 *    ✅ 使用合适的数据类型
 *    ✅ 避免不必要的类型转换
 *    
 * 2. 内存管理
 *    ✅ 及时释放大对象
 *    ✅ 分批处理大数据集
 *    ✅ 在适当时候建议 GC
 *    
 * 3. 计算优化
 *    ✅ 使用原生统计方法
 *    ✅ 配置合适的线程池
 *    ✅ 利用并行计算
 *    
 * 4. 异步操作
 *    ✅ 选择合适的异步方法
 *    ✅ 链式操作减少嵌套
 *    ✅ 完整的错误处理
 *    
 * 5. 数据处理
 *    ✅ 高效的数据选择
 *    ✅ 批量操作
 *    ✅ 避免循环处理
 *    
 * 6. 错误处理
 *    ✅ 全面的异常捕获
 *    ✅ 合理的错误恢复
 *    ✅ 资源清理
 *    
 * 7. 监控调试
 *    ✅ 性能基准测试
 *    ✅ 调试模式配置
 *    ✅ 统计信息监控
 */
```

#### 6.11.2 性能优化示例

```kotlin
// ✅ 完整的优化示例
class OptimizedDataProcessor(private val context: Context) {
    
    private val batchSize = getOptimalBatchSize()
    
    // 1. 高效的数据加载
    suspend fun loadDataAsync(fileName: String): DataFrame = 
        withContext(Dispatchers.IO) {
            Andas.getInstance().readCSVFromAssets(fileName)
        }
    
    // 2. 优化的数据处理
    suspend fun processData(df: DataFrame): Result = 
        withContext(Dispatchers.Default) {
            try {
                // 使用原生方法
                val stats = mapOf(
                    "mean" to df.meanNative("value"),
                    "std" to df.stdNative("value"),
                    "corr" to df.corr()
                )
                
                // 分批处理大结果
                if (df.shape().first > batchSize) {
                    processInBatches(df)
                }
                
                Result.Success(stats)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    
    // 3. 分批处理
    private fun processInBatches(df: DataFrame) {
        val totalRows = df.shape().first
        for (i in 0 until totalRows step batchSize) {
            val end = minOf(i + batchSize, totalRows)
            val batch = df.slice(i, end)
            
            // 处理批次
            processBatch(batch)
            
            // 内存管理
            if (i % (batchSize * 5) == 0) {
                System.gc()
            }
        }
    }
    
    // 4. 高效保存
    suspend fun saveDataAsync(df: DataFrame, fileName: String): Result = 
        withContext(Dispatchers.IO) {
            try {
                Andas.getInstance().saveToPrivateStorageAsync(
                    fileName, df,
                    onSuccess = { Result.Success(Unit) },
                    onError = { Result.Error(it) }
                )
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
}

// 使用示例
class MainViewModel : ViewModel() {
    private val processor = OptimizedDataProcessor(getApplication())
    
    fun loadAndProcess() {
        viewModelScope.launch {
            when (val loadResult = processor.loadDataAsync("data.csv")) {
                is Result.Success -> {
                    val processed = processor.processData(loadResult.data)
                    // 更新 UI
                }
                is Result.Error -> {
                    // 处理错误
                }
            }
        }
    }
}
```

### 6.12 小结

本章详细介绍了 Andas SDK 的性能优化策略，包括：

1. **Series 创建性能**：高效创建和批量操作
2. **JNI vs Kotlin 对比**：实际性能差异和加速比
3. **内存管理**：资源释放、分批处理、GC 管理
4. **计算性能**：原生方法、并行计算、线程池配置
5. **异步操作**：方法选择、链式操作、错误处理
6. **数据处理**：高效选择、批量操作、避免循环
7. **错误处理**：全面异常处理、资源清理
8. **性能监控**：基准测试、调试模式、统计监控
9. **平台优化**：Android 特定优化、内存敏感优化

通过遵循这些最佳实践，可以充分发挥 Andas SDK 的性能优势，构建高效、稳定的数据处理应用。
