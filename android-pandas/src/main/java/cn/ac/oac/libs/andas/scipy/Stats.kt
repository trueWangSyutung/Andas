package cn.ac.oac.libs.andas.scipy

import cn.ac.oac.libs.andas.numpy.NDArray
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

data class TTestResult(val t: Double, val p: Double, val df: Int)
data class WelchTTestResult(val t: Double, val p: Double, val df: Double)
data class LinearRegressionResult(
    val slope: Double,
    val intercept: Double,
    val r: Double,
    val p: Double,
    val stdErr: Double
)
data class HistogramResult(val counts: IntArray, val edges: DoubleArray)

/** 统计检验与描述统计。 */
object Stats {

    fun describe(values: NDArray): Map<String, Double> {
        val n = values.size
        val mean = values.mean()
        val variance = values.variance(1)
        val sorted = values.toDoubleArray().also { it.sort() }
        return linkedMapOf(
            "count" to n.toDouble(),
            "mean" to mean,
            "std" to sqrt(variance),
            "min" to sorted.first(),
            "25%" to quantile(values, 0.25),
            "50%" to quantile(values, 0.50),
            "75%" to quantile(values, 0.75),
            "max" to sorted.last()
        )
    }

    fun quantile(values: NDArray, q: Double): Double {
        require(q in 0.0..1.0) { "q 必须在 [0,1]" }
        val sorted = values.toDoubleArray().also { it.sort() }
        if (sorted.isEmpty()) return Double.NaN
        val position = q * (sorted.size - 1)
        val lower = position.toInt()
        val upper = kotlin.math.min(lower + 1, sorted.size - 1)
        val fraction = position - lower
        return sorted[lower] * (1 - fraction) + sorted[upper] * fraction
    }

    fun median(values: NDArray): Double = quantile(values, 0.5)

    fun normalPdf(x: Double, mean: Double = 0.0, std: Double = 1.0): Double {
        val z = (x - mean) / std
        return kotlin.math.exp(-0.5 * z * z) / (std * sqrt(2 * Math.PI))
    }

    fun normalCdf(x: Double, mean: Double = 0.0, std: Double = 1.0): Double =
        0.5 * (1.0 + Special.erf((x - mean) / (std * sqrt(2.0))))

    fun pearson(x: NDArray, y: NDArray): Double {
        require(x.ndim == 1 && y.ndim == 1 && x.size == y.size && x.size > 1)
        val n = x.size
        var meanX = 0.0
        var meanY = 0.0
        var m2X = 0.0
        var m2Y = 0.0
        var cov = 0.0
        for (i in 0 until n) {
            val vx = x.get(i)
            val vy = y.get(i)
            val dx = vx - meanX
            val dy = vy - meanY
            meanX += dx / (i + 1)
            meanY += dy / (i + 1)
            m2X += dx * (vx - meanX)
            m2Y += dy * (vy - meanY)
            cov += dx * (vy - meanY)
        }
        val denominator = sqrt(m2X * m2Y)
        return if (denominator == 0.0) Double.NaN else cov / denominator
    }

    /** 标准正态分位数（Acklam 近似）。 */
    fun normalInv(p: Double, mean: Double = 0.0, std: Double = 1.0): Double {
        require(p > 0.0 && p < 1.0) { "p 必须在 (0,1)" }
        val a = doubleArrayOf(
            -3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02,
            1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00
        )
        val b = doubleArrayOf(
            -5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02,
            6.680131188771972e+01, -1.328068155288572e+01
        )
        val c = doubleArrayOf(
            -7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00,
            -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00
        )
        val d = doubleArrayOf(
            7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00,
            3.754408661907416e+00
        )
        val low = 0.02425
        val high = 1 - low
        val z = when {
            p < low -> {
                val q = sqrt(-2 * ln(p))
                (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                    ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1)
            }
            p <= high -> {
                val q = p - 0.5
                val r = q * q
                (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q /
                    (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1)
            }
            else -> {
                val q = sqrt(-2 * ln(1 - p))
                -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                    ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1)
            }
        }
        return mean + std * z
    }

    fun uniformCdf(x: Double, low: Double = 0.0, high: Double = 1.0): Double {
        require(high > low)
        return ((x - low) / (high - low)).coerceIn(0.0, 1.0)
    }

    fun corrcoef(x: NDArray, y: NDArray): NDArray {
        val r = pearson(x, y)
        return NDArray.from2D(arrayOf(doubleArrayOf(1.0, r), doubleArrayOf(r, 1.0)))
    }

    fun zscore(values: NDArray): NDArray {
        val mean = values.mean()
        val sd = values.std(1)
        return values.map { (it - mean) / sd }
    }

    fun mode(values: NDArray): Double? {
        val counts = HashMap<Double, Int>()
        for (value in values.toDoubleArray()) {
            if (!value.isNaN()) counts[value] = (counts[value] ?: 0) + 1
        }
        return counts.maxByOrNull { it.value }?.key
    }

    /** Welch 双侧独立样本 t 检验。 */
    fun ttestInd(first: NDArray, second: NDArray): WelchTTestResult {
        require(first.size > 1 && second.size > 1)
        val meanDifference = first.mean() - second.mean()
        val variance = first.variance(1) / first.size + second.variance(1) / second.size
        if (variance <= 0.0) return WelchTTestResult(Double.NaN, Double.NaN, Double.NaN)
        val t = meanDifference / sqrt(variance)
        val numerator = variance * variance
        val denominator = (first.variance(1) / first.size).let { it * it / (first.size - 1) } +
            (second.variance(1) / second.size).let { it * it / (second.size - 1) }
        val df = numerator / denominator
        val p = regularizedIncompleteBeta(df / (df + t * t), df / 2.0, 0.5)
        return WelchTTestResult(t, p.coerceIn(0.0, 1.0), df)
    }

