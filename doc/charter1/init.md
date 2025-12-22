
## SDK的初始化

Andas SDK 采用单例模式设计， 通过 Andas.initialize() 方法进行初始化。
SDK 支持快速初始化和自定义配置两种方式。

#### 快速初始化（推荐）：
```kotlin
// 在 Application.onCreate() 或 Activity.onCreate() 中
Andas.initialize(this) {
    debugMode = true
    logLevel = Andas.LogLevel.DEBUG
    timeoutSeconds = 60L
}
```
#### 标准初始化

```kotlin
// 1. 获取实例
val instance = Andas.getInstance()

// 2. 配置参数
val config = Andas.AndasConfig().apply {
    debugMode = true
    logLevel = Andas.LogLevel.INFO
    timeoutSeconds = 30L
    cachePath = "andas_cache"
    memoryOptimization = true
    maxConcurrentTasks = 4
    errorHandler = { exception ->
        Log.e("Andas", "发生错误", exception)
    }
}

// 3. 执行初始化
instance.init(this, config)
```

#### 配置参数详解

`AndasConfig` 类提供了丰富的配置选项：

| 参数 | 类型 | 默认值 | 说明 | 
|------|------|--------|------| 
| `debugMode` | Boolean | `false` | 调试模式，开启后会输出详细日志和堆栈信息 | 
| `logLevel` | LogLevel | `INFO` | 日志级别：DEBUG、INFO、WARN、ERROR、NONE |
| `timeoutSeconds` | Long | `30L` | 异步操作超时时间（秒） |
| `cachePath` | String | `"andas_cache"` | 缓存目录名称，位于应用缓存目录下 | 
| `memoryOptimization` | Boolean | `true` | 内存优化，自动清理临时文件 | 
| `maxConcurrentTasks` | Int | `4` | 最大并发任务数，用于线程池管理 |
| `errorHandler` | ((Exception) -> Unit)? | `null` | 全局错误处理器 |

__日志级别说明：__

- `DEBUG`: 输出所有调试信息，包括详细的执行流程
- `INFO`: 输出关键信息，如初始化成功、操作完成等
- `WARN`: 输出警告信息，如重复初始化、性能建议等
- `ERROR`: 仅输出错误信息
- `NONE`: 不输出任何日志

### 初始化的最佳实践
__推荐做法：__

我们推荐只在应用启动的时候初始化一次。重复的初始化无效，且在异步任务中初始化可能导致竞态条件。

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 在应用启动时初始化
        Andas.initialize(this) {
            debugMode = BuildConfig.DEBUG
            logLevel = if (BuildConfig.DEBUG) Andas.LogLevel.DEBUG else Andas.LogLevel.INFO
            timeoutSeconds = 60L
            maxConcurrentTasks = Runtime.getRuntime().availableProcessors()
        }
    }
}
```

Andas 的初始化是线程安全的，采用了双重检查锁定（Double-Checked Locking）模式：

```kotlin
// 内部实现
fun getInstance(): Andas {
    if (instance == null) {
        synchronized(Andas::class) {
            if (instance == null) {
                instance = Andas()
            }
        }
    }
    return instance!!
}
```

这意味着您可以在多个线程中安全地调用 `getInstance()`，但建议仍然在主线程中进行初始化。

### 配置优化建议
__对于数据密集型应用：__

```kotlin
Andas.initialize(this) {
    debugMode = false
    logLevel = Andas.LogLevel.WARN
    timeoutSeconds = 120L  // 更长的超时时间
    memoryOptimization = true
    maxConcurrentTasks = Runtime.getRuntime().availableProcessors() * 2
}
```

__对于轻量级应用：__

```kotlin
Andas.initialize(this) {
    debugMode = false
    logLevel = Andas.LogLevel.ERROR
    timeoutSeconds = 30L
    memoryOptimization = true
    maxConcurrentTasks = 2  // 限制并发数以节省资源
}
```

__对于开发调试阶段：__

```kotlin
Andas.initialize(this) {
    debugMode = true
    logLevel = Andas.LogLevel.DEBUG
    timeoutSeconds = 60L
    memoryOptimization = false  // 关闭优化以便调试
    maxConcurrentTasks = 1  // 单线程执行，便于调试
    errorHandler = { exception ->
        // 在调试时抛出异常，便于定位问题
        throw exception
    }
}
```

### 错误处理和常见问题
__1. 重复初始化：__

```kotlin
// 第一次初始化
Andas.initialize(this) { }

// 第二次初始化（会忽略并输出警告）
Andas.initialize(this) { }
// 输出: [Andas-WARN] Andas SDK已初始化，重复初始化将被忽略
```

__2. 配置参数无效：__

```kotlin
Andas.initialize(this) {
    maxConcurrentTasks = 0  // ❌ 会抛出异常
}
// 抛出: IllegalArgumentException: 最大并发任务数必须大于0
```

__3. Context 为空：__

```kotlin
// ❌ 错误：不能使用 Activity Context 在 Application 中初始化
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 这里应该使用 this，而不是某个 Activity 的 Context
    }
}
```

### 检查初始化状态

```kotlin
// 检查是否已初始化
try {
    Andas.getInstance().checkInitialized()
    // SDK 已正确初始化
} catch (e: IllegalStateException) {
    // SDK 未初始化
    Log.e("Andas", "请先初始化 SDK", e)
}

// 获取 SDK 统计信息
val stats = Andas.getInstance().getStats()
println("SDK 状态: $stats")
// 输出: {initialized=true, debug_mode=true, cache_directory=..., thread_pool_stats=...}
```

### 资源清理

```kotlin
// 在应用退出时销毁 SDK（可选）
override fun onTerminate() {
    super.onTerminate()
    Andas.getInstance().destroy()
}
```


### 小结
本章详细介绍了 Andas SDK 的初始化流程和配置方法。关键要点：

1. __初始化时机__: 推荐在 Application.onCreate() 中完成
2. __配置策略__: 根据应用场景选择合适的配置参数
3. __错误处理__: 使用全局错误处理器统一处理异常
4. __性能优化__: 合理设置并发数和超时时间
5. __最佳实践__: 避免重复初始化，正确使用 Context

完成初始化后，您就可以开始使用 Andas SDK 的强大数据处理功能了。下一章将详细介绍 DataFrame 和 Series 的基本操作。








