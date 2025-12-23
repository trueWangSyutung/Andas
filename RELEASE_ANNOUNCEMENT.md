# Andas SDK 0.1.0 版本发布

## 项目概述

我们很高兴发布 Andas SDK 的第一个版本 (0.0.1)。这是一个专为 Android 平台设计的数据处理库，灵感来源于 Python 的 pandas 库。通过 JNI 技术集成 C++ 原生代码，我们希望为 Android 开发者提供高效的数据处理能力。

## 什么是 Andas SDK？

Andas SDK (Android Data Analysis System) 是一个面向 Android 开发者的数据处理库，提供类似 pandas 的 API 接口。

**主要特点：**
- **性能优化**：通过 JNI 集成 C++ 原生代码
- **数据结构**：支持 Series 和 DataFrame
- **异步支持**：完整的异步操作 API
- **简洁 API**：易于使用的接口设计
- **统计功能**：内置基础统计和数学函数

## 核心功能示例

### 1. 快速开始

```kotlin
// SDK 初始化
Andas.initialize(context) {
    debugMode = true
    maxConcurrentTasks = 4
}

// 创建 Series
val series = Andas.getInstance().createSeries(
    listOf(1, 2, 3, 4, 5)
)

// 基础统计
val sum = series.sum()      // 15
val mean = series.mean()    // 3.0
val max = series.max()      // 5
```

### 2. DataFrame 操作

```kotlin
// 创建 DataFrame
val df = Andas.getInstance().createDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie"),
        "age" to listOf(25, 30, 35),
        "salary" to listOf(50000, 60000, 70000)
    )
)

// 数据查看
println(df.head())      // 前几行
println(df.shape())     // 形状: (3, 3)
println(df.columns())   // 列名: [name, age, salary]

// 数据筛选
val filtered = df.filter { row ->
    row.getInt("age") > 28
}

// 新增列
val dfWithBonus = df.addColumn("bonus") { row ->
    row.getInt("salary") * 0.1
}
```

### 3. 原生计算

```kotlin
// 性能对比测试
val data = (1..100000).map { it.toDouble() }

// Kotlin 实现
val kotlinSum = data.sum()

// JNI 原生实现
val jniSum = NativeMath.sumDoubleArray(data.toDoubleArray())
```

### 4. 异步操作

```kotlin
// 异步读取 CSV
Andas.getInstance().readCSVFromAssetsAsync(
    "large_dataset.csv",
    onSuccess = { df ->
        println("读取成功: ${df.shape()}")
        // 处理数据
    },
    onError = { error ->
        println("读取失败: ${error.message}")
    }
)
```



## 技术架构

### 系统架构

```
┌─────────────────────────────────────┐
│         Kotlin 应用层                │
│  (Series, DataFrame, API 封装)      │
├─────────────────────────────────────┤
│         JNI 接口层                   │
│  (数据转换，方法调用)                │
├─────────────────────────────────────┤
│         C++ 原生层                   │
│  (OpenMP 并行计算，数学运算)         │
└─────────────────────────────────────┘
```

### 核心技术栈

- **JNI**：Java 与 C++ 通信接口
- **OpenMP**：多线程并行计算框架
- **协程**：异步操作支持
- **内存管理**：优化的数据传输机制

## 应用场景

### 数据分析
```kotlin
// 用户行为分析
val userData = readCSV("user_actions.csv")
val stats = mapOf(
    "avg_session" to userData.meanNative("session_duration"),
    "total_users" to userData.count(),
    "conversion_rate" to userData.filter { 
        it.getString("action") == "purchase" 
    }.count() / userData.count().toDouble()
)
```

### 金融计算
```kotlin
// 股票数据分析
val stockData = Andas.getInstance().createDataFrame(
    mapOf(
        "date" to dates,
        "price" to prices,
        "volume" to volumes
    )
)
val ma5 = stockData["price"]?.rolling(5)?.mean()
```

