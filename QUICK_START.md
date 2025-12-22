# Andas SDK 快速入门指南

## 1. 环境准备

### 系统要求
- Android Studio Arctic Fox 或更高版本
- Android SDK API 21+
- NDK 21+
- Kotlin 1.8+

### 项目配置

#### 1.1 添加模块依赖

在 `settings.gradle.kts` 中确保包含 android-pandas 模块：

```kotlin
include(":app")
```

#### 1.2 添加模块依赖

在 `app/build.gradle.kts` 中添加依赖：

```kotlin
dependencies {
    implementation(project(":android-pandas"))
    
    // Compose 依赖（如果使用）
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
```

#### 1.3 配置 NDK

在 `local.properties` 中确保 NDK 路径正确：

```properties
ndk.dir=/path/to/ndk
```

## 2. 初始化 SDK

### 2.1 基本初始化

在你的 `MainActivity` 或 Application 类中：

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化 Andas SDK
        Andas.initialize(this) {
            debugMode = true              // 开发阶段开启调试
            logLevel = Andas.LogLevel.DEBUG  // 日志级别
            timeoutSeconds = 60L          // 异步操作超时时间
            cachePath = "andas_cache"     // 缓存目录
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // 销毁 SDK，释放资源
        Andas.getInstance().destroy()
    }
}
```

### 2.2 在 AndroidManifest.xml 中注册

```xml
<application
    android:name=".MyApplication"
    ... >
    <!-- 你的 Activity 配置 -->
</application>
```

## 3. 创建第一个 DataFrame

### 3.1 基础示例

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 创建 DataFrame
        val df = Andas.getInstance().createDataFrame(
            mapOf(
                "name" to listOf("Alice", "Bob", "Charlie", "David"),
                "age" to listOf(25, 30, 35, 40),
                "salary" to listOf(50000, 60000, 70000, 80000),
                "department" to listOf("IT", "HR", "IT", "Finance")
            )
        )
        
        // 打印 DataFrame
        println(df)
        
        // 查看基本信息
        println("Shape: ${df.shape()}")      // (4, 4)
        println("Columns: ${df.columns()}")  // [name, age, salary, department]
    }
}
```

### 3.2 输出结果

```
DataFrame (4 rows x 4 columns)
name      age  salary  department
Alice     25   50000   IT
Bob       30   60000   HR
Charlie   35   70000   IT
David     40   80000   Finance
```

## 4. Series 基础操作

### 4.1 创建 Series

```kotlin
// 从列表创建
val numbers = Andas.getInstance().createSeries(
    listOf(1, 2, 3, 4, 5),
    name = "numbers"
)

// 从 Map 创建
val prices = Andas.getInstance().createSeriesFromMap(
    mapOf("Apple" to 150, "Banana" to 80, "Orange" to 120),
    name = "prices"
)
```

### 4.2 Series 操作

```kotlin
// 基本访问
println(numbers[2])        // 3
println(prices["Apple"])   // 150

// 数据查看
println(numbers.head(3))   // [1, 2, 3]
println(numbers.tail(2))   // [4, 5]

// 数学运算
val doubled = numbers * 2  // [2, 4, 6, 8, 10]

// 空值处理
val withNulls = Andas.getInstance().createSeries(
    listOf(1, null, 3, null, 5)
)
val cleaned = withNulls.dropna()  // [1, 3, 5]
val filled = withNulls.fillna(0)  // [1, 0, 3, 0, 5]

// 统计功能
val fruits = Andas.getInstance().createSeries(
    listOf("apple", "banana", "apple", "cherry", "banana")
)
println(fruits.unique())        // [apple, banana, cherry]
println(fruits.valueCounts())  // {apple=2, banana=2, cherry=1}
```

## 5. DataFrame 高级操作

### 5.1 数据筛选

```kotlin
val df = Andas.getInstance().createDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie", "David"),
        "age" to listOf(25, 30, 35, 40),
        "salary" to listOf(50000, 60000, 70000, 80000)
    )
)

// 筛选年龄大于30的记录
val filtered = df.filter { row ->
    (row["age"] as Int) > 30
}
// 结果: Charlie(35), David(40)
```

### 5.2 列操作

```kotlin
// 选择列
val selected = df.selectColumns("name", "salary")

// 添加列
val withBonus = df.addColumn("bonus", listOf(5000, 6000, 7000, 8000))

// 重命名列
val renamed = df.rename(mapOf("salary" to "income"))

// 删除列
val dropped = df.dropColumns("temp_column")
```

