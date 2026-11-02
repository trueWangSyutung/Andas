# Matplotlib 风格绘图

`cn.ac.oac.libs.andas.plot` 用 Android Canvas 渲染折线、散点、柱状、直方图、饼图与子图，
也可以导出与平台无关的 SVG 字符串（适合 JVM 测试、服务端或文档）。

## 折线/散点/柱状/直方图

```kotlin
import cn.ac.oac.libs.andas.plot.Figure
import cn.ac.oac.libs.andas.numpy.NDArray

val figure = Figure(width = 800, height = 600)
figure.title = "Andas 数据概览"

val axes = figure.axes()
axes.title = "销售额"
axes.xLabel = "月份"
axes.yLabel = "金额"
axes.line(doubleArrayOf(1.0, 2.0, 3.0, 4.0), doubleArrayOf(10.0, 18.0, 15.0, 24.0), "2026")
axes.scatter(doubleArrayOf(1.0, 2.0, 3.0, 4.0), doubleArrayOf(10.0, 18.0, 15.0, 24.0))

val bitmap = figure.renderBitmap()   // android.graphics.Bitmap
val svg = figure.renderSvg()         // 纯 Kotlin，可在 JVM 单测中使用
```

柱状图与直方图：

```kotlin
figure.axes().bar(listOf("A", "B", "C"), doubleArrayOf(12.0, 30.0, 18.0), "分类")
figure.axes().hist(NDArray.of(1.0, 2.0, 2.0, 3.0, 4.0, 5.0), bins = 5)
```

## 子图

`Figure(rows, cols)` 创建网格，通过 `axes(row, col)` 访问：

```kotlin
val grid = Figure(width = 1000, height = 700, rows = 2, cols = 2)
grid.axes(0, 0).line(x, y, "line")
grid.axes(0, 1).scatter(x, y, "scatter")
grid.axes(1, 0).bar(labels, values, "bar")
grid.axes(1, 1).hist(values, bins = 8, "hist")
```

## 饼图

```kotlin
figure.axes().pie(doubleArrayOf(45.0, 30.0, 25.0), listOf("A", "B", "C"))
```

## 坐标轴、样式与导出

```kotlin
val axes = figure.axes()
axes.xLim = 0.0 to 12.0
axes.yLim = 0.0 to 100.0
axes.logY = true                       // 对数纵轴（数据必须 > 0）
axes.line(x, y, "series", lineWidth = 3f)
axes.scatter(x, y, "points", markerSize = 5f)
axes.bar(listOf("A", "B", "C"), values, "category")

figure.savePng("/sdcard/Download/andas-plot.png")
```

- `xLim`/`yLim` 为数据空间范围；`logX`/`logY` 在渲染时转换为对数坐标。
- 柱状图会在 x 轴显示分类标签。
- `savePng` 仅在 Android 平台可用；`renderSvg()` 跨平台。

## 设计约束

- 默认使用语义色板（`Palette`），支持自定义 ARGB 颜色。
- 自动计算坐标范围并绘制 5 个刻度、网格、标题、轴标签和图例。
- `renderBitmap()` 依赖 Android；`renderSvg()` 不依赖 Android，可用于 CI 中的视觉回归。
- `Figure` 不执行布局测量，尺寸由构造参数决定；在 Compose 中用 `Image(bitmap.asImageBitmap())` 展示。
