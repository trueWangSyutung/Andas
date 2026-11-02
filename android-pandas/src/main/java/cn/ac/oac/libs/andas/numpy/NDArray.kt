package cn.ac.oac.libs.andas.numpy

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * NumPy 风格 N 维数组，支持 FLOAT64/FLOAT32/INT32/INT64/BOOLEAN 存储。
 *
 * - `get(...)` 始终返回 Double（便于旧 API 兼容）；需要精确整数时使用 [getLong]/[getInt]。
 * - 算术结果提升为 FLOAT64；显式类型请使用 [astype]。
 * - 支持标量/同形状/任意维度广播（右对齐）。
 */
class NDArray internal constructor(
    internal val storage: Any,
    val shape: IntArray,
    internal val strides: IntArray,
    internal val offset: Int,
    val dtype: DType
) {
    val ndim: Int get() = shape.size
    val size: Int get() = shape.fold(1) { acc, value -> acc * value }

    init {
        require(shape.isNotEmpty()) { "shape 不能为空" }
        require(shape.all { it > 0 }) { "shape 必须为正数: ${shape.contentToString()}" }
        require(strides.size == shape.size)
        require(storageSize(storage) >= offset + logicalExtent()) { "storage 太小" }
    }

    private fun storageSize(storage: Any): Int = when (storage) {
        is DoubleArray -> storage.size
        is FloatArray -> storage.size
        is IntArray -> storage.size
        is LongArray -> storage.size
        is BooleanArray -> storage.size
        else -> 0
    }

    private fun logicalExtent(): Int {
        var extent = 1
        for (i in shape.indices) extent += (shape[i] - 1) * strides[i]
        return extent
    }

    internal fun doubleStorage(): DoubleArray? = storage as? DoubleArray

    internal fun isContiguous(): Boolean {
        if (offset != 0) return false
        var expected = 1
        for (i in shape.indices.reversed()) {
            if (strides[i] != expected) return false
            expected *= shape[i]
        }
        return storageSize(storage) == size
    }

    fun copy(): NDArray {
        val copied = when (storage) {
            is DoubleArray -> storage.copyOf()
            is FloatArray -> storage.copyOf()
            is IntArray -> storage.copyOf()
            is LongArray -> storage.copyOf()
            is BooleanArray -> storage.copyOf()
            else -> throw IllegalStateException("unsupported storage")
        }
        return NDArray(copied, shape.copyOf(), strides.copyOf(), 0, dtype)
    }

    private fun positionOfFlat(index: Int): Int {
        require(index in 0 until size) { "索引越界: $index" }
        var remaining = index
        var position = offset
        for (i in shape.indices.reversed()) {
            val coordinate = remaining % shape[i]
            remaining /= shape[i]
            position += coordinate * strides[i]
        }
        return position
    }

    private fun getDoubleAt(position: Int): Double = when (val s = storage) {
        is DoubleArray -> s[position]
        is FloatArray -> s[position].toDouble()
        is IntArray -> s[position].toDouble()
        is LongArray -> s[position].toDouble()
        is BooleanArray -> if (s[position]) 1.0 else 0.0
        else -> 0.0
    }

    private fun getLongAt(position: Int): Long = when (val s = storage) {
        is DoubleArray -> s[position].toLong()
        is FloatArray -> s[position].toLong()
        is IntArray -> s[position].toLong()
        is LongArray -> s[position]
        is BooleanArray -> if (s[position]) 1L else 0L
        else -> 0L
    }

    private fun setDoubleAt(position: Int, value: Double) {
        when (val s = storage) {
            is DoubleArray -> s[position] = value
            is FloatArray -> s[position] = value.toFloat()
            is IntArray -> s[position] = value.toInt()
            is LongArray -> s[position] = value.toLong()
            is BooleanArray -> s[position] = value != 0.0
            else -> Unit
        }
    }

    fun get(vararg indices: Int): Double {
        require(indices.size == ndim) { "需要 $ndim 个索引，实际 ${indices.size}" }
        var position = offset
        for (i in indices.indices) {
            require(indices[i] in 0 until shape[i]) { "索引越界: ${indices[i]}" }
            position += indices[i] * strides[i]
        }
        return getDoubleAt(position)
    }

    fun getLong(vararg indices: Int): Long {
        require(indices.size == ndim)
        var position = offset
        for (i in indices.indices) {
            require(indices[i] in 0 until shape[i])
            position += indices[i] * strides[i]
        }
        return getLongAt(position)
    }

    fun getInt(vararg indices: Int): Int = getLong(*indices).toInt()
    fun getBoolean(vararg indices: Int): Boolean = get(*indices) != 0.0
    fun getFloat(vararg indices: Int): Float = get(*indices).toFloat()

    fun set(value: Double, vararg indices: Int) {
        require(indices.size == ndim) { "需要 $ndim 个索引，实际 ${indices.size}" }
        var position = offset
        for (i in indices.indices) {
            require(indices[i] in 0 until shape[i]) { "索引越界: ${indices[i]}" }
            position += indices[i] * strides[i]
        }
        setDoubleAt(position, value)
    }

    fun getFlat(index: Int): Double = getDoubleAt(positionOfFlat(index))

    fun setFlat(index: Int, value: Double) = setDoubleAt(positionOfFlat(index), value)

    /** 按逻辑顺序复制为连续 DoubleArray。 */
    fun toDoubleArray(): DoubleArray {
        if (isContiguous()) {
            (storage as? DoubleArray)?.let { return it.copyOf() }
        }
        return DoubleArray(size) { getFlat(it) }
    }

    fun toFloatArray(): FloatArray = FloatArray(size) { getFlat(it).toFloat() }
    fun toIntArray(): IntArray = IntArray(size) { getLongAt(positionOfFlat(it)).toInt() }
    fun toLongArray(): LongArray = LongArray(size) { getLongAt(positionOfFlat(it)) }
    fun toBooleanArray(): BooleanArray = BooleanArray(size) { getFlat(it) != 0.0 }
    fun toList(): List<Double> = toDoubleArray().asList()

    fun to2DArray(): Array<DoubleArray> {
        require(ndim == 2) { "仅支持二维数组" }
        return Array(shape[0]) { r -> DoubleArray(shape[1]) { c -> get(r, c) } }
    }

    fun astype(target: DType): NDArray {
        if (target == dtype) return copy()
        val values = DoubleArray(size)
        val longs = if (target == DType.INT64 || target == DType.INT32) LongArray(size) else null
        for (i in 0 until size) {
            val position = positionOfFlat(i)
            values[i] = getDoubleAt(position)
            longs?.set(i, getLongAt(position))
        }
        return fromTypedArray(values, longs, target, *shape.copyOf())
    }

    fun reshape(vararg newShape: Int): NDArray {
        require(newShape.isNotEmpty() && newShape.all { it > 0 }) { "非法 shape" }
        require(newShape.fold(1) { a, b -> a * b } == size) { "reshape 元素数量不一致" }
        if (isContiguous()) {
            return NDArray(storage, newShape, rowMajorStrides(newShape), offset, dtype)
        }
        return fromValues(toDoubleArray(), dtype, *newShape)
    }

    fun flatten(): NDArray = reshape(size)

    /** 任意维度转置；不传 axes 时反转维度顺序。 */
    fun transpose(vararg axes: Int): NDArray {
        val permutation = if (axes.isEmpty()) shape.indices.reversed().toList() else axes.toList()
        require(permutation.size == ndim && permutation.sorted() == shape.indices.toList()) {
            "axes 必须是 0..${ndim - 1} 的排列"
        }
        val newShape = IntArray(ndim) { shape[permutation[it]] }
        val newStrides = IntArray(ndim) { strides[permutation[it]] }
        return NDArray(storage, newShape, newStrides, offset, dtype)
    }

    fun T(): NDArray = if (ndim <= 1) copy() else transpose()

    fun row(index: Int): NDArray {
        require(ndim == 2 && index in 0 until shape[0]) { "行索引越界" }
        return fromValues(DoubleArray(shape[1]) { get(index, it) }, dtype, shape[1])
    }

    fun col(index: Int): NDArray {
        require(ndim == 2 && index in 0 until shape[1]) { "列索引越界" }
        return fromValues(DoubleArray(shape[0]) { get(it, index) }, dtype, shape[0])
    }

    /** 按第一维切片 [start, end)，返回副本。 */
    fun slice(start: Int, end: Int): NDArray {
        require(ndim >= 1 && start in 0..shape[0] && end in start..shape[0]) { "切片范围非法" }
        if (ndim == 1) return fromValues(DoubleArray(end - start) { get(it + start) }, dtype, end - start)
        if (ndim == 2) {
            val rows = end - start
            val result = DoubleArray(rows * shape[1])
            for (r in 0 until rows) for (c in 0 until shape[1]) result[r * shape[1] + c] = get(start + r, c)
            return fromValues(result, dtype, rows, shape[1])
        }
        throw UnsupportedOperationException("当前仅支持 1D/2D 切片")
    }

    fun map(transform: (Double) -> Double): NDArray =
        fromArray(DoubleArray(size) { transform(getFlat(it)) }, *shape.copyOf())

    fun mapIndexed(transform: (Int, Double) -> Double): NDArray =
        fromArray(DoubleArray(size) { transform(it, getFlat(it)) }, *shape.copyOf())

    operator fun plus(other: NDArray): NDArray = binary(other) { a, b -> a + b }
    operator fun minus(other: NDArray): NDArray = binary(other) { a, b -> a - b }
    operator fun times(other: NDArray): NDArray = binary(other) { a, b -> a * b }
    operator fun div(other: NDArray): NDArray = binary(other) { a, b -> a / b }

    operator fun plus(value: Double): NDArray = map { it + value }
    operator fun minus(value: Double): NDArray = map { it - value }
    operator fun times(value: Double): NDArray = map { it * value }
    operator fun div(value: Double): NDArray = map { it / value }
    operator fun unaryMinus(): NDArray = map { -it }

    fun pow(value: Double): NDArray = map { it.pow(value) }
    fun abs(): NDArray = map { abs(it) }
    fun sqrt(): NDArray = map { sqrt(it) }
    fun exp(): NDArray = map { exp(it) }
    fun log(): NDArray = map { ln(it) }
    fun sin(): NDArray = map { sin(it) }
    fun cos(): NDArray = map { cos(it) }
    fun clip(minValue: Double, maxValue: Double): NDArray = map { it.coerceIn(minValue, maxValue) }

    fun sum(): Double {
        if (dtype == DType.FLOAT64 && isContiguous() && NativeNumpy.available && size > 64) {
            return NativeNDArray.sum(doubleStorage()!!)
        }
        var total = 0.0
        for (i in 0 until size) total += getFlat(i)
        return total
    }

    fun sumLong(): Long {
        var total = 0L
        for (i in 0 until size) total += getLongAt(positionOfFlat(i))
        return total
    }

    fun mean(): Double = if (size == 0) Double.NaN else sum() / size

    fun variance(ddof: Int = 1): Double {
        if (size <= ddof) return if (ddof == 0) 0.0 else Double.NaN
        var count = 0L
        var mean = 0.0
        var m2 = 0.0
        for (i in 0 until size) {
            val value = getFlat(i)
            count++
            val delta = value - mean
            mean += delta / count
            m2 += delta * (value - mean)
        }
        return m2 / (count - ddof)
    }

    fun std(ddof: Int = 1): Double = sqrt(variance(ddof))

    fun min(): Double {
        var result = Double.POSITIVE_INFINITY
        for (i in 0 until size) result = kotlin.math.min(result, getFlat(i))
        return result
    }

    fun max(): Double {
        var result = Double.NEGATIVE_INFINITY
        for (i in 0 until size) result = kotlin.math.max(result, getFlat(i))
        return result
    }

    fun argmin(): Int {
        var index = 0
        var value = Double.POSITIVE_INFINITY
        for (i in 0 until size) {
            val current = getFlat(i)
            if (current < value) { value = current; index = i }
        }
        return index
    }

    fun argmax(): Int {
        var index = 0
        var value = Double.NEGATIVE_INFINITY
        for (i in 0 until size) {
            val current = getFlat(i)
            if (current > value) { value = current; index = i }
        }
        return index
    }

    fun prod(): Double {
        var result = 1.0
        for (i in 0 until size) result *= getFlat(i)
        return result
    }

    fun norm(): Double {
        var total = 0.0
        for (i in 0 until size) {
            val value = getFlat(i)
            total += value * value
        }
        return sqrt(total)
    }

    /** 任意维度按 axis 求和；结果去掉该轴。 */
    fun sum(axis: Int): NDArray = reduceAxis(axis) { it }

    /** 任意维度按 axis 求均值；结果去掉该轴。 */
    fun mean(axis: Int): NDArray = reduceAxis(axis) { it / shape[axis] }

    private fun reduceAxis(axis: Int, normalize: (Double) -> Double): NDArray {
        require(axis in 0 until ndim) { "axis 越界: $axis" }
        val outShape = shape.filterIndexed { index, _ -> index != axis }.toIntArray()
        if (outShape.isEmpty()) return of(normalize(sum()))
        val out = DoubleArray(outShape.fold(1) { a, b -> a * b })
        val coords = IntArray(ndim)
        for (flat in 0 until size) {
            var remaining = flat
            for (d in ndim - 1 downTo 0) {
                coords[d] = remaining % shape[d]
                remaining /= shape[d]
            }
            var outIndex = 0
            var outDim = 0
            for (d in 0 until ndim) {
                if (d == axis) continue
                outIndex = outIndex * outShape[outDim] + coords[d]
                outDim++
            }
            out[outIndex] += getFlat(flat)
        }
        if (normalize(1.0) != 1.0) {
            for (i in out.indices) out[i] = normalize(out[i])
        }
        return fromArray(out, *outShape)
    }

    fun sort(): NDArray {
        require(ndim == 1) { "sort 当前仅支持一维" }
        val values = toDoubleArray()
        values.sort()
        return fromValues(values, dtype, values.size)
    }

    fun argsort(): IntArray {
        require(ndim == 1) { "argsort 当前仅支持一维" }
        val values = toDoubleArray()
        return values.indices.sortedWith(compareBy { values[it] }).toIntArray()
    }

    fun dot(other: NDArray): Double {
        require(ndim == 1 && other.ndim == 1 && size == other.size) { "dot 要求两个等长一维数组" }
        return (0 until size).sumOf { getFlat(it) * other.getFlat(it) }
    }

    fun matmul(other: NDArray): NDArray {
        require(ndim == 2 && other.ndim == 2) { "matmul 当前支持二维矩阵" }
        require(shape[1] == other.shape[0])
        val m = shape[0]
        val k = shape[1]
        val n = other.shape[1]
        if (dtype == DType.FLOAT64 && other.dtype == DType.FLOAT64 &&
            NativeNumpy.available && m * n * k > 1024
        ) {
            val left = if (isContiguous()) doubleStorage()!! else toDoubleArray()
            val right = if (other.isContiguous()) other.doubleStorage()!! else other.toDoubleArray()
            return fromArray(NativeNDArray.matmul(left, m, k, n, right), m, n)
        }
        val result = DoubleArray(m * n)
        for (i in 0 until m) for (j in 0 until n) {
            var total = 0.0
            for (p in 0 until k) total += get(i, p) * other.get(p, j)
            result[i * n + j] = total
        }
        return fromArray(result, m, n)
    }

    private fun binary(other: NDArray, op: (Double, Double) -> Double): NDArray {
        if (shape.contentEquals(other.shape)) {
            if (dtype == DType.FLOAT64 && other.dtype == DType.FLOAT64 &&
                isContiguous() && other.isContiguous() && NativeNumpy.available && size > 256
            ) {
                // 只对加法/乘法走原生；其他运算仍用统一循环。
                if (op(1.0, 2.0) == 3.0) {
                    return fromArray(NativeNDArray.add(doubleStorage()!!, other.doubleStorage()!!), *shape.copyOf())
                }
                if (op(2.0, 3.0) == 6.0) {
                    return fromArray(NativeNDArray.multiply(doubleStorage()!!, other.doubleStorage()!!), *shape.copyOf())
                }
            }
            return fromArray(DoubleArray(size) { op(getFlat(it), other.getFlat(it)) }, *shape.copyOf())
        }
        val resultShape = broadcastShape(shape, other.shape)
        val result = DoubleArray(resultShape.fold(1) { a, b -> a * b })
        val resultCoords = IntArray(resultShape.size)
        for (flat in result.indices) {
            var remaining = flat
            for (d in resultShape.indices.reversed()) {
                resultCoords[d] = remaining % resultShape[d]
                remaining /= resultShape[d]
            }
            result[flat] = op(getBroadcast(resultCoords, resultShape, this), getBroadcast(resultCoords, resultShape, other))
        }
        return fromArray(result, *resultShape)
    }

    private fun getBroadcast(coords: IntArray, resultShape: IntArray, array: NDArray): Double {
        var position = array.offset
        val offset = resultShape.size - array.ndim
        for (d in array.shape.indices) {
            val coordinate = if (array.shape[d] == 1) 0 else coords[d + offset]
            position += coordinate * array.strides[d]
        }
        return array.getDoubleAt(position)
    }

    override fun toString(): String {
        if (ndim == 1) return (0 until min(size, 8)).joinToString(prefix = "[", postfix = if (size > 8) " ...]" else "]") { getFlat(it).toString() }
        if (ndim == 2) {
            val rows = min(shape[0], 6)
            val cols = min(shape[1], 6)
            return buildString {
                append("array([")
                for (r in 0 until rows) {
                    append((0 until cols).joinToString(prefix = "[", postfix = "]") { get(r, it).toString() })
                    if (r != rows - 1) append(",\n       ")
                }
                append("])")
            }
        }
        return "NDArray(shape=${shape.contentToString()}, dtype=$dtype)"
    }

    companion object {
        private fun rowMajorStrides(shape: IntArray): IntArray {
            val strides = IntArray(shape.size)
            var stride = 1
            for (i in shape.indices.reversed()) {
                strides[i] = stride
                stride *= shape[i]
            }
            return strides
        }

        private fun broadcastShape(a: IntArray, b: IntArray): IntArray {
            val rank = max(a.size, b.size)
            val result = IntArray(rank)
            for (i in 0 until rank) {
                val da = if (i < rank - a.size) 1 else a[i - (rank - a.size)]
                val db = if (i < rank - b.size) 1 else b[i - (rank - b.size)]
                require(da == db || da == 1 || db == 1) {
                    "广播不兼容: ${a.contentToString()} 与 ${b.contentToString()}"
                }
                result[i] = max(da, db)
            }
            return result
        }

        fun zeros(vararg shape: Int): NDArray = zeros(DType.FLOAT64, *shape)

        fun zeros(dtype: DType, vararg shape: Int): NDArray {
            require(shape.isNotEmpty() && shape.all { it > 0 })
            val count = shape.fold(1) { a, b -> a * b }
            val storage = when (dtype) {
                DType.FLOAT64 -> DoubleArray(count)
                DType.FLOAT32 -> FloatArray(count)
                DType.INT32 -> IntArray(count)
                DType.INT64 -> LongArray(count)
                DType.BOOLEAN -> BooleanArray(count)
            }
            return NDArray(storage, shape.copyOf(), rowMajorStrides(shape), 0, dtype)
        }

        fun ones(vararg shape: Int): NDArray = full(1.0, *shape)

        fun full(value: Double, vararg shape: Int): NDArray = full(value, DType.FLOAT64, *shape)

        fun full(value: Double, dtype: DType, vararg shape: Int): NDArray {
            val result = zeros(dtype, *shape)
            for (i in 0 until result.size) result.setFlat(i, value)
            return result
        }

        fun of(vararg values: Double): NDArray = fromArray(values.copyOf(), values.size)

        fun ofType(dtype: DType, vararg values: Double): NDArray =
            fromValues(values.copyOf(), dtype, values.size)

        fun fromArray(data: DoubleArray, vararg shape: Int): NDArray =
            fromValues(data, DType.FLOAT64, *shape)

        fun fromValues(data: DoubleArray, dtype: DType, vararg shape: Int): NDArray {
            require(shape.isNotEmpty() && shape.all { it > 0 })
            require(data.size == shape.fold(1) { a, b -> a * b }) { "数据长度与 shape 不匹配" }
            val longs = if (dtype == DType.INT64 || dtype == DType.INT32) LongArray(data.size) { data[it].toLong() } else null
            return fromTypedArray(data, longs, dtype, *shape)
        }

        internal fun fromTypedArray(
            doubles: DoubleArray,
            longs: LongArray?,
            dtype: DType,
            vararg shape: Int
        ): NDArray {
            val count = shape.fold(1) { a, b -> a * b }
            require(doubles.size == count)
            val storage = when (dtype) {
                DType.FLOAT64 -> doubles.copyOf()
                DType.FLOAT32 -> FloatArray(count) { doubles[it].toFloat() }
                DType.INT32 -> IntArray(count) { (longs?.get(it) ?: doubles[it].toLong()).toInt() }
                DType.INT64 -> LongArray(count) { longs?.get(it) ?: doubles[it].toLong() }
                DType.BOOLEAN -> BooleanArray(count) { doubles[it] != 0.0 }
            }
            return NDArray(storage, shape.copyOf(), rowMajorStrides(shape), 0, dtype)
        }

        fun fromLongArray(data: LongArray, vararg shape: Int): NDArray {
            require(shape.isNotEmpty() && data.size == shape.fold(1) { a, b -> a * b })
            return NDArray(data.copyOf(), shape.copyOf(), rowMajorStrides(shape), 0, DType.INT64)
        }

        fun fromIntArray(data: IntArray, vararg shape: Int): NDArray {
            require(shape.isNotEmpty() && data.size == shape.fold(1) { a, b -> a * b })
            return NDArray(data.copyOf(), shape.copyOf(), rowMajorStrides(shape), 0, DType.INT32)
        }

        fun fromFloatArray(data: FloatArray, vararg shape: Int): NDArray {
            require(shape.isNotEmpty() && data.size == shape.fold(1) { a, b -> a * b })
            return NDArray(data.copyOf(), shape.copyOf(), rowMajorStrides(shape), 0, DType.FLOAT32)
        }

        fun fromBooleanArray(data: BooleanArray, vararg shape: Int): NDArray {
            require(shape.isNotEmpty() && data.size == shape.fold(1) { a, b -> a * b })
            return NDArray(data.copyOf(), shape.copyOf(), rowMajorStrides(shape), 0, DType.BOOLEAN)
        }

        /** 沿 axis 拼接数组；除 axis 外形状必须一致。 */
        fun concatenate(arrays: List<NDArray>, axis: Int = 0): NDArray {
            require(arrays.isNotEmpty())
            val ndim = arrays.first().ndim
            require(axis in 0 until ndim)
            val outShape = arrays.first().shape.copyOf()
            outShape[axis] = arrays.sumOf { it.shape[axis] }
            for (array in arrays) {
                require(array.ndim == ndim)
                for (d in 0 until ndim) {
                    if (d != axis) require(array.shape[d] == outShape[d])
                }
            }
            val out = DoubleArray(outShape.fold(1) { a, b -> a * b })
            var offsetAxis = 0
            val coords = IntArray(ndim)
            for (array in arrays) {
                for (flat in 0 until array.size) {
                    var remaining = flat
                    for (d in ndim - 1 downTo 0) {
                        coords[d] = remaining % array.shape[d]
                        remaining /= array.shape[d]
                    }
                    coords[axis] += offsetAxis
                    var outIndex = 0
                    for (d in 0 until ndim) outIndex = outIndex * outShape[d] + coords[d]
                    out[outIndex] = array.getFlat(flat)
                }
                offsetAxis += array.shape[axis]
            }
            return fromArray(out, *outShape)
        }

        /** 条件选择：condition 非零取 x，否则取 y；x/y 可为标量数组。 */
        fun where(condition: NDArray, x: NDArray, y: NDArray): NDArray {
            require(x.shape.contentEquals(y.shape) || x.size == 1 || y.size == 1)
            val shape = condition.shape.copyOf()
            val out = DoubleArray(condition.size)
            for (i in out.indices) {
                val chosen = if (condition.getFlat(i) != 0.0) x else y
                out[i] = if (chosen.size == 1) chosen.getFlat(0) else chosen.getFlat(i)
            }
            return fromArray(out, *shape)
        }

        fun from2D(rows: Array<DoubleArray>): NDArray {
            require(rows.isNotEmpty())
            val cols = rows.first().size
            require(rows.all { it.size == cols })
            val flat = DoubleArray(rows.size * cols)
            for (r in rows.indices) rows[r].copyInto(flat, r * cols)
            return fromArray(flat, rows.size, cols)
        }

        fun arange(start: Double, stop: Double, step: Double = 1.0): NDArray {
            require(step != 0.0)
            val count = max(0, ceil((stop - start) / step).toInt())
            return fromArray(DoubleArray(count) { start + it * step }, count)
        }

        fun linspace(start: Double, stop: Double, num: Int): NDArray {
            require(num > 0)
            if (num == 1) return of(start)
            val step = (stop - start) / (num - 1)
            return fromArray(DoubleArray(num) { start + it * step }, num)
        }

        fun eye(n: Int): NDArray {
            require(n > 0)
            val result = zeros(n, n)
            for (i in 0 until n) result.set(1.0, i, i)
            return result
        }
    }
}
