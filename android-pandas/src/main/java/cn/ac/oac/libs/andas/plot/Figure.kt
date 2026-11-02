package cn.ac.oac.libs.andas.plot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import cn.ac.oac.libs.andas.numpy.NDArray
import cn.ac.oac.libs.andas.scipy.Stats
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** Matplotlib 风格绘图：折线、散点、柱状、直方图、饼图与子图。 */
class Figure(
    val width: Int = 800,
    val height: Int = 600,
    val rows: Int = 1,
    val cols: Int = 1
) {
    var title: String = ""
    private val axesGrid = Array(rows) { r -> Array(cols) { c -> Axes(this, r, c) } }

    init {
        require(width > 100 && height > 100 && rows > 0 && cols > 0)
    }

    fun axes(row: Int = 0, col: Int = 0): Axes = axesGrid[row][col]

    fun plot(x: DoubleArray, y: DoubleArray, label: String? = null, color: Int = Palette.BLUE): Axes =
        axes().line(x, y, label, color)

    fun scatter(x: DoubleArray, y: DoubleArray, label: String? = null, color: Int = Palette.RED): Axes =
        axes().scatter(x, y, label, color)

    fun bar(labels: List<String>, values: DoubleArray, label: String? = null, color: Int = Palette.GREEN): Axes =
        axes().bar(labels, values, label, color)

    fun hist(data: NDArray, bins: Int = 10, label: String? = null, color: Int = Palette.ORANGE): Axes =
        axes().hist(data, bins, label, color)

    fun pie(values: DoubleArray, labels: List<String>): Axes = axes().pie(values, labels)

    fun renderBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        PlotRenderer.render(this, AndroidDrawTarget(canvas))
        return bitmap
    }

    /** 保存 PNG；返回是否成功。 */
    fun savePng(path: String): Boolean = try {
        java.io.FileOutputStream(path).use { output ->
            renderBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        true
    } catch (_: Throwable) {
        false
    }

    fun renderSvg(): String = PlotRenderer.render(this, SvgDrawTarget(width, height))
}

class Axes internal constructor(
    private val figure: Figure,
    internal val row: Int,
    internal val col: Int
) {
    var title: String = ""
    var xLabel: String = ""
    var yLabel: String = ""
    var xLim: Pair<Double, Double>? = null
    var yLim: Pair<Double, Double>? = null
    var logX: Boolean = false
    var logY: Boolean = false
    internal val series = mutableListOf<PlotSeries>()

    fun line(
        x: DoubleArray,
        y: DoubleArray,
        label: String? = null,
        color: Int = Palette.BLUE,
        lineWidth: Float = 2f
    ): Axes {
        require(x.size == y.size)
        series.add(PlotSeries(PlotKind.LINE, x, y, label, color, lineWidth = lineWidth))
        return this
    }

    fun line(
        x: NDArray,
        y: NDArray,
        label: String? = null,
        color: Int = Palette.BLUE,
        lineWidth: Float = 2f
    ): Axes = line(x.toDoubleArray(), y.toDoubleArray(), label, color, lineWidth)

    fun scatter(
        x: DoubleArray,
        y: DoubleArray,
        label: String? = null,
        color: Int = Palette.RED,
        markerSize: Float = 3.5f
    ): Axes {
        require(x.size == y.size)
        series.add(PlotSeries(PlotKind.SCATTER, x, y, label, color, markerSize = markerSize))
        return this
    }

    fun scatter(
        x: NDArray,
        y: NDArray,
        label: String? = null,
        color: Int = Palette.RED,
        markerSize: Float = 3.5f
    ): Axes = scatter(x.toDoubleArray(), y.toDoubleArray(), label, color, markerSize)

    fun bar(labels: List<String>, values: DoubleArray, label: String? = null, color: Int = Palette.GREEN): Axes {
        require(labels.size == values.size)
        series.add(PlotSeries(PlotKind.BAR, DoubleArray(values.size) { it.toDouble() }, values, label, color, labels))
        return this
    }

    fun bar(labels: List<String>, values: NDArray, label: String? = null, color: Int = Palette.GREEN): Axes =
        bar(labels, values.toDoubleArray(), label, color)

    fun hist(data: NDArray, bins: Int = 10, label: String? = null, color: Int = Palette.ORANGE): Axes {
        val histogram = Stats.histogram(data, bins)
        val centers = DoubleArray(bins) { (histogram.edges[it] + histogram.edges[it + 1]) / 2 }
        val counts = DoubleArray(bins) { histogram.counts[it].toDouble() }
        series.add(PlotSeries(PlotKind.HIST, centers, counts, label, color, emptyList(), histogram.edges))
        return this
    }

    fun pie(values: DoubleArray, labels: List<String>): Axes {
        require(values.isNotEmpty() && values.size == labels.size && values.sum() > 0)
        series.add(PlotSeries(PlotKind.PIE, DoubleArray(values.size) { it.toDouble() }, values, null, Palette.BLUE, labels))
        return this
    }
}

