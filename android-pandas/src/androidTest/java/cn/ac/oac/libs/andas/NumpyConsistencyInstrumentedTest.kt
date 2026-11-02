package cn.ac.oac.libs.andas

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.ac.oac.libs.andas.numpy.FFT
import cn.ac.oac.libs.andas.numpy.Linalg
import cn.ac.oac.libs.andas.numpy.NDArray
import cn.ac.oac.libs.andas.scipy.Signal
import cn.ac.oac.libs.andas.scipy.Stats
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/** Android（含 JNI）与 NumPy/mpmath 参考值的一致性校验。 */
@RunWith(AndroidJUnit4::class)
class NumpyConsistencyInstrumentedTest {

    private val expected: Map<String, Double> by lazy {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        assets.open("numpy_consistency.csv").bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() }.associate { line ->
                val parts = line.split(',')
                parts[0] to parts[1].toDouble()
            }
        }
    }

    private fun assertClose(key: String, actual: Double, relative: Double = 1e-10, absolute: Double = 1e-12) {
        val expectedValue = expected[key] ?: error("缺少参考值: $key")
        val difference = abs(actual - expectedValue)
        val tolerance = maxOf(absolute, relative * maxOf(abs(expectedValue), abs(actual)))
        assertTrue("$key: expected=$expectedValue actual=$actual diff=$difference", difference <= tolerance)
    }

    @Test
    fun androidMatchesNumpyReference() {
        val data = NDArray.fromArray(DoubleArray(1000) { it * 0.5 - 10.0 }, 1000)
        assertClose("size", data.size.toDouble(), 0.0, 0.0)
        assertClose("sum", data.sum())
        assertClose("mean", data.mean())
        assertClose("std", data.std())
        assertClose("variance", data.variance())
        assertClose("quantile_25", Stats.quantile(data, 0.25))
        assertClose("quantile_50", Stats.quantile(data, 0.50))
        assertClose("quantile_75", Stats.quantile(data, 0.75))

        val a = NDArray.from2D(
            arrayOf(
                doubleArrayOf(4.0, 1.0, 2.0),
                doubleArrayOf(1.0, 3.0, 0.0),
                doubleArrayOf(2.0, 0.0, 5.0)
            )
        )
        assertClose("det", Linalg.det(a))
        val solution = Linalg.solve(a, NDArray.of(1.0, 2.0, 3.0))
        for (i in 0 until 3) assertClose("solution_$i", solution.get(i))

        val matrix = NDArray.from2D(
            arrayOf(
                doubleArrayOf(1.0, 2.0, 3.0),
                doubleArrayOf(4.0, 5.0, 6.0),
                doubleArrayOf(7.0, 8.0, 10.0)
            )
        )
        val product = matrix.matmul(matrix.transpose())
        for (i in 0 until 3) {
            assertClose("matmul_${i}", product.get(i, 0))
            assertClose("matmul_${i}_1", product.get(i, 1))
        }

        val spectrum = FFT.fft(NDArray.arange(1.0, 9.0))
        for (i in 0 until 8) {
            assertClose("fft_real_$i", spectrum.real[i], 1e-9)
            assertClose("fft_imag_$i", spectrum.imag[i], 1e-9)
        }

        val convolution = Signal.convolve(NDArray.of(1.0, 2.0, 3.0), NDArray.of(4.0, 5.0))
        for (i in 0 until 4) assertClose("conv_$i", convolution.get(i))

        val regression = Stats.linregress(
            NDArray.arange(1.0, 11.0),
            NDArray.fromArray(DoubleArray(10) { 2.0 * (it + 1) + 1.0 }, 10)
        )
        assertClose("regression_slope", regression.slope)
        assertClose("regression_intercept", regression.intercept)
        assertClose("normal_cdf", Stats.normalCdf(1.5), 1e-9)
        val ttest = Stats.ttestOneSample(data, 0.0)
        assertClose("t_stat", ttest.t, 1e-9)
        assertClose("t_p", ttest.p, 1e-9, 1e-300)
    }
}
