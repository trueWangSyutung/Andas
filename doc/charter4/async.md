## 第四章：异步操作 API

### 4.1 异步操作概述

Andas SDK 提供了完整的异步操作支持，通过协程和回调机制，确保在处理大量数据时不会阻塞主线程，提供流畅的用户体验。

**核心特性：**
- 🚀 非阻塞操作：不阻塞 UI 线程
- 📊 进度追踪：支持操作进度反馈
- 🔄 回调机制：成功和失败回调
- ⚡ 高性能：基于协程的异步处理
- 🛡️ 错误处理：完整的异常捕获和处理

### 4.2 CSV 文件异步操作

#### 4.2.1 异步读取 CSV

```kotlin
// 从 Assets 异步读取
Andas.getInstance().readCSVFromAssetsAsync(
    "large_dataset.csv",
    ",",
    onSuccess = { df ->
        // 在主线程中处理结果
        println("读取成功: ${df.shape()}")
        println("列名: ${df.columns()}")
        println("预览:\n${df.head(3)}")
    },
    onError = { error ->
        // 处理错误
        println("读取失败: ${error.message}")
        error.printStackTrace()
    }
)
```

#### 4.2.2 异步写入 CSV

```kotlin
val df = Andas.getInstance().createDataFrame(
    mapOf("id" to (1..1000).toList())
)

// 异步保存到私有存储
Andas.getInstance().saveToPrivateStorageAsync(
    "output.csv", df,
    onSuccess = {
        println("保存成功")
    },
    onError = { error ->
        println("保存失败: ${error.message}")
    }
)
```

### 4.3 通用异步执行

#### 4.3.1 计算任务异步化

```kotlin
// 执行耗时计算任务
Andas.getInstance().executeAsync(
    task = {
        // 模拟复杂计算
        val largeDf = Andas.getInstance().createDataFrame(
            mapOf("data" to (1..100000).map { it * 1.5 })
        )
        largeDf.sumNative("data")
    },
    onSuccess = { result ->
        println("计算结果: $result")
    },
    onError = { error ->
        println("计算失败: ${error.message}")
    }
)
```

#### 4.3.2 IO 任务异步化

```kotlin
// 执行文件读写任务
Andas.getInstance().executeIO(
    task = {
        // 读取大文件
        val file = File(context.filesDir, "large_data.csv")
        Andas.getInstance().readCSV(file)
    },
    onSuccess = { df ->
        println("文件读取成功: ${df.shape()}")
    },
    onError = { error ->
        println("文件读取失败: ${error.message}")
    }
)
```

### 4.4 异步操作最佳实践

#### 4.4.1 链式异步操作

```kotlin
// 1. 读取数据
Andas.getInstance().readCSVFromAssetsAsync(
    "input.csv",
    onSuccess = { df ->
        // 2. 处理数据
        val processed = df.addColumn("new_col") { row ->
            row.getInt("value") * 2
        }
        
        // 3. 保存结果
        Andas.getInstance().saveToPrivateStorageAsync(
            "output.csv", processed,
            onSuccess = {
                println("完整流程完成")
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
```

#### 4.4.2 并行异步操作

```kotlin
// 并行处理多个文件
val files = listOf("data1.csv", "data2.csv", "data3.csv")

files.forEach { fileName ->
    Andas.getInstance().readCSVFromAssetsAsync(
        fileName,
        onSuccess = { df ->
            println("$fileName 读取成功: ${df.shape()}")
            // 可以在这里进行独立处理
        },
        onError = { error ->
            println("$fileName 读取失败: ${error.message}")
        }
    )
}
```

### 4.5 错误处理和超时管理

#### 4.5.1 完整的错误处理

```kotlin
Andas.getInstance().readCSVFromAssetsAsync(
    "data.csv",
    onSuccess = { df ->
        try {
            // 数据处理逻辑
            val result = df.meanNative("value")
            println("平均值: $result")
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
```

#### 4.5.2 超时处理

