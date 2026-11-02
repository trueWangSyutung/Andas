package cn.ac.oac.libs.andas.scipy.sparse

import cn.ac.oac.libs.andas.numpy.NDArray

/**
 * CSR 稀疏矩阵（double）。
 *
 * 适合稀疏特征、图邻接矩阵与大规模 one-hot 数据；支持与稠密向量的乘法、
 * 转置与稀疏-稀疏乘法（内部按稠密计算，后续可替换为 CSR 算法）。
 */
class SparseMatrix internal constructor(
    val rows: Int,
    val cols: Int,
    internal val values: DoubleArray,
    internal val colIndices: IntArray,
    internal val rowPointers: IntArray
) {
    val nnz: Int get() = values.size

    fun toDense(): NDArray {
        val dense = NDArray.zeros(rows, cols)
        for (r in 0 until rows) {
            for (k in rowPointers[r] until rowPointers[r + 1]) {
                dense.set(values[k], r, colIndices[k])
            }
        }
        return dense
    }

    /** 稀疏矩阵 × 稠密向量。 */
    fun multiply(vector: NDArray): NDArray {
        require(vector.ndim == 1 && vector.size == cols) { "向量长度必须等于列数" }
        val result = DoubleArray(rows)
        for (r in 0 until rows) {
            var total = 0.0
            for (k in rowPointers[r] until rowPointers[r + 1]) {
                total += values[k] * vector.get(colIndices[k])
            }
            result[r] = total
        }
        return NDArray.fromArray(result, rows)
    }

    fun transpose(): SparseMatrix {
        val counts = IntArray(cols)
        for (c in colIndices) counts[c]++
        val transposePointers = IntArray(cols + 1)
        for (c in 0 until cols) transposePointers[c + 1] = transposePointers[c] + counts[c]
        val next = transposePointers.copyOf()
        val transposeValues = DoubleArray(nnz)
        val transposeRows = IntArray(nnz)
        for (r in 0 until rows) {
            for (k in rowPointers[r] until rowPointers[r + 1]) {
                val c = colIndices[k]
                val destination = next[c]++
                transposeValues[destination] = values[k]
                transposeRows[destination] = r
            }
        }
        return SparseMatrix(cols, rows, transposeValues, transposeRows, transposePointers)
    }

    operator fun times(other: SparseMatrix): SparseMatrix {
        require(cols == other.rows) { "矩阵维度不匹配" }
        val dense = toDense().matmul(other.toDense())
        return fromDense(dense)
    }

    operator fun times(vector: NDArray): NDArray = multiply(vector)

    fun add(other: SparseMatrix): SparseMatrix {
        require(rows == other.rows && cols == other.cols)
        return fromDense(toDense() + other.toDense())
    }

    override fun toString(): String = "SparseMatrix(${rows}x$cols, nnz=$nnz)"

    companion object {
        fun fromDense(matrix: NDArray): SparseMatrix {
            require(matrix.ndim == 2)
            val rows = matrix.shape[0]
            val cols = matrix.shape[1]
            val values = ArrayList<Double>()
            val columns = ArrayList<Int>()
            val pointers = IntArray(rows + 1)
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val value = matrix.get(r, c)
                    if (value != 0.0) {
                        values.add(value)
                        columns.add(c)
                    }
                }
                pointers[r + 1] = values.size
            }
            return SparseMatrix(rows, cols, values.toDoubleArray(), columns.toIntArray(), pointers)
        }

        /** 由 (row, col, value) 三元组构建，重复坐标会累加。 */
        fun fromTriplets(rows: Int, cols: Int, triplets: List<Triplet>): SparseMatrix {
            val entries = HashMap<Long, Double>()
            for (triplet in triplets) {
                require(triplet.row in 0 until rows && triplet.col in 0 until cols)
                val key = triplet.row.toLong() * cols + triplet.col
                entries[key] = (entries[key] ?: 0.0) + triplet.value
            }
            val sorted = entries.entries.sortedBy { it.key }
            val values = sorted.map { it.value }.toDoubleArray()
            val columns = IntArray(sorted.size) { (sorted[it].key % cols).toInt() }
            val pointers = IntArray(rows + 1)
            var index = 0
            for (r in 0 until rows) {
                while (index < sorted.size && sorted[index].key / cols == r.toLong()) index++
                pointers[r + 1] = index
            }
            return SparseMatrix(rows, cols, values, columns, pointers)
        }
    }

    data class Triplet(val row: Int, val col: Int, val value: Double)
}
