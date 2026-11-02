# 用户指南

本目录是 Andas SDK 的用户文档，覆盖初始化、`Series`、`DataFrame`、异步操作、JNI 原生接口
与性能优化。在线版本由 GitBook 构建，地址：<https://truewangsyutung.github.io/Andas/>。

## 推荐阅读顺序

| 章节 | 内容 | 适合读者 |
| --- | --- | --- |
| [SDK 初始化](charter1/init.md) | 初始化、配置、资源清理 | 首次接入 |
| [Series API](charter2/series.md) | 一维数据、索引、运算、空值处理 | 数据处理 |
| [DataFrame API](charter3/dataframe.md) | 列操作、筛选、排序、分组、连接 | 数据处理 |
| [异步操作](charter4/async.md) | 回调、线程池、CSV 异步读写 | Android 开发 |
| [JNI 原生 API](charter5/jni.md) | 原生数组内核与降级策略 | 性能优化 |
| [性能优化](charter6/performance.md) | 0.1.5 实测、内存与批量实践 | 性能调优 |

## 数值计算

- [NumPy 风格 NDArray](numpy/ndarray.md)
- [SciPy 科学计算](scipy/stats.md)
- [Matplotlib 风格绘图](plot/plotting.md)
- [注解与扩展](annotation/extensions.md)

## Demo

- [章节式 Demo 与 MULLION 功能展廊](demo/showcase.md)

## 相关材料

- [快速开始](../QUICK_START.md)
- [完整 API 参考](../API_REFERENCE.md)
- [基准测试与技术报告](../BENCHMARK_REPORT.md)
- [实验复现指南](../bench/README.md)
- [English Technical Report](../report/andas-technical-report.pdf)

## 版本

当前版本：`0.2.1`（2026-11-02）。历史变更见 [CHANGELOG_0.2.1.md](../changelog/CHANGELOG_0.2.1.md)。