```kotlin
// SDK 配置中设置超时时间
val config = Andas.AndasConfig().apply {
    timeoutSeconds = 60L  // 60秒超时
}

Andas.initialize(context, config)

// 异步操作会自动受超时控制
Andas.getInstance().readCSVFromAssetsAsync(
    "large_file.csv",
    onSuccess = { df ->
        println("读取成功")
    },
    onError = { error ->
        if (error is java.util.concurrent.TimeoutException) {
            println("操作超时")
        } else {
            println("其他错误: ${error.message}")
        }
    }
)
```

### 4.6 性能优化建议

#### 4.6.1 选择合适的异步方法

```kotlin
// CPU 密集型任务：使用 executeAsync
Andas.getInstance().executeAsync(
    task = {
        // 复杂计算
        val df = createLargeDataFrame()
        df.corr()  // 计算相关系数矩阵
    },
    onSuccess = { result ->
        println("计算完成")
    },
    onError = { error ->
        println("计算失败")
    }
)

// IO 密集型任务：使用 executeIO
Andas.getInstance().executeIO(
    task = {
        // 文件操作
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

#### 4.6.2 内存管理

```kotlin
// 处理大文件时，考虑分批处理
Andas.getInstance().readCSVFromAssetsAsync(
    "large_dataset.csv",
    onSuccess = { df ->
        // 分批处理避免内存溢出
        val batchSize = 1000
        val totalRows = df.shape().first
        
        for (i in 0 until totalRows step batchSize) {
            val end = minOf(i + batchSize, totalRows)
            val batch = df.slice(i, end)
            
            // 处理这一批数据
            processBatch(batch)
        }
        
        println("分批处理完成")
    },
    onError = { error ->
        println("读取失败: ${error.message}")
    }
)
```

### 4.7 与 UI 的集成

#### 4.7.1 在 Activity 中使用

```kotlin
class MainActivity : AppCompatActivity() {
    
    private fun loadData() {
        // 显示加载状态
        showLoading("正在加载数据...")
        
        Andas.getInstance().readCSVFromAssetsAsync(
            "data.csv",
            onSuccess = { df ->
                runOnUiThread {
                    hideLoading()
                    updateUI(df)
                }
            },
            onError = { error ->
                runOnUiThread {
                    hideLoading()
                    showError(error.message ?: "加载失败")
                }
            }
        )
    }
    
    private fun updateUI(df: DataFrame) {
        // 更新界面显示
        textView.text = "数据加载成功: ${df.shape()}"
        // 显示数据预览等
    }
}
```

#### 4.7.2 在 ViewModel 中使用

```kotlin
class DataViewModel : ViewModel() {
    
    private val _dataState = MutableStateFlow<DataState>(DataState.Idle)
    val dataState: StateFlow<DataState> = _dataState
    
    fun loadData() {
        viewModelScope.launch {
            _dataState.value = DataState.Loading
            
            Andas.getInstance().readCSVFromAssetsAsync(
                "data.csv",
                onSuccess = { df ->
                    _dataState.value = DataState.Success(df)
                },
                onError = { error ->
                    _dataState.value = DataState.Error(error.message ?: "未知错误")
                }
            )
        }
    }
}

sealed class DataState {
    object Idle : DataState()
    object Loading : DataState()
    data class Success(val df: DataFrame) : DataState()
    data class Error(val message: String) : DataState()
}
```

### 4.8 小结

本章详细介绍了 Andas SDK 的异步操作 API，包括：

1. **异步操作概述**：核心特性和优势
2. **CSV 文件异步操作**：读取和写入
3. **通用异步执行**：计算任务和 IO 任务
4. **最佳实践**：链式操作和并行操作
5. **错误处理**：完整的错误处理机制
6. **性能优化**：内存管理和方法选择
7. **UI 集成**：在 Activity 和 ViewModel 中使用

异步操作是现代应用开发的核心，合理使用异步 API 可以显著提升应用性能和用户体验。