    /** 双侧单样本 t 检验。 */
    fun ttestOneSample(values: NDArray, popMean: Double = 0.0): TTestResult {
        require(values.size > 1)
        val n = values.size
        val mean = values.mean()
        val se = sqrt(values.variance(1) / n)
        if (se == 0.0) return TTestResult(Double.NaN, Double.NaN, n - 1)
        val t = (mean - popMean) / se
        val df = n - 1.0
        val p = regularizedIncompleteBeta(df / (df + t * t), df / 2.0, 0.5)
        return TTestResult(t, p.coerceIn(0.0, 1.0), n - 1)
    }

    fun linregress(x: NDArray, y: NDArray): LinearRegressionResult {
        require(x.ndim == 1 && y.ndim == 1 && x.size == y.size && x.size > 2)
        val n = x.size
        val meanX = x.mean()
        val meanY = y.mean()
        var sxx = 0.0
        var sxy = 0.0
        var syy = 0.0
        for (i in 0 until n) {
            val dx = x.get(i) - meanX
            val dy = y.get(i) - meanY
            sxx += dx * dx
            sxy += dx * dy
            syy += dy * dy
        }
        val slope = sxy / sxx
        val intercept = meanY - slope * meanX
        val r = sxy / sqrt(sxx * syy)
        val residual = syy - slope * sxy
        val stdErr = sqrt(residual / (n - 2) / sxx)
        val t = slope / stdErr
        val p = regularizedIncompleteBeta((n - 2.0) / (n - 2.0 + t * t), (n - 2.0) / 2.0, 0.5)
        return LinearRegressionResult(slope, intercept, r, p.coerceIn(0.0, 1.0), stdErr)
    }

    fun skewness(values: NDArray): Double {
        val n = values.size
        val mean = values.mean()
        val std = values.std(0)
        if (std == 0.0) return 0.0
        return values.toDoubleArray().sumOf { ((it - mean) / std).pow(3) } * n / ((n - 1.0) * (n - 2.0))
    }

    fun kurtosis(values: NDArray): Double {
        val n = values.size
        val mean = values.mean()
        val std = values.std(0)
        if (std == 0.0) return 0.0
        return values.toDoubleArray().sumOf { ((it - mean) / std).pow(4) } * n * (n + 1) / ((n - 1.0) * (n - 2.0) * (n - 3.0)) -
            3.0 * (n - 1.0) * (n - 1.0) / ((n - 2.0) * (n - 3.0))
    }

    fun histogram(values: NDArray, bins: Int = 10): HistogramResult {
        require(bins > 0)
        val data = values.toDoubleArray()
        val minValue = data.minOrNull() ?: 0.0
        val maxValue = data.maxOrNull() ?: 0.0
        val counts = if (NativeScipySupport.available) {
            NativeSciPy.histogram(data, minValue, maxValue, bins)
        } else {
            IntArray(bins).also { result ->
                val width = (maxValue - minValue) / bins
                for (value in data) {
                    val index = if (width == 0.0) 0 else ((value - minValue) / width).toInt().coerceIn(0, bins - 1)
                    result[index]++
                }
            }
        }
        val width = (maxValue - minValue) / bins
        val edges = DoubleArray(bins + 1) { minValue + it * width }
        return HistogramResult(counts, edges)
    }

    private fun Double.pow(exponent: Int): Double = Math.pow(this, exponent.toDouble())

    /** 正则化不完全 Beta 函数（连分式），用于 t 分布 p 值。 */
    private fun regularizedIncompleteBeta(x: Double, a: Double, b: Double): Double {
        if (x <= 0) return 0.0
        if (x >= 1) return 1.0
        val logBeta = Special.logGamma(a) + Special.logGamma(b) - Special.logGamma(a + b)
        return if (x < (a + 1) / (a + b + 2)) {
            val logFront = a * ln(x) + b * ln(1 - x) - logBeta - ln(a)
            val logValue = logFront + ln(betaContinuedFraction(x, a, b))
            if (logValue < -745.0) 0.0 else kotlin.math.exp(logValue)
        } else {
            val logFront = b * ln(1 - x) + a * ln(x) - logBeta - ln(b)
            val logValue = logFront + ln(betaContinuedFraction(1 - x, b, a))
            val complement = if (logValue < -745.0) 0.0 else kotlin.math.exp(logValue)
            1.0 - complement
        }
    }

    private fun betaContinuedFraction(x: Double, a: Double, b: Double): Double {
        val maxIterations = 200
        val epsilon = 3e-14
        val tiny = 1e-30
        var c = 1.0
        var d = 1.0 - (a + b) * x / (a + 1)
        if (abs(d) < tiny) d = tiny
        d = 1.0 / d
        var h = d
        for (m in 1..maxIterations) {
            val m2 = 2 * m
            var aa = m * (b - m) * x / ((a + m2 - 1) * (a + m2))
            d = 1.0 + aa * d; if (abs(d) < tiny) d = tiny
            c = 1.0 + aa / c; if (abs(c) < tiny) c = tiny
            d = 1.0 / d
            h *= d * c
            aa = -(a + m) * (a + b + m) * x / ((a + m2) * (a + m2 + 1))
            d = 1.0 + aa * d; if (abs(d) < tiny) d = tiny
            c = 1.0 + aa / c; if (abs(c) < tiny) c = tiny
            d = 1.0 / d
            val delta = d * c
            h *= delta
            if (abs(delta - 1.0) < epsilon) break
        }
        return h
    }
}