### 5.3 排序和分组

```kotlin
// 排序
val sorted = df.sortValues("age", descending = true)

// 分组聚合
val grouped = df.groupBy("department").mean()

// 多聚合
val result = df.agg(mapOf(
    "avg_age" to "mean",
    "max_salary" to "max",
    "count" to "count"
))
```

### 5.4 空值处理

```kotlin
val dfWithNulls = Andas.getInstance().createDataFrame(
    mapOf(
        "a" to listOf(1, null, 3),
        "b" to listOf(4, 5, null)
    )
)

// 移除空值行
val cleaned = dfWithNulls.dropna()

// 填充空值
val filled = dfWithNulls.fillna(0)
```

## 6. 异步操作

### 6.1 CSV 导出

```kotlin
val df = Andas.getInstance().createDataFrame(/* ... */)
val csvFile = File(cacheDir, "data.csv")

Andas.getInstance().exportCSVAsync(
    df,
    csvFile,
    onSuccess = {
        Log.i("CSV", "导出成功: ${csvFile.absolutePath}")
    },
    onError = { e ->
        Log.e("CSV", "导出失败", e)
    }
)
```

### 6.2 CSV 读取

```kotlin
val csvFile = File(cacheDir, "data.csv")

Andas.getInstance().readCSVAsync(
    csvFile,
    onSuccess = { df ->
        Log.i("CSV", "读取成功，${df.shape().first}行")
        // 处理 DataFrame
    },
    onError = { e ->
        Log.e("CSV", "读取失败", e)
    }
)
```

## 7. JNI 原生加速

### 7.1 检查可用性

```kotlin
val mathAvailable = NativeMath.isAvailable()
val dataAvailable = NativeData.isAvailable()

if (mathAvailable && dataAvailable) {
    Log.i("JNI", "原生库加载成功")
} else {
    Log.w("JNI", "原生库不可用，使用纯 Kotlin 实现")
}
```

### 7.2 高性能数学运算

```kotlin
// 准备数据
val data = (1..100000).map { it.toDouble() }.toDoubleArray()

// 原生乘法（比 Java 快 3-5 倍）
val result = NativeMath.multiplyDoubleArray(data, 2.0)

// 统计运算
val sum = NativeMath.sumDoubleArray(data)
val mean = NativeMath.meanDoubleArray(data)
val max = NativeMath.maxDoubleArray(data)
val min = NativeMath.minDoubleArray(data)

// 排序索引
val sortedIndices = NativeMath.argsort(data)

// 归一化
val normalized = NativeMath.normalize(data)
```

### 7.3 数据处理优化

```kotlin
// 空值处理
val nullData = doubleArrayOf(1.0, Double.NaN, 3.0, Double.NaN, 5.0)
val nullIndices = NativeData.findNullIndices(nullData)
val cleaned = NativeData.dropNullValues(nullData)

// 统计描述
val description = NativeData.describe(data)
// [count, mean, std, min, max]

// 采样
val sample = NativeData.sample(data, 1000)
```

## 8. 完整示例：数据处理应用

### 8.1 创建数据处理界面

