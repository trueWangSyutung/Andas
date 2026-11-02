package cn.ac.oac.libs.andas.numpy

import kotlin.math.abs
import kotlin.math.sqrt

/** 基础线性代数：行列式、求逆、线性方程、最小二乘、幂迭代。 */
object Linalg {

    fun det(matrix: NDArray): Double {
        require(matrix.ndim == 2 && matrix.shape[0] == matrix.shape[1]) { "det 需要方阵" }
        val a = matrix.to2DArray()
        val n = a.size
        var result = 1.0
        for (i in 0 until n) {
            var pivot = i
            for (r in i until n) if (abs(a[r][i]) > abs(a[pivot][i])) pivot = r
            if (abs(a[pivot][i]) < 1e-15) return 0.0
            if (pivot != i) {
                val temp = a[i]; a[i] = a[pivot]; a[pivot] = temp
                result = -result
            }
            result *= a[i][i]
            for (r in i + 1 until n) {
                val factor = a[r][i] / a[i][i]
                for (c in i + 1 until n) a[r][c] -= factor * a[i][c]
            }
        }
        return result
    }

    fun inv(matrix: NDArray): NDArray {
        require(matrix.ndim == 2 && matrix.shape[0] == matrix.shape[1]) { "inv 需要方阵" }
        val n = matrix.shape[0]
        val a = Array(n) { r -> DoubleArray(2 * n) { c ->
            when {
                c < n -> matrix.get(r, c)
                c - n == r -> 1.0
                else -> 0.0
            }
        } }
        for (i in 0 until n) {
            var pivot = i
            for (r in i until n) if (abs(a[r][i]) > abs(a[pivot][i])) pivot = r
            require(abs(a[pivot][i]) > 1e-15) { "矩阵不可逆" }
            if (pivot != i) { val t = a[i]; a[i] = a[pivot]; a[pivot] = t }
            val divisor = a[i][i]
            for (c in 0 until 2 * n) a[i][c] /= divisor
            for (r in 0 until n) {
                if (r == i) continue
                val factor = a[r][i]
                if (factor == 0.0) continue
                for (c in 0 until 2 * n) a[r][c] -= factor * a[i][c]
            }
        }
        return NDArray.from2D(Array(n) { r -> DoubleArray(n) { c -> a[r][c + n] } })
    }

    fun solve(a: NDArray, b: NDArray): NDArray {
        require(a.ndim == 2 && a.shape[0] == a.shape[1]) { "solve 需要方阵 A" }
        require(b.ndim == 1 && b.size == a.shape[0]) { "solve 需要长度匹配的向量 b" }
        val n = a.shape[0]
        val m = Array(n) { r -> DoubleArray(n + 1) { c ->
            if (c < n) a.get(r, c) else b.get(r)
        } }
        for (i in 0 until n) {
            var pivot = i
            for (r in i until n) if (abs(m[r][i]) > abs(m[pivot][i])) pivot = r
            require(abs(m[pivot][i]) > 1e-15) { "矩阵奇异，无法求解" }
            if (pivot != i) { val t = m[i]; m[i] = m[pivot]; m[pivot] = t }
            val divisor = m[i][i]
            for (c in i until n + 1) m[i][c] /= divisor
            for (r in 0 until n) {
                if (r == i) continue
                val factor = m[r][i]
                if (factor == 0.0) continue
                for (c in i until n + 1) m[r][c] -= factor * m[i][c]
            }
        }
        return NDArray.fromArray(DoubleArray(n) { m[it][n] }, n)
    }

    /** 最小二乘：返回 [斜率, 截距]。 */
    fun lstsq(x: NDArray, y: NDArray): NDArray {
        require(x.ndim == 1 && y.ndim == 1 && x.size == y.size) { "lstsq 需要等长一维数组" }
        val n = x.size
        val design = NDArray.from2D(Array(n) { doubleArrayOf(1.0, x.get(it)) })
        val xt = design.transpose()
        val normal = xt.matmul(design)
        val rhs = xt.matmul(NDArray.from2D(Array(n) { doubleArrayOf(y.get(it)) }))
        val beta = solve(normal, rhs.flatten())
        return NDArray.of(beta.get(1), beta.get(0))
    }

    /** 幂迭代求主特征值与特征向量。 */
    fun powerIteration(matrix: NDArray, iterations: Int = 100, tolerance: Double = 1e-10): Pair<Double, NDArray> {
        require(matrix.ndim == 2 && matrix.shape[0] == matrix.shape[1]) { "需要方阵" }
        var vector = NDArray.ones(matrix.shape[0])
        var eigenvalue = 0.0
        repeat(iterations) {
            val next = matrix.matmul(vector)
            val norm = next.norm()
            if (norm == 0.0) return@repeat
            eigenvalue = norm
            vector = next / norm
        }
        return eigenvalue to vector
    }

    fun norm(matrix: NDArray): Double = sqrt(matrix.toDoubleArray().sumOf { it * it })
}
