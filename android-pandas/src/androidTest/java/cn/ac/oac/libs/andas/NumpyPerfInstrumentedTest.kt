package cn.ac.oac.libs.andas

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.ac.oac.libs.andas.numpy.FFT
import cn.ac.oac.libs.andas.numpy.NDArray
import cn.ac.oac.libs.andas.scipy.MultipleLinearRegression
import cn.ac.oac.libs.andas.scipy.Signal
import cn.ac.oac.libs.andas.scipy.Stats
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Random

/** Android/JNI NDArray/SciPy benchmark; results are logged as `op,ms` lines. */
@RunWith(AndroidJUnit4::class)
class NumpyPerfInstrumentedTest {

    private var sink = 0.0

    private fun medianMs(warmup: Int = 1, repeats: Int = 3, operation: () -> Double): Double {
        repeat(warmup) { sink += operation() }
        val samples = DoubleArray(repeats)
        for (i in 0 until repeats) {
            val start = System.nanoTime()
            sink += operation()
            samples[i] = (System.nanoTime() - start) / 1_000_000.0
        }
        samples.sort()
        return samples[repeats / 2]
    }

    @Test
    fun benchmarkNumericKernels() {
        val random = Random(20261102)
        fun array(size: Int): DoubleArray = DoubleArray(size) { random.nextDouble() }
        val left = NDArray.fromArray(array(1_000_000), 1_000_000)
        val right = NDArray.fromArray(array(1_000_000), 1_000_000)
        val sumArray = NDArray.fromArray(array(1_000_000), 1_000_000)

        val matrix = NDArray.from2D(Array(300) { array(300) })
        val fft = NDArray.fromArray(array(1024), 1024)

        val regressionX = DoubleArray(20_000 * 5)
        val regressionY = DoubleArray(20_000)
        val weights = doubleArrayOf(1.5, -2.0, 0.5, 3.0, -1.0)
        for (i in 0 until 20_000) {
            var value = 2.0
            for (j in 0 until 5) {
                val v = random.nextDouble()
                regressionX[i * 5 + j] = v
                value += weights[j] * v
            }
            regressionY[i] = value
        }
        val features = NDArray.fromArray(regressionX, 20_000, 5)
        val target = NDArray.fromArray(regressionY, 20_000)

        val convolution = NDArray.fromArray(array(100_000), 100_000)
        val kernel = NDArray.fromArray(array(16), 16)
        val cdfData = array(100_000)

        val results = linkedMapOf(
            "add_1m" to medianMs { left.plus(right).sum() },
            "sum_1m" to medianMs { sumArray.sum() },
            "matmul_300" to medianMs { matrix.matmul(matrix.transpose()).sum() },
            "fft_1024" to medianMs { FFT.fft(fft).magnitude().sum() },
            "quantile_1m" to medianMs { Stats.quantile(sumArray, 0.5) },
            "regression_20k_x5" to medianMs { MultipleLinearRegression.fit(features, target).r2 },
            "convolve_100k_x16" to medianMs { Signal.convolve(convolution, kernel).sum() },
            "normal_cdf_100k" to medianMs {
                var total = 0.0
                for (value in cdfData) total += Stats.normalCdf(value)
                total
            }
        )
        Log.i("AndasNumpyBench", "maxMemory=${Runtime.getRuntime().maxMemory() / (1024 * 1024)}MB")
        for ((name, value) in results) {
            Log.i("AndasNumpyBench", "$name,${String.format(java.util.Locale.US, "%.4f", value)}")
        }
        Log.i("AndasNumpyBench", "sink=$sink")
    }
}
