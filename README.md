# Andas SDK

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-orange.svg)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-26+-green.svg)](https://developer.android.com/)

> **项目状态**：`0.1.5` 已发布，核心 API 稳定；欢迎通过 Issue 反馈问题与性能数据。

[![](https://jitpack.io/v/trueWangSyutung/Andas.svg)](https://jitpack.io/#trueWangSyutung/Andas)

## 项目简介

Andas SDK (Android Data Analysis System) 是一个专为 Android 平台设计的高性能数据处理库，灵感来源于 Python 的 pandas 库。通过 JNI 技术集成 C++ 原生代码和 OpenMP 并行计算，实现了显著的性能提升。

## 📋 版本信息
**当前版本**: `0.1.5` (2026-11-01)  

### 核心特性

| 能力 | 说明 |
| --- | --- |
| 类 pandas API | `Series` / `DataFrame`、筛选、排序、分组、连接、空值处理 |
| 流式 CSV | 列式解析，不整文件物化；36 MB / 20 万行 × 15 列约 1.4 s（JVM） |
| 列式计算 | 共享范围索引、原始类型数组、单次遍历统计、哈希分组/连接 |
| 混合架构 | Kotlin + C++/JNI + OpenMP；JVM 环境自动回退到 Kotlin |
| 异步 I/O | CSV 读写、线程池与回调式异步 API |
| 文档完善 | 用户指南、API 参考、基准测试与技术报告 |

> 文档站点（GitHub Pages 默认域名）：<https://truewangsyutung.github.io/Andas/>

### 最新性能优化（0.1.5）

CSV 读取已从“整文件读入 → 每行 Map → 转置”重构为**流式列式解析**，
统计、排序、筛选、分组、连接等操作也改为单次遍历或哈希算法。
详见 [CHANGELOG_0.1.5.md](CHANGELOG_0.1.5.md)；
完整的实验数据、图表与复现脚本见 [BENCHMARK_REPORT.md](BENCHMARK_REPORT.md) 和 [bench/README.md](bench/README.md)。

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
    implementation("com.github.trueWangSyutung:Andas:0.1.5")
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
├── doc/                     # 用户指南（GitBook 源文档）
│   ├── charter1/init.md
│   ├── charter2/series.md
│   ├── charter3/dataframe.md
│   ├── charter4/async.md
│   ├── charter5/jni.md
│   └── charter6/performance.md
├── bench/                   # 性能实验脚本、原始数据与复现指南
├── report/                  # 英文技术报告（LaTeX / PDF）
├── site/                    # GitBook 站点配置与构建脚本
├── BENCHMARK_REPORT.md      # 中文基准测试与技术报告
├── API_REFERENCE.md         # API 参考
├── QUICK_START.md           # 快速入门
└── CHANGELOG_0.1.5.md       # 版本更新日志
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

- **[快速入门](QUICK_START.md)**：15 分钟完成依赖、初始化与第一个 DataFrame
- **[API 参考](API_REFERENCE.md)**：完整 API 与示例
- **[用户指南](doc/README.md)**：SDK 初始化、Series、DataFrame、异步、JNI、性能
- **[基准测试报告](BENCHMARK_REPORT.md)**：JVM、Android 与 pandas 对比
- **[实验复现](bench/README.md)**：数据生成、基线、脚本与命令
- **[English Technical Report](report/andas-technical-report.pdf)**：英文技术报告 PDF
- **[在线文档](https://truewangsyutung.github.io/Andas/)**：GitBook 站点

## 环境要求

- **Android Studio**: Ladybug 或更高版本
- **Android SDK**: API 26+（`minSdk = 26`）
- **Kotlin**: 2.0+
- **Gradle**: 8.13+（仓库使用 Gradle Wrapper）
- **NDK**: 21+（仅从源码构建原生模块时需要）

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

// 2. 数据筛选（行值为单元素 Series）
val filtered = df.filter { row ->
    val sales = row["sales"]?.get(0) as? Int
    sales != null && sales > 100
}

// 3. 分组聚合
val result = filtered.groupBy("region").sum()

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
// 原生数组内核（可用时；JVM 环境自动回退）
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

欢迎提交 Issue、测试用例、性能数据与使用场景反馈。当前重点关注：

- 更多边界情况与设备兼容性测试
- 原生列式内核与零拷贝缓冲区
- 文档示例与最佳实践补充
- Bug 修复与 API 稳定性

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

**说明**：请在引入生产环境前，结合自身数据规模与设备矩阵完成验证。版本发布记录见
[CHANGELOG_0.1.5.md](CHANGELOG_0.1.5.md)。

**项目状态**：0.1.5 已发布 · 功能完整 · 持续优化
