package cn.ac.oac.libs.andas.scipy

import cn.ac.oac.libs.andas.numpy.NDArray

/** 线性插值。 */
class LinearInterpolator(x: NDArray, y: NDArray) {
    private val xs: DoubleArray
    private val ys: DoubleArray

    init {
        require(x.ndim == 1 && y.ndim == 1 && x.size == y.size && x.size >= 2)
        val pairs = (0 until x.size).map { x.get(it) to y.get(it) }.sortedBy { it.first }
        xs = pairs.map { it.first }.toDoubleArray()
        ys = pairs.map { it.second }.toDoubleArray()
        for (i in 1 until xs.size) require(xs[i - 1] < xs[i]) { "x 必须严格递增" }
    }

    fun evaluate(query: Double): Double {
        if (query <= xs.first()) return ys.first()
        if (query >= xs.last()) return ys.last()
        var low = 0
        var high = xs.size - 1
        while (high - low > 1) {
            val mid = (low + high) / 2
            if (xs[mid] <= query) low = mid else high = mid
        }
        val ratio = (query - xs[low]) / (xs[high] - xs[low])
        return ys[low] + ratio * (ys[high] - ys[low])
    }

    fun evaluate(values: NDArray): NDArray = values.map { evaluate(it) }
}

/** 自然三次样条插值。 */
class CubicSpline(x: NDArray, y: NDArray) {
    private val xs: DoubleArray
    private val ys: DoubleArray
    private val second: DoubleArray

    init {
        require(x.ndim == 1 && y.ndim == 1 && x.size == y.size && x.size >= 3)
        val pairs = (0 until x.size).map { x.get(it) to y.get(it) }.sortedBy { it.first }
        xs = pairs.map { it.first }.toDoubleArray()
        ys = pairs.map { it.second }.toDoubleArray()
        val n = xs.size
        val h = DoubleArray(n - 1) { xs[it + 1] - xs[it] }
        val alpha = DoubleArray(n)
        for (i in 1 until n - 1) alpha[i] = 3 / h[i] * (ys[i + 1] - ys[i]) - 3 / h[i - 1] * (ys[i] - ys[i - 1])
        val l = DoubleArray(n); val mu = DoubleArray(n); val z = DoubleArray(n)
        l[0] = 1.0
        for (i in 1 until n - 1) {
            l[i] = 2 * (xs[i + 1] - xs[i - 1]) - h[i - 1] * mu[i - 1]
            mu[i] = h[i] / l[i]
            z[i] = (alpha[i] - h[i - 1] * z[i - 1]) / l[i]
        }
        l[n - 1] = 1.0
        second = DoubleArray(n)
        for (j in n - 2 downTo 0) {
            second[j] = z[j] - mu[j] * second[j + 1]
        }
    }

    fun evaluate(query: Double): Double {
        if (query <= xs.first()) return ys.first()
        if (query >= xs.last()) return ys.last()
        var low = 0
        var high = xs.size - 1
        while (high - low > 1) {
            val mid = (low + high) / 2
            if (xs[mid] <= query) low = mid else high = mid
        }
        val h = xs[high] - xs[low]
        val a = (xs[high] - query) / h
        val b = (query - xs[low]) / h
        return a * ys[low] + b * ys[high] +
            ((a * a * a - a) * second[low] + (b * b * b - b) * second[high]) * h * h / 6.0
    }
}
