package cn.ac.oac.libs.andas.scipy

import kotlin.math.abs
import kotlin.math.sqrt

data class MinimizeResult(val x: Double, val funValue: Double, val iterations: Int, val converged: Boolean)

/** 一维/简单多维优化。 */
object Optimize {

    /** 黄金分割搜索（单峰函数）。 */
    fun goldenSection(
        function: (Double) -> Double,
        lower: Double,
        upper: Double,
        tolerance: Double = 1e-8,
        maxIterations: Int = 200
    ): MinimizeResult {
        require(upper > lower)
        val phi = (sqrt(5.0) - 1.0) / 2.0
        var a = lower
        var b = upper
        var c = b - phi * (b - a)
        var d = a + phi * (b - a)
        var fc = function(c)
        var fd = function(d)
        var iterations = 0
        while (abs(b - a) > tolerance && iterations < maxIterations) {
            if (fc < fd) {
                b = d; d = c; fd = fc
                c = b - phi * (b - a); fc = function(c)
            } else {
                a = c; c = d; fc = fd
                d = a + phi * (b - a); fd = function(d)
            }
            iterations++
        }
        val x = (a + b) / 2
        return MinimizeResult(x, function(x), iterations, iterations < maxIterations)
    }

    fun gradientDescent(
        gradient: (DoubleArray) -> DoubleArray,
        start: DoubleArray,
        learningRate: Double = 0.01,
        iterations: Int = 1000,
        tolerance: Double = 1e-8
    ): DoubleArray {
        var x = start.copyOf()
        repeat(iterations) {
            val grad = gradient(x)
            var norm = 0.0
            for (i in x.indices) {
                x[i] -= learningRate * grad[i]
                norm += grad[i] * grad[i]
            }
            if (sqrt(norm) < tolerance) return x
        }
        return x
    }

    fun newton1D(
        function: (Double) -> Double,
        derivative: (Double) -> Double,
        start: Double = 0.0,
        tolerance: Double = 1e-10,
        maxIterations: Int = 100
    ): MinimizeResult {
        var x = start
        repeat(maxIterations) { iteration ->
            val slope = derivative(x)
            if (abs(slope) < tolerance) {
                return MinimizeResult(x, function(x), iteration, true)
            }
            x -= function(x) / slope
            if (abs(function(x)) < tolerance) return MinimizeResult(x, function(x), iteration + 1, true)
        }
        return MinimizeResult(x, function(x), maxIterations, false)
    }
}