internal enum class PlotKind { LINE, SCATTER, BAR, HIST, PIE }

internal class PlotSeries(
    val kind: PlotKind,
    val x: DoubleArray,
    val y: DoubleArray,
    val label: String?,
    val color: Int,
    val labels: List<String> = emptyList(),
    val edges: DoubleArray? = null,
    val lineWidth: Float = 2f,
    val markerSize: Float = 3.5f
)

internal object Palette {
    const val BLUE = 0xFF2563EB.toInt()
    const val RED = 0xFFDC2626.toInt()
    const val GREEN = 0xFF16A34A.toInt()
    const val ORANGE = 0xFFEA580C.toInt()
    const val PURPLE = 0xFF7C3AED.toInt()
    const val GRAY = 0xFF6B7280.toInt()
    val SERIES = intArrayOf(BLUE, RED, GREEN, ORANGE, PURPLE, 0xFF0891B2.toInt(), 0xFFDB2777.toInt())
}

internal data class Bounds(val minX: Double, val maxX: Double, val minY: Double, val maxY: Double)

internal interface DrawTarget {
    fun background(color: Int)
    fun rect(left: Float, top: Float, right: Float, bottom: Float, color: Int, fill: Boolean)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, width: Float = 1f)
    fun circle(x: Float, y: Float, radius: Float, color: Int, fill: Boolean = true)
    fun polyline(points: FloatArray, color: Int, width: Float = 2f)
    fun text(x: Float, y: Float, text: String, color: Int, size: Float, align: Int = 0)
    fun pieSlice(cx: Float, cy: Float, radius: Float, startDegrees: Float, sweepDegrees: Float, color: Int)
}

internal object PlotRenderer {

    private const val MARGIN_LEFT = 62f
    private const val MARGIN_RIGHT = 22f
    private const val MARGIN_TOP = 44f
    private const val MARGIN_BOTTOM = 48f

    fun render(figure: Figure, target: DrawTarget): String {
        target.background(Color.WHITE)
        val cellWidth = figure.width.toFloat() / figure.cols
        val cellHeight = figure.height.toFloat() / figure.rows
        for (r in 0 until figure.rows) {
            for (c in 0 until figure.cols) {
                val axes = figure.axes(r, c)
                val left = c * cellWidth + MARGIN_LEFT
                val top = r * cellHeight + MARGIN_TOP
                val right = (c + 1) * cellWidth - MARGIN_RIGHT
                val bottom = (r + 1) * cellHeight - MARGIN_BOTTOM
                if (right <= left || bottom <= top) continue
                if (axes.title.isNotEmpty()) {
                    target.text((left + right) / 2, top - 14, axes.title, Color.BLACK, 14f, 1)
                }
                if (axes.series.any { it.kind == PlotKind.PIE }) {
                    drawPie(axes, target, left, top, right, bottom)
                    continue
                }
                val bounds = bounds(axes)
                drawGridAndAxes(target, axes, bounds, left, top, right, bottom)
                drawSeries(axes, bounds, target, left, top, right, bottom)
                drawLegend(axes, target, right, top)
            }
        }
        if (figure.title.isNotEmpty()) {
            target.text(figure.width / 2f, 26f, figure.title, Color.BLACK, 18f, 1)
        }
        return (target as? SvgDrawTarget)?.finish() ?: ""
    }

