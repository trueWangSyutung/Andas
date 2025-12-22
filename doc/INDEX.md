
# Andas SDK API 参考文档

## 背景

众所周知，python具有一个数据操作的库pandas，受该库的启发，我设计实现了一个基于安卓的类pandas库，
旨在进一步完善Android生态。

Andas SDK (Android Data Analysis System) 是一个专为 Android 平台设计的高性能数据处理库，
灵感来源于 Python 的 pandas 库。通过 JNI 技术集成 C++ 原生代码和 OpenMP 并行计算，
实现了显著的性能提升。

## 目录

1. [SDK 初始化](./charter1/init.md)
2. [Series API](./charter2/series.md)
3. [DataFrame API](./charter3/dataframe.md)
4. [异步操作 API](./charter4/async.md)
5. [JNI 原生 API](./charter5/jni.md)
6. [性能优化和最佳实践](./charter6/performance.md)