### 科学计算
```kotlin
// 实验数据处理
val experimentData = Andas.getInstance().createDataFrame(
    mapOf(
        "sample_id" to (1..1000).toList(),
        "measurement" to measurements
    )
)
val correlation = experimentData.corr()
```

## 开发者体验

### 链式操作
```kotlin
val result = df
    .filter { it.getInt("age") > 25 }
    .addColumn("category") { row ->
        when (row.getInt("age")) {
            in 20..29 -> "青年"
            in 30..39 -> "中年"
            else -> "其他"
        }
    }
    .groupBy("category")
    .agg(mapOf("salary" to "mean"))
```

### 错误处理
```kotlin
Andas.getInstance().readCSVFromAssetsAsync(
    "data.csv",
    onSuccess = { df ->
        // 成功处理
    },
    onError = { error ->
        when (error) {
            is FileNotFoundException -> {
                // 文件不存在处理
            }
            is SecurityException -> {
                // 权限不足处理
            }
            else -> {
                // 其他错误
            }
        }
    }
)
```

### 调试支持
```kotlin
val config = Andas.AndasConfig().apply {
    debugMode = true
    logLevel = Andas.LogLevel.DEBUG
}

Andas.initialize(context, config)

// 查看 SDK 状态
val stats = Andas.getInstance().getStats()
println("SDK 统计: $stats")
```

## 版本功能

### 当前版本 (0.0.1) 包含
- ✅ Series 和 DataFrame 核心数据结构
- ✅ 原生数学运算（求和、平均值、方差、标准差等）
- ✅ 异步操作 API
- ✅ CSV 文件读写
- ✅ 基础统计函数
- ✅ 空值处理
- ✅ 数据筛选和变换

### 已知限制
- ⚠️ 部分高级统计功能尚未实现
- ⚠️ 内存使用还有优化空间
- ⚠️ 文档和示例需要进一步完善
- ⚠️ 健壮性有待测试，欢迎维护

## 文档资源

### 章节化文档
```
doc/
├── INDEX.md                    # 项目总览
├── charter1/                   # 第一章：初始化
│   ├── init.md
│   └── series.md
├── charter2/                   # 第二章：Series
│   └── series.md
├── charter3/                   # 第三章：DataFrame
│   └── dataframe.md
├── charter4/                   # 第四章：异步操作
│   └── async.md
├── charter5/                   # 第五章：JNI 原生
│   └── jni.md
└── charter6/                   # 第六章：性能优化
    └── performance.md
```

### Demo 应用
- 6 个章节
- 20+ 实际例子
- 可交互界面
- 实时性能展示

## 参与贡献

我们欢迎社区贡献：

### 贡献方式
- 报告 Bug 和问题
- 提出功能建议
- 改进文档
- 提交代码改进

### 反馈渠道
- GitHub Issues
- 技术讨论
- 文档反馈

## 未来计划

### 短期目标
- 更多统计函数（偏度、峰度等）
- 性能优化
- 文档完善

### 长期规划
- 数据可视化支持
- 机器学习算法
- 更多文件格式
- 数据库集成

## 安装使用

### 添加依赖

在项目级 settings.gradle.kts 中：
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

在 app/build.gradle.kts 中：
```kotlin
dependencies {
    implementation("com.github.trueWangSyutung:Andas:0.0.1")
}
```

### 快速开始
```kotlin
// 初始化
Andas.initialize(context)

// 创建数据
val series = Andas.getInstance().createSeries(listOf(1, 2, 3, 4, 5))

// 使用统计功能
val sum = series.sum()
val mean = series.mean()
```

## 项目信息

- **版本**: 0.0.1
- **发布时间**: 2025年12月22日
- **GitHub**: [github.com/trueWangSyutung/Andas](https://github.com/trueWangSyutung/Andas)
- **文档**: 项目 doc/ 目录

## 致谢

感谢所有参与测试和提供反馈的开发者，以及开源社区的支持。

---

**Andas SDK 开发团队**
*2025年12月22日*
