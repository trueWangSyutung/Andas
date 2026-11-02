package cn.ac.oac.libs.andas.numpy

import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt

/** NumPy 风格随机数生成器，固定 seed 可复现。 */
class NumpyRandom(seed: Long = System.nanoTime()) {
    private val random = Random(seed)

    fun uniform(low: Double = 0.0, high: Double = 1.0, size: Int = 1): NDArray {
        require(high > low && size > 0)
        return NDArray.fromArray(DoubleArray(size) { low + (high - low) * random.nextDouble() }, size)
    }

    fun normal(loc: Double = 0.0, scale: Double = 1.0, size: Int = 1): NDArray {
        require(scale > 0 && size > 0)
        return NDArray.fromArray(DoubleArray(size) {
            val u1 = random.nextDouble().coerceAtLeast(1e-12)
            val u2 = random.nextDouble()
            loc + scale * sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
        }, size)
    }

    fun randint(low: Int, high: Int, size: Int = 1): NDArray {
        require(high > low && size > 0)
        return NDArray.fromArray(DoubleArray(size) { (low + random.nextInt(high - low)).toDouble() }, size)
    }

    fun choice(values: DoubleArray, size: Int = 1): NDArray {
        require(values.isNotEmpty() && size > 0)
        return NDArray.fromArray(DoubleArray(size) { values[random.nextInt(values.size)] }, size)
    }

    fun shuffle(array: NDArray): NDArray {
        require(array.ndim == 1)
        val values = array.toDoubleArray()
        for (i in values.indices.reversed()) {
            val j = random.nextInt(i + 1)
            val temp = values[i]; values[i] = values[j]; values[j] = temp
        }
        return NDArray.fromArray(values)
    }
}
