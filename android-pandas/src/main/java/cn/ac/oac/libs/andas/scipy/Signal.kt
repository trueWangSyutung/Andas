package cn.ac.oac.libs.andas.scipy

import cn.ac.oac.libs.andas.numpy.FFT
import cn.ac.oac.libs.andas.numpy.NDArray
import kotlin.math.cos
import kotlin.math.sqrt

/** 信号处理：卷积、滑动平均、窗函数、去趋势、频谱。 */
object Signal {

    fun convolve(a: NDArray, b: NDArray): NDArray {
        require(a.ndim == 1 && b.ndim == 1 && a.size > 0 && b.size > 0)
        val av = a.toDoubleArray()
        val bv = b.toDoubleArray()
        val outputSize = av.size + bv.size - 1
        if (NativeScipySupport.available) return NDArray.fromArray(NativeSciPy.convolve(av, bv), outputSize)
        val result = DoubleArray(outputSize)
        for (i in av.indices) for (j in bv.indices) result[i + j] += av[i] * bv[j]
        return NDArray.fromArray(result, result.size)
    }

    fun movingAverage(values: NDArray, window: Int): NDArray {
        require(values.ndim == 1 && window > 0 && window <= values.size)
        val data = values.toDoubleArray()
        val result = DoubleArray(data.size)
        var sum = 0.0
        for (i in data.indices) {
            sum += data[i]
            if (i >= window) sum -= data[i - window]
            result[i] = if (i >= window - 1) sum / window else Double.NaN
        }
        return NDArray.fromArray(result)
    }

    fun hann(size: Int): NDArray {
        require(size > 1)
        return NDArray.fromArray(DoubleArray(size) {
            0.5 * (1.0 - cos(2.0 * Math.PI * it / (size - 1)))
        }, size)
    }

    fun hamming(size: Int): NDArray {
        require(size > 1)
        return NDArray.fromArray(DoubleArray(size) {
            0.54 - 0.46 * cos(2.0 * Math.PI * it / (size - 1))
        }, size)
    }

    fun blackman(size: Int): NDArray {
        require(size > 1)
        return NDArray.fromArray(DoubleArray(size) {
            val x = 2.0 * Math.PI * it / (size - 1)
            0.42 - 0.5 * cos(x) + 0.08 * cos(2 * x)
        }, size)
    }

    /** 基于 FFT 的快速卷积（零填充到 2 的幂）。 */
    fun fftConvolve(a: NDArray, b: NDArray): NDArray {
        require(a.ndim == 1 && b.ndim == 1 && a.size > 0 && b.size > 0)
        val outputSize = a.size + b.size - 1
        var fftSize = 1
        while (fftSize < outputSize) fftSize = fftSize shl 1
        val left = DoubleArray(fftSize)
        val right = DoubleArray(fftSize)
        for (i in 0 until a.size) left[i] = a.get(i)
        for (i in 0 until b.size) right[i] = b.get(i)
        val leftSpectrum = FFT.fft(NDArray.fromArray(left, fftSize))
        val rightSpectrum = FFT.fft(NDArray.fromArray(right, fftSize))
        val real = DoubleArray(fftSize)
        val imag = DoubleArray(fftSize)
        for (i in 0 until fftSize) {
            real[i] = leftSpectrum.real[i] * rightSpectrum.real[i] - leftSpectrum.imag[i] * rightSpectrum.imag[i]
            imag[i] = leftSpectrum.real[i] * rightSpectrum.imag[i] + leftSpectrum.imag[i] * rightSpectrum.real[i]
        }
        val inverse = FFT.ifft(cn.ac.oac.libs.andas.numpy.ComplexArray(real, imag))
        return NDArray.fromArray(DoubleArray(outputSize) { inverse.real[it] }, outputSize)
    }

    fun detrend(values: NDArray): NDArray {
        require(values.ndim == 1)
        val n = values.size
        val x = NDArray.arange(0.0, n.toDouble())
        val fit = cn.ac.oac.libs.andas.numpy.Linalg.lstsq(x, values)
        val slope = fit.get(0)
        val intercept = fit.get(1)
        return values.mapIndexed { index, value -> value - (intercept + slope * index) }
    }

    fun spectralMagnitude(values: NDArray, sampleRate: Double = 1.0): Pair<NDArray, NDArray> {
        require(values.ndim == 1)
        val spectrum = FFT.fft(values)
        val half = values.size / 2 + 1
        val frequencies = NDArray.fromArray(DoubleArray(half) { it * sampleRate / values.size }, half)
        val magnitude = spectrum.magnitude()
        val magnitudes = NDArray.fromArray(DoubleArray(half) { magnitude.get(it) }, half)
        return frequencies to magnitudes
    }

    fun rms(values: NDArray): Double = sqrt(values.toDoubleArray().sumOf { it * it } / values.size)
}
