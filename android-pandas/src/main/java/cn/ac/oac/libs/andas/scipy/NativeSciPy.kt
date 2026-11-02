package cn.ac.oac.libs.andas.scipy

object NativeSciPy {
    init {
        try {
            System.loadLibrary("andas_native")
        } catch (_: Throwable) {
        }
    }

    external fun erf(x: Double): Double
    external fun gamma(x: Double): Double
    external fun convolve(a: DoubleArray, b: DoubleArray): DoubleArray
    external fun histogram(data: DoubleArray, minValue: Double, maxValue: Double, bins: Int): IntArray
}

internal object NativeScipySupport {
    val available: Boolean by lazy {
        try {
            kotlin.math.abs(NativeSciPy.erf(1.0) - 0.8427007929497149) < 1e-6
        } catch (_: Throwable) {
            false
        }
    }
}
