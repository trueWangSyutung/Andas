package cn.ac.oac.libs.andas

import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.ac.oac.libs.andas.numpy.FFT
import cn.ac.oac.libs.andas.numpy.Linalg
import cn.ac.oac.libs.andas.numpy.NativeNDArray
import cn.ac.oac.libs.andas.numpy.NDArray
import cn.ac.oac.libs.andas.plot.Figure
import cn.ac.oac.libs.andas.scipy.NativeSciPy
import cn.ac.oac.libs.andas.scipy.Special
import cn.ac.oac.libs.andas.scipy.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NumpyNativeInstrumentedTest {

    @Test
    fun nativeNumpyKernelsWork() {
        val sum = NativeNDArray.sum(doubleArrayOf(1.0, 2.0, 3.0))
        assertEquals(6.0, sum, 1e-12)

        val added = NativeNDArray.add(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0))
        assertEquals(4.0, added[0], 1e-12)
        assertEquals(6.0, added[1], 1e-12)

        val product = NativeNDArray.matmul(
            doubleArrayOf(1.0, 2.0, 3.0, 4.0), 2, 2, 2,
            doubleArrayOf(5.0, 6.0, 7.0, 8.0)
        )
        assertEquals(19.0, product[0], 1e-12)
        assertEquals(50.0, product[3], 1e-12)

        val fft = NativeNDArray.fft(doubleArrayOf(1.0, 1.0, 1.0, 1.0))
        assertEquals(4.0, fft[0], 1e-9)
        assertEquals(0.0, fft[1], 1e-9)

        val spectrum = FFT.fft(NDArray.of(1.0, 1.0, 1.0, 1.0))
        assertEquals(4.0, spectrum.real[0], 1e-9)
    }

    @Test
    fun nativeScipyWorks() {
        assertEquals(0.8427007929497149, NativeSciPy.erf(1.0), 1e-9)
        assertEquals(120.0, NativeSciPy.gamma(6.0), 1e-9)
        assertEquals(0.8427007929497149, Special.erf(1.0), 1e-6)
        assertEquals(0.9772498680518208, Stats.normalCdf(2.0), 1e-6)

        val convolution = NativeSciPy.convolve(doubleArrayOf(1.0, 2.0, 3.0), doubleArrayOf(1.0, 2.0))
        assertEquals(4.0, convolution[1], 1e-12)
        assertEquals(6.0, convolution[3], 1e-12)

        val histogram = NativeSciPy.histogram(
            doubleArrayOf(0.0, 0.2, 0.5, 0.9, 1.0), 0.0, 1.0, 2
        )
        assertEquals(2, histogram[0])
        assertEquals(3, histogram[1])
    }

    @Test
    fun ndarrayAndPlotWorkOnDevice() {
        val a = NDArray.from2D(arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)))
        assertEquals(10.0, a.sum(), 1e-12)
        assertEquals(-2.0, Linalg.det(a), 1e-9)

        val figure = Figure(320, 240)
        figure.title = "Android"
        figure.plot(doubleArrayOf(0.0, 1.0, 2.0), doubleArrayOf(1.0, 4.0, 9.0), "square")
        val bitmap = figure.renderBitmap()
        assertNotNull(bitmap)
        assertEquals(320, bitmap.width)
        assertEquals(240, bitmap.height)
        assertTrue(figure.renderSvg().contains("<svg"))
    }
}
