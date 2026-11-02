package cn.ac.oac.libs.andas.scipy

import cn.ac.oac.libs.andas.numpy.NDArray

/** 数值积分：梯形法、Simpson 法。 */
object Integrate {

    fun trapezoid(y: NDArray, dx: Double = 1.0): Double {
        require(y.ndim == 1 && y.size >= 2)
        var sum = 0.0
        for (i in 1 until y.size) sum += (y.get(i - 1) + y.get(i)) * 0.5 * dx
        return sum
    }

    fun trapezoid(x: NDArray, y: NDArray): Double {
        require(x.ndim == 1 && y.ndim == 1 && x.size == y.size && x.size >= 2)
        var sum = 0.0
        for (i in 1 until x.size) {
            sum += (x.get(i) - x.get(i - 1)) * (y.get(i - 1) + y.get(i)) * 0.5
        }
        return sum
    }

    /** 自适应 Simpson 积分。 */
    fun quad(function: (Double) -> Double, lower: Double, upper: Double, tolerance: Double = 1e-8): Double {
        require(upper > lower) { "quad 要求 upper > lower" }
        val middle = (lower + upper) / 2
        val fa = function(lower)
        val fm = function(middle)
        val fb = function(upper)
        val whole = (upper - lower) / 6 * (fa + 4 * fm + fb)
        return adaptive(function, lower, upper, fa, fm, fb, whole, tolerance, 0)
    }

    private fun adaptive(
        function: (Double) -> Double,
        a: Double, b: Double, fa: Double, fm: Double, fb: Double,
        whole: Double, tolerance: Double, depth: Int
    ): Double {
        val middle = (a + b) / 2
        val leftMiddle = (a + middle) / 2
        val rightMiddle = (middle + b) / 2
        val flm = function(leftMiddle)
        val frm = function(rightMiddle)
        val left = (middle - a) / 6 * (fa + 4 * flm + fm)
        val right = (b - middle) / 6 * (fm + 4 * frm + fb)
        val delta = left + right - whole
        if (depth >= 20 || kotlin.math.abs(delta) <= 15 * tolerance) return left + right + delta / 15
        return adaptive(function, a, middle, fa, flm, fm, left, tolerance / 2, depth + 1) +
            adaptive(function, middle, b, fm, frm, fb, right, tolerance / 2, depth + 1)
    }

    fun simpson(y: NDArray, dx: Double = 1.0): Double {
        require(y.ndim == 1 && y.size >= 3)
        val n = y.size - 1
        var sum = y.get(0) + y.get(n)
        for (i in 1 until n) {
            sum += y.get(i) * if (i % 2 == 0) 2.0 else 4.0
        }
        return sum * dx / 3.0
    }
}