    private fun bounds(axes: Axes): Bounds {
        var minX = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        fun tx(value: Double): Double = if (axes.logX) kotlin.math.ln(value) else value
        fun ty(value: Double): Double = if (axes.logY) kotlin.math.ln(value) else value
        for (series in axes.series) {
            if (series.kind == PlotKind.HIST && series.edges != null) {
                minX = min(minX, tx(series.edges.first()))
                maxX = max(maxX, tx(series.edges.last()))
                minY = min(minY, ty(0.0))
                maxY = max(maxY, ty(series.y.max()))
            } else if (series.kind == PlotKind.BAR) {
                minX = min(minX, tx(-0.5))
                maxX = max(maxX, tx(series.x.size - 0.5))
                minY = min(minY, ty(0.0))
                maxY = max(maxY, ty(series.y.max()))
            } else {
                for (i in series.x.indices) {
                    if (!axes.logX || series.x[i] > 0) {
                        minX = min(minX, tx(series.x[i]))
                        maxX = max(maxX, tx(series.x[i]))
                    }
                    if (!axes.logY || series.y[i] > 0) {
                        minY = min(minY, ty(series.y[i]))
                        maxY = max(maxY, ty(series.y[i]))
                    }
                }
            }
        }
        if (!minX.isFinite()) { minX = 0.0; maxX = 1.0 }
        if (!minY.isFinite()) { minY = 0.0; maxY = 1.0 }
        if (minX == maxX) { minX -= 1; maxX += 1 }
        if (minY == maxY) { minY -= 1; maxY += 1 }
        val xPad = (maxX - minX) * 0.05
        val yPad = (maxY - minY) * 0.08
        val explicitX = axes.xLim?.let { tx(it.first) to tx(it.second) }
        val explicitY = axes.yLim?.let { ty(it.first) to ty(it.second) }
        return Bounds(
            explicitX?.first ?: (minX - xPad),
            explicitX?.second ?: (maxX + xPad),
            explicitY?.first ?: (minY - yPad),
            explicitY?.second ?: (maxY + yPad)
        )
    }

    private fun drawGridAndAxes(
        target: DrawTarget, axes: Axes, bounds: Bounds,
        left: Float, top: Float, right: Float, bottom: Float
    ) {
        val gridColor = 0xFFE5E7EB.toInt()
        val axisColor = 0xFF374151.toInt()
        val ticks = 5
        for (i in 0..ticks) {
            val ratio = i.toFloat() / ticks
            val x = left + ratio * (right - left)
            val y = bottom - ratio * (bottom - top)
            target.line(x, top, x, bottom, gridColor, 1f)
            target.line(left, y, right, y, gridColor, 1f)
            val xValue = bounds.minX + ratio * (bounds.maxX - bounds.minX)
            val yValue = bounds.minY + ratio * (bounds.maxY - bounds.minY)
            val xLabelValue = if (axes.logX) kotlin.math.exp(xValue) else xValue
            val yLabelValue = if (axes.logY) kotlin.math.exp(yValue) else yValue
            target.text(x, bottom + 16, format(xLabelValue), axisColor, 9f, 1)
            target.text(left - 6, y + 3, format(yLabelValue), axisColor, 9f, 2)
        }
        target.line(left, bottom, right, bottom, axisColor, 1.2f)
        target.line(left, top, left, bottom, axisColor, 1.2f)
        for (series in axes.series) {
            if (series.kind == PlotKind.BAR && series.labels.isNotEmpty()) {
                for (i in series.labels.indices) {
                    val x = left + ((series.x[i] - bounds.minX) / (bounds.maxX - bounds.minX) * (right - left)).toFloat()
                    target.text(x, bottom + 29, series.labels[i], axisColor, 8f, 1)
                }
            }
        }
        if (axes.xLabel.isNotEmpty()) target.text((left + right) / 2, bottom + 38, axes.xLabel, axisColor, 11f, 1)
        if (axes.yLabel.isNotEmpty()) target.text(left - 46, (top + bottom) / 2, axes.yLabel, axisColor, 11f, 1)
    }

