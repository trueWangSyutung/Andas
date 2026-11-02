package cn.ac.oac.libs.andas.numpy

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** 复数数组（实部/虚部分离）。 */
class ComplexArray(val real: DoubleArray, val imag: DoubleArray) {
    val size: Int get() = real.size

    fun magnitude(): NDArray = NDArray.fromArray(DoubleArray(size) {
        sqrt(real[it] * real[it] + imag[it] * imag[it])
    }, size)

    fun phase(): NDArray = NDArray.fromArray(DoubleArray(size) { kotlin.math.atan2(imag[it], real[it]) }, size)
}

/** 离散傅里叶变换；长度为 2 的幂时优先走 JNI radix-2 FFT。 */
object FFT {

    fun fft(input: NDArray): ComplexArray {
        require(input.ndim == 1 && input.size > 0) { "fft 需要非空一维数组" }
        val values = input.toDoubleArray()
        if (Integer.bitCount(values.size) == 1 && NativeNumpy.available) {
            val interleaved = NativeNDArray.fft(values)
            return ComplexArray(
                DoubleArray(values.size) { interleaved[2 * it] },
                DoubleArray(values.size) { interleaved[2 * it + 1] }
            )
        }
        return dft(values, inverse = false)
    }

    fun ifft(input: ComplexArray): ComplexArray {
        require(input.size > 0)
        // 逆变换直接走 DFT，保证复数输入正确；正向 FFT 才使用 JNI radix-2。
        return dft(input.real, inverse = true, imagInput = input.imag)
    }

    fun magnitude(input: NDArray): NDArray = fft(input).magnitude()

    /** 任意长度 DFT 的 Kotlin 回退实现。 */
    private fun dft(values: DoubleArray, inverse: Boolean, imagInput: DoubleArray? = null): ComplexArray {
        val n = values.size
        val real = DoubleArray(n)
        val imag = DoubleArray(n)
        val sign = if (inverse) 1.0 else -1.0
        for (k in 0 until n) {
            var sumReal = 0.0
            var sumImag = 0.0
            for (t in 0 until n) {
                val angle = sign * 2.0 * Math.PI * k * t / n
                val c = cos(angle)
                val s = sin(angle)
                val vr = values[t]
                val vi = imagInput?.get(t) ?: 0.0
                sumReal += vr * c - vi * s
                sumImag += vr * s + vi * c
            }
            val scale = if (inverse) n.toDouble() else 1.0
            real[k] = sumReal / scale
            imag[k] = sumImag / scale
        }
        return ComplexArray(real, imag)
    }
}
