# Andas 0.2.1：完整 MULLION Demo 与更多图类型

发布日期：2026-11-02

## Demo

- MULLION 成为默认启动入口，统一包含 **7 个学习章节、2 个论文实验、7 项数值能力**；
- 所有页面均可点击运行：章节示例、论文实验、NumPy/SciPy/绘图/回归/转换/扩展；
- `ChapterDetailScreen` 与 `PaperExperimentScreen` 在窄屏改为单栏，宽屏保持双栏；
- 演示图表扩展为：面积、阶梯、误差棒、横向柱、堆叠柱、箱线图、热力图。

## 绘图

- 新增 `area`、`step`、`errorBar`、`barh`、`stackedBar`、`box`、`heatmap`；
- 支持任意子图中的这些图类型与分类标签；
- 保留 Bitmap/PNG 与 SVG 双导出。

## 文档

- Demo 说明与截图更新为统一入口；
- 绘图文档补充新图类型示例；
- MULLION Demo APK 随本 Release 提供。

## 安装

```kotlin
implementation("com.github.trueWangSyutung:Andas:0.2.1")
```

Demo APK：见 [Release 0.2.1](https://github.com/trueWangSyutung/Andas/releases/tag/0.2.1)。
