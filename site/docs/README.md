# Andas SDK

> Android 上的高性能数据分析库：用熟悉的 pandas 风格 API，在设备本地完成 CSV 读取、清洗、聚合与导出。

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Android](https://img.shields.io/badge/Android-26%2B-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-orange.svg)](https://kotlinlang.org/)
[![Release](https://img.shields.io/badge/Release-0.2.0-blue.svg)](https://github.com/trueWangSyutung/Andas/releases/tag/0.2.0)

Andas（Android Data Analysis System）为 Android 提供列式存储与流式 CSV 读取能力，
在 Kotlin/Java 中提供 `Series`、`DataFrame`、筛选、排序、分组、连接、空值处理和异步 I/O。
0.2.0 起，CSV 读取不再全量物化文件和行对象，20 万行 × 15 列（约 36 MB）可在
192 MB 堆的 Android 环境完成读取。

## 为什么选择 Andas

| 能力 | 说明 |
| --- | --- |
| 大文件可读 | 流式解析 + 原始类型列存储，不再为每行创建 Map |
| 内存可控 | 默认索引 O(1) 内存，空值用位掩码表示 |
| 计算更快 | 统计单次遍历，排序 O(n log n)，连接 O(n + m) |
| 原生加速 | 可选 JNI/OpenMP 内核，JVM 环境自动回退 |
| API 友好 | 类 pandas 的 `Series` / `DataFrame` 接口 |

## 快速开始

```kotlin
Andas.initialize(context)

val df = Andas.getInstance().createDataFrame(
    mapOf(
        "name" to listOf("Alice", "Bob", "Charlie"),
        "age" to listOf(25, 30, 35),
        "salary" to listOf(50000, 60000, 70000)
    )
)

val seniors = df.filterGreaterThan("age", 28)
println(seniors.shape())
```

在 `settings.gradle.kts` 中添加 JitPack 仓库，然后依赖：

```kotlin
implementation("com.github.trueWangSyutung:Andas:0.2.0")
```

## 文档导航

- [快速开始](quickstart.md)
- [SDK 初始化](guide/init.md)
- [Series API](guide/series.md)
- [DataFrame API](guide/dataframe.md)
- [异步操作](guide/async.md)
- [JNI 原生 API](guide/jni.md)
- [性能优化](guide/performance.md)
- [完整 API 参考](api.md)

## 技术报告

- [基准测试与优化报告](benchmark/report.md)：JVM、Android 与 pandas 对比
- [实验复现指南](benchmark/reproduce.md)：数据生成、基线、脚本与命令
- [English Technical Report](benchmark/english-report.md)：英文技术报告（PDF + LaTeX）

## 项目状态

- 当前版本：`0.2.0`（2026-11-02）
- 最低 Android API：26
- 单元测试：32 个 JVM 测试通过；9 个 JNI 测试在 JVM 跳过，Android instrumentation 覆盖读取路径
- 许可证：Apache-2.0

## 链接

- [GitHub 仓库](https://github.com/trueWangSyutung/Andas)
- [Release 0.1.5](https://github.com/trueWangSyutung/Andas/releases/tag/0.2.0)
- [问题反馈](https://github.com/trueWangSyutung/Andas/issues)
