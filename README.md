# Andas SDK

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8+-orange.svg)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-26+-green.svg)](https://developer.android.com/)

> **⚠️ 重要提示**: 本项目代码由 AI 辅助完成，目前处于测试阶段，欢迎测试和反馈！

[![](https://jitpack.io/v/trueWangSyutung/Andas.svg)](https://jitpack.io/#trueWangSyutung/Andas)

## 项目简介

Andas SDK (Android Data Analysis System) 是一个专为 Android 平台设计的高性能数据处理库，灵感来源于 Python 的 pandas 库。通过 JNI 技术集成 C++ 原生代码和 OpenMP 并行计算，实现了显著的性能提升。

## 📋 版本信息
**当前版本**: `0.0.1.rc6` (2025-12-19)  

### 核心特性

- 📊 **类 Pandas API**: 熟悉的 API 设计，降低学习成本
- 🔧 **混合架构**: Kotlin + C++ + OpenMP，智能降级
- 🧵 **异步支持**: 完整的异步操作和线程池管理
- 🌍 **中文文档**: 完整的中文技术文档

## 快速开始

### 1. 添加依赖

在项目级 `settings.gradle.kts` 中：
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

在 `app/build.gradle.kts` 中：

```kotlin
dependencies {
    implementation("com.github.trueWangSyutung:Andas:0.0.1.rc1")
}
```

### 2. 初始化 SDK

```kotlin
Andas.initialize(this) {
    debugMode = true
    logLevel = Andas.LogLevel.DEBUG
    timeoutSeconds = 60L
}
```

### 3. 创建 DataFrame

```kotlin
val df = Andas.getInstance().createDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie"),
        "age" to listOf(25, 30, 35),
        "salary" to listOf(50000, 60000, 70000)
    )
)

println(df.shape())  // (3, 3)
```


## 项目结构

```
Andas/
├── android-pandas/          # 核心库模块
│   ├── src/main/
│   │   ├── java/           # Kotlin API
│   │   │   ├── Andas.kt    # SDK 入口
│   │   │   ├── entity/     # 数据结构
│   │   │   ├── core/       # JNI 绑定
│   │   │   └── types/      # 类型定义
│   │   └── cpp/            # C++ 原生代码
│   │       ├── math_operations.cpp
│   │       └── data_processing.cpp
│   └── build.gradle.kts
├── app/                     # 演示应用
│   └── src/main/java/...   # 完整功能演示
└── 文档/
    ├── README.md           # 项目总览
    ├── API_REFERENCE.md    # API 文档
    ├── QUICK_START.md      # 快速入门
    ├── JNI_INTEGRATION.md  # JNI 集成
    ├── PERFORMANCE_BENCHMARK.md  # 性能测试
    ├── DEMO_APP_GUIDE.md   # Demo 使用指南
    └── PROJECT_SUMMARY.md  # 项目总结
```

## 核心功能

### Series (序列)
- ✅ 创建（列表、Map）
- ✅ 数据访问（索引、键）
- ✅ 数学运算（乘法、加法）
- ✅ 空值处理（dropna、fillna）
- ✅ 统计功能（unique、valueCounts）

### DataFrame (数据框)
- ✅ 创建（Map）
- ✅ 列操作（选择、添加、删除、重命名）
- ✅ 筛选排序（filter、sortValues）
- ✅ 分组聚合（groupBy、agg）
- ✅ 空值处理（dropna、fillna）
- ✅ 数据合并（merge）

### 异步操作
- ✅ CSV 导出（exportCSVAsync）
- ✅ CSV 读取（readCSVAsync）
- ✅ 线程池管理

### JNI 原生加速
- ✅ 数学运算（乘法、求和、均值）
- ✅ 排序索引（argsort）
- ✅ 归一化（normalize）
- ✅ 空值处理（findNullIndices、dropNullValues）
- ✅ 统计描述（describe）

## 文档导航

- 📖 **[快速入门](QUICK_START.md)** - 15 分钟上手
- 📚 **[API 参考](API_REFERENCE.md)** - 完整 API 文档
- 🔧 **[JNI 集成](JNI_INTEGRATION.md)** - 原生代码详解
- 📊 **[性能测试](PERFORMANCE_BENCHMARK.md)** - 基准测试报告
- 🧪 **[Demo 指南](DEMO_APP_GUIDE.md)** - 演示应用说明
- 📋 **[项目总结](PROJECT_SUMMARY.md)** - 技术架构总结

## 环境要求

- **Android Studio**: Arctic Fox 或更高版本
- **Android SDK**: API 21+ (Android 5.0+)
- **NDK**: 21+
- **Kotlin**: 1.8+
- **Gradle**: 7.0+

## 快速示例

### 完整数据处理流程

```kotlin
// 1. 创建数据
val df = Andas.getInstance().createDataFrame(
    mapOf(
        "product" to listOf("A", "B", "A", "C", "B"),
        "sales" to listOf(100, 200, 150, 80, 300),
        "region" to listOf("East", "West", "East", "North", "West")
    )
)

// 2. 数据筛选
val filtered = df.filter { row ->
    (row["sales"] as Int) > 100
}

// 3. 分组聚合
val result = filtered.groupBy("region").agg(
    mapOf(
        "total_sales" to "sum",
        "avg_sales" to "mean",
        "count" to "count"
    )
)

// 4. 异步导出
Andas.getInstance().exportCSVAsync(
    result,
    File(cacheDir, "report.csv"),
    onSuccess = { Log.i("Export", "成功") },
    onError = { e -> Log.e("Export", "失败", e) }
)
```

### 原生性能优化

```kotlin
// 使用原生数学运算（3-5 倍加速）
val data = (1..100000).map { it.toDouble() }.toDoubleArray()
val sum = NativeMath.sumDoubleArray(data)
val mean = NativeMath.meanDoubleArray(data)
val normalized = NativeMath.normalize(data)
```



## 技术亮点

### 1. 混合架构设计
- **Kotlin 层**: 友好 API，类型安全
- **JNI 层**: 高性能计算，无缝集成
- **OpenMP**: 多线程并行，自动优化



## 贡献指南

欢迎测试和反馈！由于本项目由 AI 辅助完成，可能存在以下需要改进的地方：

- 🧪 **测试覆盖**: 需要更多边界情况测试
- 📊 **性能优化**: 特定场景的性能调优
- 📖 **文档完善**: 更多使用示例和最佳实践
- 🐛 **Bug 修复**: 发现并修复潜在问题

### 反馈方式

1. 提交 Issue
2. 提供测试用例
3. 性能基准数据
4. 使用场景反馈



## 致谢

- 感谢 Python pandas 项目的灵感
- 感谢 OpenMP 社区的并行计算支持
- 感谢所有测试和反馈的用户

---

**注意**: 本项目代码由 AI 辅助生成，目前处于测试阶段。请在生产环境使用前充分测试。欢迎提交反馈和贡献！

**项目状态**: 🧪 测试中 | 🎯 功能完整 | 📈 持续优化
