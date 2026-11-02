package cn.ac.oac.libs.andas.scipy

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/** 特殊函数：误差函数、Gamma、Log-Gamma。 */
object Special {

    fun erf(x: Double): Double {
        if (NativeScipySupport.available) return NativeSciPy.erf(x)
        if (x == 0.0) return 0.0
        val negative = x < 0
        val ax = abs(x)
        if (ax >= 4.0) return if (negative) -1.0 else 1.0
        // erf 的 Maclaurin 级数，|x|<4 时收敛稳定。
        var term = ax
        var sum = ax
        var n = 1
        while (n < 200) {
            term *= -ax * ax / n
            val contribution = term / (2 * n + 1)
            sum += contribution
            if (abs(contribution) < 1e-18) break
            n++
        }
        val result = 2.0 / sqrt(PI) * sum
        return if (negative) -result else result
    }

    fun gamma(x: Double): Double {
        if (NativeScipySupport.available) return NativeSciPy.gamma(x)
        if (x < 0.5) return PI / (kotlin.math.sin(PI * x) * gamma(1.0 - x))
        val z = x - 1.0
        val coefficients = doubleArrayOf(
            0.99999999999980993, 676.5203681218851, -1259.1392167224028,
            771.32342877765313, -176.61502916214059, 12.507343278686905,
            -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7
        )
        var sum = coefficients[0]
        for (i in 1 until coefficients.size) sum += coefficients[i] / (z + i)
        val t = z + 7.5
        return sqrt(2 * PI) * t.pow(z + 0.5) * exp(-t) * sum
    }

    fun logGamma(x: Double): Double {
        if (x < 0.5) return ln(PI) - ln(kotlin.math.sin(PI * x)) - logGamma(1.0 - x)
        val z = x - 1.0
        val coefficients = doubleArrayOf(
            0.99999999999980993, 676.5203681218851, -1259.1392167224028,
            771.32342877765313, -176.61502916214059, 12.507343278686905,
            -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7
        )
        var sum = coefficients[0]
        for (i in 1 until coefficients.size) sum += coefficients[i] / (z + i)
        val t = z + 7.5
        return 0.5 * ln(2 * PI) + (z + 0.5) * ln(t) - t + ln(sum)
    }

    fun beta(a: Double, b: Double): Double = gamma(a) * gamma(b) / gamma(a + b)

    fun logBeta(a: Double, b: Double): Double = logGamma(a) + logGamma(b) - logGamma(a + b)
}