    private fun drawSeries(axes: Axes, bounds: Bounds, target: DrawTarget, left: Float, top: Float, right: Float, bottom: Float) {
        fun tx(value: Double): Double = if (axes.logX) kotlin.math.ln(value) else value
        fun ty(value: Double): Double = if (axes.logY) kotlin.math.ln(value) else value
        fun px(value: Double) = left + ((tx(value) - bounds.minX) / (bounds.maxX - bounds.minX) * (right - left)).toFloat()
        fun py(value: Double) = bottom - ((ty(value) - bounds.minY) / (bounds.maxY - bounds.minY) * (bottom - top)).toFloat()
        for (series in axes.series) {
            when (series.kind) {
                PlotKind.LINE -> {
                    val points = FloatArray(series.x.size * 2)
                    for (i in series.x.indices) {
                        points[2 * i] = px(series.x[i]); points[2 * i + 1] = py(series.y[i])
                    }
                    target.polyline(points, series.color, series.lineWidth)
                }
                PlotKind.SCATTER -> {
                    for (i in series.x.indices) target.circle(px(series.x[i]), py(series.y[i]), series.markerSize, series.color)
                }
                PlotKind.BAR -> {
                    val step = (right - left) / series.x.size
                    val barWidth = step * 0.7f
                    for (i in series.x.indices) {
                        val x = px(series.x[i])
                        target.rect(x - barWidth / 2, py(series.y[i]), x + barWidth / 2, py(0.0), series.color, true)
                    }
                }
                PlotKind.HIST -> {
                    val edges = series.edges ?: continue
                    for (i in 0 until series.y.size) {
                        target.rect(px(edges[i]), py(series.y[i]), px(edges[i + 1]), py(0.0), series.color, true)
                    }
                }
                PlotKind.PIE -> Unit
            }
        }
    }

    private fun drawLegend(axes: Axes, target: DrawTarget, right: Float, top: Float) {
        val labeled = axes.series.filter { !it.label.isNullOrEmpty() }
        if (labeled.isEmpty()) return
        var y = top + 12
        for ((index, series) in labeled.withIndex()) {
            target.line(right - 90, y - 4, right - 74, y - 4, series.color, 3f)
            target.text(right - 70, y, series.label ?: "", Color.DKGRAY, 9f, 0)
            y += 14
        }
    }

    private fun drawPie(axes: Axes, target: DrawTarget, left: Float, top: Float, right: Float, bottom: Float) {
        val series = axes.series.first { it.kind == PlotKind.PIE }
        val total = series.y.sum()
        val radius = min(right - left, bottom - top) * 0.36f
        val cx = (left + right) / 2
        val cy = (top + bottom) / 2
        var start = -90f
        for (i in series.y.indices) {
            val sweep = (series.y[i] / total * 360.0).toFloat()
            target.pieSlice(cx, cy, radius, start, sweep, Palette.SERIES[i % Palette.SERIES.size])
            start += sweep
        }
        var y = top + 14
        for (i in series.labels.indices) {
            target.rect(right - 110, y - 8, right - 100, y + 2, Palette.SERIES[i % Palette.SERIES.size], true)
            target.text(right - 96, y, series.labels[i], Color.DKGRAY, 9f, 0)
            y += 15
        }
    }

    private fun format(value: Double): String {
        val absolute = kotlin.math.abs(value)
        return when {
            absolute >= 1e4 || (absolute in 1e-9..1e-3) -> String.format(Locale.US, "%.2e", value)
            else -> String.format(Locale.US, "%.2f", value)
        }
    }
}

private class AndroidDrawTarget(private val canvas: Canvas) : DrawTarget {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun background(color: Int) = canvas.drawColor(color)

    override fun rect(left: Float, top: Float, right: Float, bottom: Float, color: Int, fill: Boolean) {
        paint.color = color
        paint.style = if (fill) Paint.Style.FILL else Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        canvas.drawRect(left, top, right, bottom, paint)
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, width: Float) {
        paint.color = color; paint.style = Paint.Style.STROKE; paint.strokeWidth = width
        canvas.drawLine(x1, y1, x2, y2, paint)
    }

    override fun circle(x: Float, y: Float, radius: Float, color: Int, fill: Boolean) {
        paint.color = color
        paint.style = if (fill) Paint.Style.FILL else Paint.Style.STROKE
        canvas.drawCircle(x, y, radius, paint)
    }

    override fun polyline(points: FloatArray, color: Int, width: Float) {
        if (points.size < 4) return
        paint.color = color; paint.style = Paint.Style.STROKE; paint.strokeWidth = width
        val path = Path()
        path.moveTo(points[0], points[1])
        for (i in 2 until points.size step 2) path.lineTo(points[i], points[i + 1])
        canvas.drawPath(path, paint)
    }