```kotlin
@Composable
fun DataProcessorScreen() {
    var logOutput by remember { mutableStateOf("准备就绪\n") }
    var isProcessing by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 控制按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    isProcessing = true
                    processData({ log ->
                        logOutput += log
                    }, { isProcessing = false })
                },
                enabled = !isProcessing
            ) {
                Text("处理数据")
            }
            
            Button(
                onClick = { logOutput = "" },
                enabled = !isProcessing
            ) {
                Text("清空日志")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 日志输出
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Text(
                text = logOutput,
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                fontFamily = FontFamily.Monospace
            )
        }
        
        if (isProcessing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

private fun processData(
    onLog: (String) -> Unit,
    onComplete: () -> Unit
) {
    Thread {
        try {
            onLog("\n=== 开始数据处理 ===\n")
            
            // 1. 创建数据
            val df = Andas.getInstance().createDataFrame(
                mapOf(
                    "id" to (1..10000).toList(),
                    "value" to (1..10000).map { it * 2 },
                    "category" to (1..10000).map { 
                        if (it % 3 == 0) "A" 
                        else if (it % 3 == 1) "B" 
                        else "C" 
                    }
                )
            )
            onLog("创建数据: ${df.shape()} 行\n")
            
            // 2. 过滤数据
            val filtered = df.filter { row ->
                (row["value"] as Int) > 10000
            }
            onLog("过滤后: ${filtered.shape().first} 行\n")
            
            // 3. 分组聚合
            val grouped = filtered.groupBy("category").agg(
                mapOf(
                    "avg_value" to "mean",
                    "max_id" to "max",
                    "count" to "count"
                )
            )
            onLog("分组聚合:\n$grouped\n")
            
            // 4. 使用原生计算
            val values = filtered["value"].filterNotNull().map { 
                (it as Number).toDouble() 
            }.toDoubleArray()
            
            val sum = NativeMath.sumDoubleArray(values)
            val mean = NativeMath.meanDoubleArray(values)
            
            onLog("原生计算结果:\n")
            onLog("- 总和: $sum\n")
            onLog("- 平均值: $mean\n")
            
            // 5. 导出 CSV
            val csvFile = File(
                android.content.ContextWrapper(null).cacheDir,
                "processed_data.csv"
            )
            
            Andas.getInstance().exportCSVAsync(
                filtered,
                csvFile,
                onSuccess = {
                    onLog("CSV 导出成功: ${csvFile.absolutePath}\n")
                    onLog("\n=== 处理完成 ===\n")
                    onComplete()
                },
                onError = { e ->
                    onLog("导出失败: ${e.message}\n")
                    onComplete()
                }
            )
            
        } catch (e: Exception) {
            onLog("错误: ${e.message}\n")
            e.printStackTrace()
            onComplete()
        }
    }.start()
}
```

## 9. 性能优化技巧

### 9.1 数据量优化

```kotlin
// ✅ 推荐：大数据量使用异步
Andas.getInstance().readCSVAsync(file) { df ->
    // 处理数据
}

// ❌ 避免：在主线程处理大数据
val df = Andas.getInstance().readCSV(file) // 可能阻塞 UI
```

### 9.2 内存管理

```kotlin
// ✅ 及时释放资源
override fun onDestroy() {
    super.onDestroy()
    Andas.getInstance().destroy()
}

// ✅ 复用 DataFrame 对象
var df: DataFrame? = null
df = Andas.getInstance().createDataFrame(data)
// 使用 df
df = null // 帮助 GC
```

### 9.3 原生加速

```kotlin
// ✅ 大数据量使用原生方法
val data = (1..100000).map { it.toDouble() }.toDoubleArray()
val result = NativeMath.multiplyDoubleArray(data, 2.0)

// ✅ 统计计算使用原生
val sum = NativeMath.sumDoubleArray(data)
val mean = NativeMath.meanDoubleArray(data)
```

## 10. 调试和日志

### 10.1 配置日志级别

```kotlin
Andas.initialize(this) {
    debugMode = true
    logLevel = Andas.LogLevel.DEBUG  // 开发时使用
    // logLevel = Andas.LogLevel.INFO  // 生产环境
}
```

### 10.2 查看线程池状态

```kotlin
val stats = Andas.getInstance().getStats()
Log.i("Stats", """
    活跃线程: ${stats["active_threads"]}
    队列大小: ${stats["queue_size"]}
    完成任务: ${stats["completed_tasks"]}
""".trimIndent())
```

## 11. 常见问题解决

### 11.1 SDK 未初始化

**错误：** `InitializationException: Andas SDK not initialized`

**解决：**
```kotlin
// 确保在使用前调用
Andas.initialize(context) { /* 配置 */ }
```

### 11.2 原生库加载失败

**错误：** `NativeException: Cannot load native library`

**解决：**
1. 检查 NDK 是否安装
2. 清理并重新构建项目
3. 检查 ABI 配置

### 11.3 内存溢出

**错误：** `OutOfMemoryError`

**解决：**
```kotlin
// 1. 使用异步操作
// 2. 分批处理大数据
// 3. 及时调用 destroy()
```

## 12. 下一步

- 📖 阅读 [API 参考文档](API_REFERENCE.md) 了解详细 API
- 🔍 查看 [完整项目文档](README.md) 了解架构设计
- 🧪 运行 Demo 应用体验所有功能
- 🚀 开始构建你的数据处理应用！

---

**提示：** 如果遇到问题，请查看日志输出或提交 Issue 到项目仓库。
