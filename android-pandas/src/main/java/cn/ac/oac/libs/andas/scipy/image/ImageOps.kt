package cn.ac.oac.libs.andas.scipy.image

import cn.ac.oac.libs.andas.numpy.NDArray
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.sqrt

/** 轻量图像处理：灰度化、高斯模糊、Sobel 边缘、阈值、最近邻缩放。 */
object ImageOps {

    /** RGB/RGBA -> 灰度二维数组。 */
    fun grayscale(rgb: NDArray): NDArray {
        require(rgb.ndim == 3 && (rgb.shape[2] == 3 || rgb.shape[2] == 4))
        val height = rgb.shape[0]
        val width = rgb.shape[1]
        val channels = rgb.shape[2]
        val result = DoubleArray(height * width)
        for (r in 0 until height) for (c in 0 until width) {
            val red = rgb.get(r, c, 0)
            val green = rgb.get(r, c, 1)
            val blue = rgb.get(r, c, 2)
            val alpha = if (channels == 4) rgb.get(r, c, 3) / 255.0 else 1.0
            result[r * width + c] = (0.299 * red + 0.587 * green + 0.114 * blue) * alpha
        }
        return NDArray.fromArray(result, height, width)
    }

    /** 可分离高斯模糊；边界使用 clamp。 */
    fun gaussianBlur(image: NDArray, sigma: Double = 1.0): NDArray {
        require(image.ndim == 2 && sigma > 0)
        val radius = maxOf(1, ceil(3.0 * sigma).toInt())
        val kernel = DoubleArray(2 * radius + 1)
        var sum = 0.0
        for (i in -radius..radius) {
            val value = exp(-(i * i) / (2.0 * sigma * sigma)) / (sqrt(2 * Math.PI) * sigma)
            kernel[i + radius] = value
            sum += value
        }
        for (i in kernel.indices) kernel[i] /= sum
        val height = image.shape[0]
        val width = image.shape[1]
        val horizontal = DoubleArray(height * width)
        for (r in 0 until height) for (c in 0 until width) {
            var total = 0.0
            for (k in -radius..radius) {
                val cc = (c + k).coerceIn(0, width - 1)
                total += image.get(r, cc) * kernel[k + radius]
            }
            horizontal[r * width + c] = total
        }
        val result = DoubleArray(height * width)
        for (r in 0 until height) for (c in 0 until width) {
            var total = 0.0
            for (k in -radius..radius) {
                val rr = (r + k).coerceIn(0, height - 1)
                total += horizontal[rr * width + c] * kernel[k + radius]
            }
            result[r * width + c] = total
        }
        return NDArray.fromArray(result, height, width)
    }

    /** Sobel 梯度幅值。 */
    fun sobel(image: NDArray): NDArray {
        require(image.ndim == 2)
        val height = image.shape[0]
        val width = image.shape[1]
        val result = DoubleArray(height * width)
        val gx = arrayOf(intArrayOf(-1, 0, 1), intArrayOf(-2, 0, 2), intArrayOf(-1, 0, 1))
        val gy = arrayOf(intArrayOf(-1, -2, -1), intArrayOf(0, 0, 0), intArrayOf(1, 2, 1))
        for (r in 0 until height) for (c in 0 until width) {
            var sx = 0.0
            var sy = 0.0
            for (kr in -1..1) for (kc in -1..1) {
                val rr = (r + kr).coerceIn(0, height - 1)
                val cc = (c + kc).coerceIn(0, width - 1)
                val value = image.get(rr, cc)
                sx += value * gx[kr + 1][kc + 1]
                sy += value * gy[kr + 1][kc + 1]
            }
            result[r * width + c] = sqrt(sx * sx + sy * sy)
        }
        return NDArray.fromArray(result, height, width)
    }

    fun threshold(image: NDArray, threshold: Double, maxValue: Double = 255.0): NDArray =
        image.map { if (it >= threshold) maxValue else 0.0 }

    fun resizeNearest(image: NDArray, height: Int, width: Int): NDArray {
        require(image.ndim == 2 && height > 0 && width > 0)
        val sourceHeight = image.shape[0]
        val sourceWidth = image.shape[1]
        return NDArray.fromArray(DoubleArray(height * width) {
            val r = it / width
            val c = it % width
            image.get((r.toDouble() * sourceHeight / height).toInt().coerceIn(0, sourceHeight - 1),
                (c.toDouble() * sourceWidth / width).toInt().coerceIn(0, sourceWidth - 1))
        }, height, width)
    }
}
