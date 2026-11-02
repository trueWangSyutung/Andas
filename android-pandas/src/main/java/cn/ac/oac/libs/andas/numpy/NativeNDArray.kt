package cn.ac.oac.libs.andas.numpy

/**
 * JNI ndarray 内核。不可用时抛出异常；调用方通过 [NativeNumpy.available] 判断。
 */
object NativeNDArray {
    init {
        try {
            System.loadLibrary("andas_native")
        } catch (_: Throwable) {
            // JVM/desktop 环境没有 libandas_native.so，自动回退到 Kotlin。
        }
    }

    external fun add(a: DoubleArray, b: DoubleArray): DoubleArray
    external fun multiply(a: DoubleArray, b: DoubleArray): DoubleArray
    external fun scale(a: DoubleArray, factor: Double): DoubleArray
    external fun sum(a: DoubleArray): Double
    external fun matmul(a: DoubleArray, m: Int, k: Int, n: Int, b: DoubleArray): DoubleArray
    /** 返回长度 2n 的实部/虚部交错数组。 */
    external fun fft(real: DoubleArray): DoubleArray
}

internal object NativeNumpy {
    val available: Boolean by lazy {
        try {
            val sum = NativeNDArray.sum(doubleArrayOf(1.0, 2.0, 3.0))
            sum == 6.0
        } catch (_: Throwable) {
            false
        }
    }
}