    override fun text(x: Float, y: Float, text: String, color: Int, size: Float, align: Int) {
        paint.color = color; paint.style = Paint.Style.FILL; paint.textSize = size
        paint.textAlign = when (align) {
            1 -> Paint.Align.CENTER
            2 -> Paint.Align.RIGHT
            else -> Paint.Align.LEFT
        }
        canvas.drawText(text, x, y, paint)
    }

    override fun pieSlice(cx: Float, cy: Float, radius: Float, startDegrees: Float, sweepDegrees: Float, color: Int) {
        paint.color = color; paint.style = Paint.Style.FILL
        canvas.drawArc(RectF(cx - radius, cy - radius, cx + radius, cy + radius), startDegrees, sweepDegrees, true, paint)
    }
}

private class SvgDrawTarget(private val width: Int, private val height: Int) : DrawTarget {
    private val builder = StringBuilder()
    private var background = "#ffffff"

    override fun background(color: Int) {
        background = colorHex(color)
    }

    override fun rect(left: Float, top: Float, right: Float, bottom: Float, color: Int, fill: Boolean) {
        builder.append("<rect x=\"${f(left)}\" y=\"${f(top)}\" width=\"${f(right - left)}\" height=\"${f(bottom - top)}\" ")
        if (fill) builder.append("fill=\"${colorHex(color)}\" stroke=\"none\"") else builder.append("fill=\"none\" stroke=\"${colorHex(color)}\" stroke-width=\"1\"")
        builder.append("/>\n")
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, width: Float) {
        builder.append("<line x1=\"${f(x1)}\" y1=\"${f(y1)}\" x2=\"${f(x2)}\" y2=\"${f(y2)}\" stroke=\"${colorHex(color)}\" stroke-width=\"${f(width)}\"/>\n")
    }

    override fun circle(x: Float, y: Float, radius: Float, color: Int, fill: Boolean) {
        builder.append("<circle cx=\"${f(x)}\" cy=\"${f(y)}\" r=\"${f(radius)}\" fill=\"${if (fill) colorHex(color) else "none"}\" stroke=\"${colorHex(color)}\" stroke-width=\"1\"/>\n")
    }

    override fun polyline(points: FloatArray, color: Int, width: Float) {
        if (points.size < 4) return
        val pairs = (0 until points.size step 2).joinToString(" ") { "${f(points[it])},${f(points[it + 1])}" }
        builder.append("<polyline points=\"$pairs\" fill=\"none\" stroke=\"${colorHex(color)}\" stroke-width=\"${f(width)}\"/>\n")
    }

    override fun text(x: Float, y: Float, text: String, color: Int, size: Float, align: Int) {
        val anchor = when (align) { 1 -> "middle"; 2 -> "end"; else -> "start" }
        builder.append("<text x=\"${f(x)}\" y=\"${f(y)}\" fill=\"${colorHex(color)}\" font-family=\"sans-serif\" font-size=\"${f(size)}\" text-anchor=\"$anchor\">${escape(text)}</text>\n")
    }

    override fun pieSlice(cx: Float, cy: Float, radius: Float, startDegrees: Float, sweepDegrees: Float, color: Int) {
        val start = Math.toRadians(startDegrees.toDouble())
        val end = Math.toRadians((startDegrees + sweepDegrees).toDouble())
        val x1 = cx + radius * Math.cos(start); val y1 = cy + radius * Math.sin(start)
        val x2 = cx + radius * Math.cos(end); val y2 = cy + radius * Math.sin(end)
        val large = if (sweepDegrees > 180) 1 else 0
        builder.append("<path d=\"M ${f(cx)} ${f(cy)} L ${f(x1.toFloat())} ${f(y1.toFloat())} A ${f(radius)} ${f(radius)} 0 $large 1 ${f(x2.toFloat())} ${f(y2.toFloat())} Z\" fill=\"${colorHex(color)}\"/>\n")
    }

    fun finish(): String = buildString {
        append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$width\" height=\"$height\" viewBox=\"0 0 $width $height\">\n")
        append("<rect width=\"100%\" height=\"100%\" fill=\"$background\"/>\n")
        append(builder)
        append("</svg>\n")
    }

    private fun f(value: Float): String = String.format(Locale.US, "%.2f", value)
    private fun colorHex(color: Int): String = String.format("#%06X", color and 0xFFFFFF)
    private fun escape(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
