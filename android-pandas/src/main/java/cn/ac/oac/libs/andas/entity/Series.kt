package cn.ac.oac.libs.andas.entity

import cn.ac.oac.libs.andas.types.AndaTypes
import cn.ac.oac.libs.andas.types.typeName
import cn.ac.oac.libs.andas.core.NativeMath
import java.util.AbstractList
import cn.ac.oac.libs.andas.core.NativeBatch

/**
 * Series 类似于一维数组，可以存储任何数据类型，并通过标签（索引）访问元素
 * 它是 Andas（Android Pandas）库中的核心数据结构之一
 *
 * @param T 数据类型
 */
class Series<T> private constructor(
    rawData: List<T?>,
    rawIndex: List<Any>?,
    name: String?,
    dtype: AndaTypes?,
    copyData: Boolean,
    validate: Boolean
) {
    private var data: List<T?>
    private var index: List<Any> // 只能是 Int 或 String
    private var name: String?
    private var dtype: AndaTypes?
    // 索引到位置的映射改为惰性构建：绝大多数计算都按位置访问，不需要该映射。
    private var indexToPosition: Map<Any, Int>? = null

    // ==================== 列式原始类型存储 ====================
    // CSV 等批量读取路径可以直接存储 DoubleArray/LongArray/IntArray/BooleanArray，
    // 避免为每个单元格创建装箱对象，显著降低大文件的内存占用。
    // 当这些字段非空时，data 是一个按需装箱的只读视图。
    internal var doubleStorage: DoubleArray? = null
        private set
    internal var longStorage: LongArray? = null
        private set
    internal var intStorage: IntArray? = null
        private set
    internal var boolStorage: BooleanArray? = null
        private set
    // true 表示该位置有值；仅当列中存在空值时才创建
    internal var presenceMask: BooleanArray? = null
        private set

    init {
        this.data = if (copyData) rawData.toList() else rawData
        val rawIndex = rawIndex ?: RangeIndexList(this.data.size)

        // 验证索引类型：只能是Int或String（共享的范围索引天然合法，无需逐项检查）
        if (validate && rawIndex !is RangeIndexList) {
            validateIndexType(rawIndex)
        }

        this.index = rawIndex
        this.name = name
        this.dtype = dtype ?: if (this.data.isNotEmpty()) guessDtype(this.data.first()) else null

        // 验证索引和数据长度是否一致
        if (this.index.size != this.data.size) {
            throw IllegalArgumentException("Index和Data的长度必须一致")
        }
    }

    /**
     * 构造函数 - 通过列表数据创建Series
     *
     * @param data 数据列表
     * @param index 索引列表，默认为从0开始的整数序列，只能是Int或String
     * @param name Series名称
     * @param dtype 数据类型
     */
    constructor(
        data: List<T?>,
        index: List<Any>? = null,
        name: String? = null,
        dtype: AndaTypes? = null
    ) : this(data, index, name, dtype, true, true)

    /**
     * 构造函数 - 通过Map创建Series
     *
     * @param map 键值对映射，键作为索引，值作为数据，索引只能是Int或String
     * @param name Series名称
     */
    constructor(
        map: Map<Any, T?>,
        name: String? = null
    ) : this(map.values.toList(), map.keys.toList(), name, null, false, true)

    /**
     * 验证索引类型：只能是Int或String
     */
    private fun validateIndexType(indexList: List<Any>) {
        for (item in indexList) {
            when (item) {
                is Int, is String -> continue
                else -> throw IllegalArgumentException("索引类型必须是Int或String，但得到了: ${item::class.simpleName}")
            }
        }
    }

    /**
     * 构建索引到位置的映射表（仅在按标签访问时调用）
     */
    private fun buildIndexToPositionMap(): Map<Any, Int> {
        val map = HashMap<Any, Int>(index.size * 2)
        for ((position, label) in index.withIndex()) {
            map[label] = position
        }
        return map
    }

    /**
     * 获取Series的索引
     */
    fun index(): List<Any> = index

    /**
     * 获取Series的数据
     */
    fun values(): List<T?> = data

    /**
     * 获取Series的数据类型
     */
    fun dtype(): AndaTypes? = dtype

    /**
     * 获取Series的大小
     */
    fun size(): Int = data.size

    /**
     * 获取Series的名称
     */
    fun name(): String? = name

    /**
     * 获取Series的形状
     */
    fun shape(): Int = data.size




    operator fun times(number: Number): Series<T> {
        val resultData = data.map { item ->
            when (item) {
                is Number -> {
                    // 根据不同的数值类型进行相应的乘法运算
                    when (item) {
                        is Double -> (item * number.toDouble()) as T
                        is Float -> (item * number.toFloat()) as T
                        is Long -> (item * number.toLong()) as T
                        is Int -> (item * number.toInt()) as T
                        is Short -> (item * number.toShort()) as T
                        is Byte -> (item * number.toByte()) as T
                        else -> item
                    }
                }
                else -> item  // 非数值类型保持不变
            }
        }

        return Series(resultData, index, name, dtype)
    }

    /**
     * 向量加法运算（仅适用于数值类型）
     * 
     * @param other 另一个Series
     * @return 加法结果的新Series
     */
    operator fun plus(other: Series<*>): Series<Double> {
        if (this.size() != other.size()) {
            throw IllegalArgumentException("两个Series的大小必须相同才能进行加法运算")
        }

        val resultData = this.data.zip(other.data) { a, b ->
            when {
                a is Number && b is Number -> a.toDouble() + b.toDouble()
                a == null || b == null -> null
                else -> throw IllegalArgumentException("不支持的类型进行加法运算")
            }
        }

        return Series(resultData, this.index, this.name)
    }


    /**
     * 向量减法运算（仅适用于数值类型）
     *
     * @param other 另一个Series
     * @return 加法结果的新Series
     */
    operator fun minus(other: Series<*>): Series<Double> {
        if (this.size() != other.size()) {
            throw IllegalArgumentException("两个Series的大小必须相同才能进行加法运算")
        }

        val resultData = this.data.zip(other.data) { a, b ->
            when {
                a is Number && b is Number -> a.toDouble() - b.toDouble()
                a == null || b == null -> null
                else -> throw IllegalArgumentException("不支持的类型进行加法运算")
            }
        }

        return Series(resultData, this.index, this.name)
    }

    /**
     * 乘法法运算（仅适用于数值类型）
     * 当且仅当 v1(1,2,3) v2(2,3,4) -> v1*v2 = (2,6,12)
     *
     * @param other 另一个Series
     * @return 加法结果的新Series
     */
    operator fun times(other: Series<*>): Series<Double> {
        if (this.size() != other.size()) {
            throw IllegalArgumentException("两个Series的大小必须相同才能进行加法运算")
        }

        val resultData = this.data.zip(other.data) { a, b ->
            when {
                a is Number && b is Number -> a.toDouble() * b.toDouble()
                a == null || b == null -> null
                else -> throw IllegalArgumentException("不支持的类型进行加法运算")
            }
        }

        return Series(resultData, this.index, this.name)
    }

    /**
     * 点积运算（仅适用于数值类型）
     *
     * @param other 另一个Series
     * @return 点积结果
     */
    fun dot(other: Series<*>): Double {
        if (this.size() != other.size()) {
            throw IllegalArgumentException("两个Series的大小必须相同才能进行点积运算")
        }

        val doubleArray1 = this.data
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()

        val doubleArray2 = other.data
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()

        return if (this.isNativeAvailable()) {
            NativeMath.dotProduct(doubleArray1, doubleArray2)
        } else {
            // 回退到Kotlin实现
            doubleArray1.zip(doubleArray2) { a, b -> a * b }.sum()
        }
    }

    /**
     * 范数运算（仅适用于数值类型）
     *
     * @return 范数值
     */
    fun norm(): Double {
        val doubleArray = data
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()

        return if (this.isNativeAvailable()) {
            NativeMath.norm(doubleArray)
        } else {
            // 回退到Kotlin实现
            Math.sqrt(doubleArray.sumOf { it * it })
        }
    }

    /**
     * 基准测试方法
     *
     * @param operationType 操作类型
     * @param dataSize 数据大小
     * @return 耗时（毫秒）
     */
    fun benchmarkOperation(operationType: Int, dataSize: Int): Long {
        return try {
            NativeMath.Benchmark.measureOperationTime(operationType, dataSize)
        } catch (e: Exception) {
            // 如果原生库不可用，返回-1
            -1L
        }
    }
    /**
     * 复制Series，可选修改属性
     */
    fun copy(
        data: List<T?>? = null,
        index: List<Any>? = null,
        name: String? = null,
        dtype: AndaTypes? = null
    ): Series<T> {
        // Series 是不可变视图，无参数时直接复用，避免对大列做无意义的复制。
        if (data == null && index == null && name == null && dtype == null) return this
        val newData = data ?: this.data
        val newIndex = index ?: this.index
        return Series(newData, newIndex, name ?: this.name, dtype ?: this.dtype)
    }


    /**
     * 通过标签索引获取元素
     *
     * @param label 标签索引
     * @return 对应标签的元素
     */
    operator fun get(label: String): T? {
        // label: Any 只能是 Int 和 String
        val position = positionOf(label)
        if (position < 0) {
            throw NoSuchElementException("未找到索引: $label")
        }
        return data[position]
    }

    /**
     * 按标签查找位置，未找到返回 -1。
     * 索引映射采用惰性构建，避免每个列/Series 都额外持有一个 HashMap。
     */
    internal fun positionOf(label: Any): Int {
        if (index is RangeIndexList) {
            val i = label as? Int ?: return -1
            return if (i in index.indices) i else -1
        }
        val map = indexToPosition ?: buildIndexToPositionMap().also { indexToPosition = it }
        return map[label] ?: -1
    }

    /**
     * 通过索引位置获取元素
     *
     * @param position 索引位置
     * @return 对应位置的元素
     */
    operator fun get(position: Int): T? {
        if (position < 0 || position >= data.size) {
            return null;
        }
        return data[position]
    }


    /**
     * 获取前n个元素
     *
     * @param n 元素个数，默认为5
     * @return 包含前n个元素的新Series
     */
    fun head(n: Int = 5): Series<T> {
        val actualN = kotlin.math.min(kotlin.math.max(n, 0), data.size)
        return take(IntArray(actualN) { it })
    }

    /**
     * 获取后n个元素
     *
     * @param n 元素个数，默认为5
     * @return 包含后n个元素的新Series
     */
    fun tail(n: Int = 5): Series<T> {
        val actualN = kotlin.math.min(kotlin.math.max(n, 0), data.size)
        val start = data.size - actualN
        return take(IntArray(actualN) { start + it })
    }

    /**
     * 检查哪些元素为空值
     *
     * @return 布尔型Series，标识每个元素是否为空
     */
    fun isnull(): Series<Boolean> {
        val nullFlags = data.map { it == null }
        return Series(nullFlags, index, if (name != null) "${name}_isnull" else null)
    }

    /**
     * 检查哪些元素不为空值
     *
     * @return 布尔型Series，标识每个元素是否不为空
     */
    fun notnull(): Series<Boolean> {
        val notNullFlags = data.map { it != null }
        return Series(notNullFlags, index, if (name != null) "${name}_notnull" else null)
    }

    /**
     * 获取唯一的值
     *
     * @return 去重后的值列表
     */
    fun unique(): List<T?> {
        val seen = mutableSetOf<T?>()
        val result = mutableListOf<T?>()
        for (item in data) {
            if (!seen.contains(item)) {
                seen.add(item)
                result.add(item)
            }
        }
        return result
    }

    /**
     * 获取每个唯一值的出现次数
     *
     * @return 包含值和对应出现次数的Map
     */
    fun valueCounts(): Map<T?, Int> {
        val counts = mutableMapOf<T?, Int>()
        for (item in data) {
            counts[item] = counts.getOrDefault(item, 0) + 1
        }
        return counts
    }

    /**
     * 对Series中的元素应用函数
     *
     * @param transform 要应用的函数
     * @return 应用函数后的新Series
     */
    fun <R> map(transform: (T?) -> R?): Series<R> {
        val transformedData = data.map(transform)
        return Series(transformedData, index, name)
    }

    /**
     * 筛选满足条件的元素
     *
     * @param predicate 判断条件函数
     * @return 满足条件的新Series
     */
    fun filter(predicate: (T?) -> Boolean): Series<T> {
        val filteredData = mutableListOf<T?>()
        val filteredIndex = mutableListOf<Any>()
        
        for (i in data.indices) {
            if (predicate(data[i])) {
                filteredData.add(data[i])
                filteredIndex.add(index[i])
            }
        }
        
        return Series(filteredData, filteredIndex, name, dtype)
    }

    /**
     * 对Series进行排序（按键索引排序）
     *
     * @return 排序后的新Series
     */
    fun sortIndex(): Series<T> {
        val positions = Array(index.size) { it }
        positions.sortWith(Comparator { a, b ->
            val ia = index[a]
            val ib = index[b]
            if (ia is Int && ib is Int) ia.compareTo(ib) else ia.toString().compareTo(ib.toString())
        })
        return take(IntArray(positions.size) { positions[it] })
    }

    /**
     * 根据值对Series进行排序
     *
     * @param descending 是否降序排列，默认为false（升序）
     * @return 排序后的新Series
     */
    fun sortValues(descending: Boolean = false): Series<T> {
        // 注意：这个实现仅适用于可比较的类型
        try {
            return sortValuesNative(descending)
        }catch (e: Exception){
            val sortedPairs = if (descending) {
                index.zip(data).sortedByDescending { it.second?.toString() }
            } else {
                index.zip(data).sortedBy { it.second?.toString() }
            }
            val sortedIndex = sortedPairs.map { it.first }
            val sortedData = sortedPairs.map { it.second }
            return Series(sortedData, sortedIndex, name, dtype)
        }

    }

    /**
     * 丢弃空值
     *
     * @return 不包含空值的新Series
     */
    fun dropna(): Series<T> {
        val mask = presenceMask
        if (mask != null) {
            var kept = 0
            for (p in mask) if (p) kept++
            val positions = IntArray(kept)
            var k = 0
            for (i in mask.indices) if (mask[i]) positions[k++] = i
            return take(positions)
        }
        var nulls = 0
        for (v in data) if (v == null) nulls++
        if (nulls == 0) return this
        val positions = IntArray(data.size - nulls)
        var k = 0
        for (i in data.indices) if (data[i] != null) positions[k++] = i
        return take(positions)
    }

    /**
     * 填充空值
     *
     * @param value 用于填充空值的值
     * @return 填充空值后的新Series
     */
    fun fillna(value: T): Series<T> {
        val filledData = data.map { it ?: value }
        return Series(filledData, index, name, dtype)
    }

    /**
     * 将Series转换为List
     *
     * @return 包含所有数据的List
     */
    fun toList(): List<T?> = data.toList()

    /**
     * 将Series转换为Map
     *
     * @return 以索引为键，数据为值的Map
     */
    fun toMap(): Map<Any, T?> = index.zip(data).toMap()

    /**
     * 累计求和（仅适用于数值类型）
     *
     * @return 累计求和结果的Series
     */
    fun cumsum(): Series<Double> {
        if (data.isEmpty()) {
            return Series<Double>(emptyList(), index, name)
        }
        
        val result = mutableListOf<Double?>()
        var sum = 0.0
        
        for (item in data) {
            if (item is Number) {
                sum += item.toDouble()
                result.add(sum)
            } else {
                result.add(null)
            }
        }
        
        return Series<Double>(result, index, if (name != null) "${name}_cumsum" else null)
    }

    override fun toString(): String {
        val builder = StringBuilder()
        for (i in 0 until kotlin.math.min(10, data.size)) {  // 最多显示10项
            builder.append("${index[i]} \t\t ${data[i]}\n")
        }
        
        if (data.size > 10) {
            builder.append("...\n")
        }
        
        builder.append("Name: $name, dtype: ${dtype?.typeName() ?: "unknown"}")
        return builder.toString()
    }

    /**
     * 猜测数据类型
     */
    private fun guessDtype(value: T?): AndaTypes? {
        return when (value) {
            is Byte -> AndaTypes.INT8
            is Short -> AndaTypes.INT16
            is Int -> AndaTypes.INT32
            is Long -> AndaTypes.INT64
            is Float -> AndaTypes.FLOAT32
            is Double -> AndaTypes.FLOAT64
            is Boolean -> AndaTypes.BOOL
            is String -> AndaTypes.STRING
            else -> null
        }
    }
    
    // ==================== JNI 原生高性能操作 ====================
    fun sumKt(): Double = sumNumeric()

    /**
     * 计算和（数值列）
     *
     * 直接基于列式原始数组/单次遍历，避免 filterNotNull + map 产生两个中间 List。
     */
    fun sum(): Double {
        if (data.isEmpty()) return 0.0
        val storage = doubleStorage
        if (storage != null && presenceMask == null && storage.size > NATIVE_THRESHOLD && NativeAvailability.available) {
            return NativeMath.sumDoubleArray(storage)
        }
        return sumNumeric()
    }

    /**
     * 计算均值（数值列）
     */
    fun mean(): Double {
        if (data.isEmpty()) return 0.0
        var count = 0L
        var sum = 0.0
        forEachNumeric { sum += it; count++ }
        return if (count == 0L) 0.0 else sum / count
    }

    /**
     * 计算最大值（数值列）
     */
    fun max(): Double {
        if (data.isEmpty()) return 0.0
        var found = false
        var max = Double.NEGATIVE_INFINITY
        forEachNumeric { v -> if (!found || v > max) { max = v; found = true } }
        return if (found) max else 0.0
    }

    /**
     * 计算最小值（数值列）
     */
    fun min(): Double {
        if (data.isEmpty()) return 0.0
        var found = false
        var min = Double.POSITIVE_INFINITY
        forEachNumeric { v -> if (!found || v < min) { min = v; found = true } }
        return if (found) min else 0.0
    }

    /**
     * 计算方差（样本方差，ddof=1，与 pandas 一致）
     */
    fun variance(): Double = varianceNumeric()

    /**
     * 计算标准差（样本标准差，ddof=1，与 pandas 一致）
     */
    fun std(): Double = kotlin.math.sqrt(varianceNumeric())

    /**
     * 标准化（z-score，总体标准差）
     */
    fun normalize(): Series<Double> {
        if (data.isEmpty()) {
            return Series(emptyList(), index, if (name != null) "${name}_normalized" else null)
        }

        var count = 0L
        var sum = 0.0
        var sumSq = 0.0
        forEachNumeric { v ->
            sum += v
            sumSq += v * v
            count++
        }
        if (count == 0L) {
            return Series(emptyList(), index, if (name != null) "${name}_normalized" else null)
        }
        val mean = sum / count
        val variance = (sumSq / count) - mean * mean
        val std = kotlin.math.sqrt(if (variance > 0) variance else 0.0)

        val result = DoubleArray(data.size)
        var i = 0
        for (v in data) {
            result[i++] = if (v is Number) {
                if (std > 0) (v.toDouble() - mean) / std else 0.0
            } else {
                Double.NaN
            }
        }
        val series = fromDoubleArray(result, null, index, if (name != null) "${name}_normalized" else null, AndaTypes.FLOAT64)
        @Suppress("UNCHECKED_CAST")
        return series as Series<Double>
    }

    /**
     * 按数值排序（保留原索引）
     */
    fun sortValuesNative(descending: Boolean = false): Series<T> {
        if (data.isEmpty()) return this
        val positions = sortedPositions(descending)
        return take(positions)
    }

    /**
     * 大于阈值的筛选（保留原索引）
     */
    fun filterGreaterThan(threshold: Double): Series<T> {
        if (data.isEmpty()) return this
        val positions = ArrayList<Int>()
        for (i in data.indices) {
            val v = data[i]
            if (v is Number && v.toDouble() > threshold) positions.add(i)
        }
        return take(positions.toIntArray())
    }

    /**
     * 查找空值索引
     */
    fun findNullIndices(): List<Int> {
        val result = ArrayList<Int>()
        val mask = presenceMask
        if (mask != null) {
            for (i in mask.indices) if (!mask[i]) result.add(i)
            return result
        }
        for (i in data.indices) if (data[i] == null) result.add(i)
        return result
    }

    /**
     * 丢弃空值（保留原索引）
     */
    fun dropNullValues(): Series<T> = dropna()

    /**
     * 用常量填充空值
     */
    fun fillNullWithConstant(value: Double): Series<Double> {
        val result = DoubleArray(data.size)
        for (i in data.indices) {
            val v = data[i]
            result[i] = if (v is Number) v.toDouble() else value
        }
        val series = fromDoubleArray(result, null, index, if (name != null) "${name}_filled" else null, AndaTypes.FLOAT64)
        @Suppress("UNCHECKED_CAST")
        return series as Series<Double>
    }

    /**
     * 排序索引（与 pandas 类似：空值排在最后）
     */
    fun sortIndices(descending: Boolean = false): List<Int> =
        sortedPositions(descending).toList()

    /**
     * 统计描述（count/mean/std/min/max）
     */
    fun describe(): Map<String, Double> {
        var count = 0L
        var mean = 0.0
        var m2 = 0.0
        var min = Double.POSITIVE_INFINITY
        var max = Double.NEGATIVE_INFINITY
        forEachNumeric { v ->
            count++
            val delta = v - mean
            mean += delta / count
            m2 += delta * (v - mean)
            if (v < min) min = v
            if (v > max) max = v
        }
        if (count == 0L) {
            return mapOf(
                "count" to 0.0,
                "mean" to Double.NaN,
                "std" to Double.NaN,
                "min" to Double.NaN,
                "max" to Double.NaN
            )
        }
        val std = if (count > 1) kotlin.math.sqrt(m2 / (count - 1)) else 0.0
        return mapOf(
            "count" to count.toDouble(),
            "mean" to mean,
            "std" to std,
            "min" to min,
            "max" to max
        )
    }

    /**
     * 随机采样（保留原索引）
     */
    fun sample(sampleSize: Int): Series<T> {
        if (data.isEmpty() || sampleSize <= 0) {
            return Series(emptyList(), index, name, dtype)
        }
        if (sampleSize >= data.size) {
            return this.copy()
        }

        val random = java.util.Random()
        val picked = java.util.HashSet<Int>(sampleSize * 2)
        while (picked.size < sampleSize) {
            picked.add(random.nextInt(data.size))
        }
        return take(picked.toIntArray())
    }

    /**
     * 批量处理（保留兼容；原生库可用时走原生批量处理）
     */
    fun processBatch(batchSize: Int = 1000): Series<Double> {
        if (data.isEmpty()) {
            return Series(emptyList(), index, if (name != null) "${name}_processed" else null)
        }

        val doubleArray = toDoubleArrayNumeric()
        val result = if (NativeAvailability.available) {
            NativeBatch.processBatch(doubleArray, batchSize)
        } else {
            doubleArray
        }
        val series = fromDoubleArray(result, null, index, if (name != null) "${name}_processed" else null, AndaTypes.FLOAT64)
        @Suppress("UNCHECKED_CAST")
        return series as Series<Double>
    }

    /**
     * 检查原生库是否可用（结果缓存，避免每次计算都触发 JNI 探测）
     */
    fun isNativeAvailable(): Boolean = NativeAvailability.available

    /** 只修改名称，复用列式存储，避免重命名大列时装箱。 */
    internal fun renamed(newName: String): Series<T> {
        val s = Series(data, index, newName, dtype, false, false)
        s.doubleStorage = doubleStorage
        s.longStorage = longStorage
        s.intStorage = intStorage
        s.boolStorage = boolStorage
        s.presenceMask = presenceMask
        return s
    }

    // ==================== 内部高性能工具 ====================

    private inline fun forEachNumeric(action: (Double) -> Unit) {
        doubleStorage?.let { values ->
            val mask = presenceMask
            if (mask == null) {
                for (v in values) action(v)
            } else {
                for (i in values.indices) if (mask[i]) action(values[i])
            }
            return
        }
        intStorage?.let { values ->
            val mask = presenceMask
            if (mask == null) {
                for (v in values) action(v.toDouble())
            } else {
                for (i in values.indices) if (mask[i]) action(values[i].toDouble())
            }
            return
        }
        longStorage?.let { values ->
            val mask = presenceMask
            if (mask == null) {
                for (v in values) action(v.toDouble())
            } else {
                for (i in values.indices) if (mask[i]) action(values[i].toDouble())
            }
            return
        }
        boolStorage?.let { values ->
            val mask = presenceMask
            if (mask == null) {
                for (v in values) action(if (v) 1.0 else 0.0)
            } else {
                for (i in values.indices) if (mask[i]) action(if (values[i]) 1.0 else 0.0)
            }
            return
        }
        for (v in data) {
            if (v == null) continue
            action((v as? Number)?.toDouble()
                ?: throw ClassCastException("列 '$name' 包含非数值类型: ${v::class.simpleName}"))
        }
    }

    internal fun sumNumeric(): Double {
        var sum = 0.0
        forEachNumeric { sum += it }
        return sum
    }

    internal fun countNumeric(): Int {
        var count = 0
        forEachNumeric { count++ }
        return count
    }

    internal fun maxNumeric(): Double {
        var found = false
        var max = Double.NEGATIVE_INFINITY
        forEachNumeric { v -> if (!found || v > max) { max = v; found = true } }
        return if (found) max else Double.NaN
    }

    internal fun minNumeric(): Double {
        var found = false
        var min = Double.POSITIVE_INFINITY
        forEachNumeric { v -> if (!found || v < min) { min = v; found = true } }
        return if (found) min else Double.NaN
    }

    internal fun nonNullCount(): Int {
        val mask = presenceMask
        if (mask != null) {
            var count = 0
            for (p in mask) if (p) count++
            return count
        }
        var count = 0
        for (v in data) if (v != null) count++
        return count
    }

    /** 第一个非空值是否为 Number；全空时按数值列处理（与原有行为一致）。 */
    internal fun isNumericColumn(): Boolean {
        for (v in data) {
            if (v == null) continue
            return v is Number
        }
        return true
    }

    /** 非空值所在的位置，用于按列过滤行。 */
    internal fun nonNullPositions(): IntArray {
        val mask = presenceMask
        if (mask != null) {
            var kept = 0
            for (p in mask) if (p) kept++
            val out = IntArray(kept)
            var k = 0
            for (i in mask.indices) if (mask[i]) out[k++] = i
            return out
        }
        var kept = 0
        for (v in data) if (v != null) kept++
        val out = IntArray(kept)
        var k = 0
        for (i in data.indices) if (data[i] != null) out[k++] = i
        return out
    }

    internal fun varianceNumeric(): Double {
        var count = 0L
        var mean = 0.0
        var m2 = 0.0
        forEachNumeric { v ->
            count++
            val delta = v - mean
            mean += delta / count
            m2 += delta * (v - mean)
        }
        return if (count > 1) m2 / (count - 1) else 0.0
    }

    /** 总体方差（ddof=0），用于 z-score 归一化。 */
    internal fun populationVarianceNumeric(): Double {
        var count = 0L
        var sum = 0.0
        var sumSq = 0.0
        forEachNumeric { v ->
            sum += v
            sumSq += v * v
            count++
        }
        if (count == 0L) return Double.NaN
        val mean = sum / count
        val variance = (sumSq / count) - mean * mean
        return if (variance > 0) variance else 0.0
    }

    /**
     * 转换为 DoubleArray（空值变为 NaN），用于原生向量运算。
     */
    internal fun toDoubleArrayNumeric(): DoubleArray {
        val out = DoubleArray(data.size)
        val mask = presenceMask
        doubleStorage?.let { values ->
            if (mask == null) {
                values.copyInto(out)
            } else {
                for (i in values.indices) out[i] = if (mask[i]) values[i] else Double.NaN
            }
            return out
        }
        intStorage?.let { values ->
            for (i in values.indices) out[i] = if (mask == null || mask[i]) values[i].toDouble() else Double.NaN
            return out
        }
        longStorage?.let { values ->
            for (i in values.indices) out[i] = if (mask == null || mask[i]) values[i].toDouble() else Double.NaN
            return out
        }
        boolStorage?.let { values ->
            for (i in values.indices) out[i] = if (mask == null || mask[i]) (if (values[i]) 1.0 else 0.0) else Double.NaN
            return out
        }
        for (i in data.indices) {
            val v = data[i]
            out[i] = if (v is Number) v.toDouble() else Double.NaN
        }
        return out
    }

    /**
     * 按位置抽取行，尽量复用列式原始数组。
     */
    internal fun take(positions: IntArray): Series<T> {
        val newIndex = ArrayList<Any>(positions.size)
        for (p in positions) newIndex.add(index[p])

        doubleStorage?.let { src ->
            val out = DoubleArray(positions.size)
            for (i in positions.indices) out[i] = src[positions[i]]
            val mask = copyMask(positions)
            @Suppress("UNCHECKED_CAST")
            return fromDoubleArray(out, mask, newIndex, name, dtype) as Series<T>
        }
        intStorage?.let { src ->
            val out = IntArray(positions.size)
            for (i in positions.indices) out[i] = src[positions[i]]
            val mask = copyMask(positions)
            @Suppress("UNCHECKED_CAST")
            return fromIntArray(out, mask, newIndex, name, dtype) as Series<T>
        }
        longStorage?.let { src ->
            val out = LongArray(positions.size)
            for (i in positions.indices) out[i] = src[positions[i]]
            val mask = copyMask(positions)
            @Suppress("UNCHECKED_CAST")
            return fromLongArray(out, mask, newIndex, name, dtype) as Series<T>
        }
        boolStorage?.let { src ->
            val out = BooleanArray(positions.size)
            for (i in positions.indices) out[i] = src[positions[i]]
            val mask = copyMask(positions)
            @Suppress("UNCHECKED_CAST")
            return fromBooleanArray(out, mask, newIndex, name, dtype) as Series<T>
        }

        val out = ArrayList<T?>(positions.size)
        for (p in positions) out.add(data[p])
        return Series(out, newIndex, name, dtype, false, false)
    }

    private fun copyMask(positions: IntArray): BooleanArray? {
        val mask = presenceMask ?: return null
        val out = BooleanArray(positions.size)
        for (i in positions.indices) out[i] = mask[positions[i]]
        return out
    }

    private fun sortedPositions(descending: Boolean): IntArray {
        val n = data.size
        val positions = Array(n) { it }
        val comparator = Comparator<Int> { a, b ->
            val va = data[a]
            val vb = data[b]
            // 空值始终排在最后，不受升/降序影响
            if (va == null) return@Comparator if (vb == null) 0 else 1
            if (vb == null) return@Comparator -1
            val result = when {
                va is Number && vb is Number -> va.toDouble().compareTo(vb.toDouble())
                else -> {
                    val ca = va as? Comparable<Any>
                    @Suppress("UNCHECKED_CAST")
                    if (ca != null && va::class == vb::class) {
                        ca.compareTo(vb as Any)
                    } else {
                        va.toString().compareTo(vb.toString())
                    }
                }
            }
            if (descending) -result else result
        }
        positions.sortWith(comparator)
        val out = IntArray(n)
        for (i in 0 until n) out[i] = positions[i]
        return out
    }

    companion object {
        private const val NATIVE_THRESHOLD = 256

        /** 内部快速包装：不复制数据、不做全量索引校验 */
        internal fun <T> wrap(
            data: List<T?>,
            index: List<Any>,
            name: String? = null,
            dtype: AndaTypes? = null
        ): Series<T> = Series(data, index, name, dtype, false, false)

        internal fun fromDoubleArray(
            values: DoubleArray,
            present: BooleanArray?,
            index: List<Any>,
            name: String?,
            dtype: AndaTypes?
        ): Series<Any> {
            val s = Series<Any>(DoubleColumnList(values, present), index, name, dtype, false, false)
            s.doubleStorage = values
            s.presenceMask = present
            return s
        }

        internal fun fromLongArray(
            values: LongArray,
            present: BooleanArray?,
            index: List<Any>,
            name: String?,
            dtype: AndaTypes?
        ): Series<Any> {
            val s = Series<Any>(LongColumnList(values, present), index, name, dtype, false, false)
            s.longStorage = values
            s.presenceMask = present
            return s
        }

        internal fun fromIntArray(
            values: IntArray,
            present: BooleanArray?,
            index: List<Any>,
            name: String?,
            dtype: AndaTypes?
        ): Series<Any> {
            val s = Series<Any>(IntColumnList(values, present), index, name, dtype, false, false)
            s.intStorage = values
            s.presenceMask = present
            return s
        }

        internal fun fromBooleanArray(
            values: BooleanArray,
            present: BooleanArray?,
            index: List<Any>,
            name: String?,
            dtype: AndaTypes?
        ): Series<Any> {
            val s = Series<Any>(BooleanColumnList(values, present), index, name, dtype, false, false)
            s.boolStorage = values
            s.presenceMask = present
            return s
        }

        /** 单个单元格的轻量 Series，用于行列访问，避免全量索引校验和复制 */
        internal fun single(value: Any?, name: String?): Series<Any> =
            Series(listOf(value), SINGLE_INDEX, name, null, false, false)

        private val SINGLE_INDEX: List<Any> = listOf(0)
    }
}

/** 仅包含 0..size-1 的虚拟索引，O(1) 内存，按需装箱。 */
internal class RangeIndexList(override val size: Int) : AbstractList<Any>() {
    override fun get(index: Int): Any = index

    override fun indexOf(element: Any?): Int {
        val i = element as? Int ?: return -1
        return if (i >= 0 && i < size) i else -1
    }

    override fun lastIndexOf(element: Any?): Int = indexOf(element)

    override fun contains(element: Any?): Boolean = indexOf(element) >= 0
}

internal class DoubleColumnList(
    private val values: DoubleArray,
    private val present: BooleanArray?
) : AbstractList<Any?>() {
    override val size: Int get() = values.size
    override fun get(index: Int): Any? = if (present == null || present[index]) values[index] else null
}

internal class LongColumnList(
    private val values: LongArray,
    private val present: BooleanArray?
) : AbstractList<Any?>() {
    override val size: Int get() = values.size
    override fun get(index: Int): Any? = if (present == null || present[index]) values[index] else null
}

internal class IntColumnList(
    private val values: IntArray,
    private val present: BooleanArray?
) : AbstractList<Any?>() {
    override val size: Int get() = values.size
    override fun get(index: Int): Any? = if (present == null || present[index]) values[index] else null
}

internal class BooleanColumnList(
    private val values: BooleanArray,
    private val present: BooleanArray?
) : AbstractList<Any?>() {
    override val size: Int get() = values.size
    override fun get(index: Int): Any? = if (present == null || present[index]) values[index] else null
}

/** 原生库可用性缓存（只探测一次）。 */
internal object NativeAvailability {
    val available: Boolean by lazy {
        try {
            NativeMath.sumDoubleArray(doubleArrayOf(1.0, 2.0, 3.0)) == 6.0
        } catch (t: Throwable) {
            false
        }
    }
}

// 移除main函数，避免编译错误
// SeriesTest.kt

fun main() {
    println("=== Series 功能测试 ===\n")

    // 1. 基本构造测试
    println("1. 基本构造测试:")
    val series1 = Series(listOf(1, 2, 3, 4, 5))
    println("默认索引Series:\n$series1\n")

    val series2 = Series(
        listOf(10, 20, 30, 40, 50),
        listOf("a", "b", "c", "d", "e"),
        "测试Series"
    )
    println("自定义索引Series:\n$series2\n")

    // 2. Map构造测试


    // 3. 数据访问测试
    println("3. 数据访问测试:")
    println("通过位置访问 series1[2]: ${series1[2]}")
    println("通过标签访问 series2[\"c\"]: ${series2["c"]}\n")

    // 4. 基本信息查询
    println("4. 基本信息查询:")
    println("series1.size(): ${series1.size()}")
    println("series1.shape(): ${series1.shape()}")
    println("series1.dtype(): ${series1.dtype()}")
    println("series1.name(): ${series1.name()}")
    println("series1.index(): ${series1.index()}")
    println("series1.values(): ${series1.values()}\n")

    // 5. 数据操作测试
    println("5. 数据操作测试:")
    println("head(3):\n${series1.head(3)}\n")
    println("tail(3):\n${series1.tail(3)}\n")

    // 6. 空值处理测试
    println("6. 空值处理测试:")
    val seriesWithNull = Series(listOf(1, null, 3, null, 5), listOf("a", "b", "c", "d", "e"))
    println("含空值Series:\n$seriesWithNull\n")
    println("isnull():\n${seriesWithNull.isnull()}\n")
    println("notnull():\n${seriesWithNull.notnull()}\n")
    println("dropna():\n${seriesWithNull.dropna()}\n")
    println("fillna(0):\n${seriesWithNull.fillna(0)}\n")

    // 7. 统计功能测试
    println("7. 统计功能测试:")
    val stringSeries = Series(listOf("apple", "banana", "apple", "cherry", "banana"))
    println("字符串Series:\n$stringSeries\n")
    println("unique(): ${stringSeries.unique()}")
    println("valueCounts(): ${stringSeries.valueCounts()}\n")

    // 8. 数据变换测试
    println("8. 数据变换测试:")
    println( " *2 :\n${series1*2}\n")
    println("map { it * 2 }:\n${series1.map { it?.times(2) }}\n")
    println("filter { it > 3 }:\n${series1.filter { it?.compareTo(3) ?: -1 > 0 }}\n")

    // 9. 排序测试
    println("9. 排序测试:")
    val unsortedSeries = Series(
        listOf(3, 1, 4, 2, 5),
        listOf("c", "a", "d", "b", "e")
    )
    println("原始Series:\n$unsortedSeries\n")
    println("sortIndex():\n${unsortedSeries.sortIndex()}\n")
    println("sortValues():\n${unsortedSeries.sortValues()}\n")

    // 10. 数值计算测试
    println("10. 数值计算测试:")
    val numericSeries = Series(listOf(1.0, 2.0, 3.0, 4.0, 5.0))
    println("数值Series:\n$numericSeries\n")
    println("cumsum():\n${numericSeries.cumsum()}\n")

    // 11. 格式转换测试
    println("11. 格式转换测试:")
    println("toList(): ${series1.toList()}")
    println("toMap(): ${series2.toMap()}\n")

    println("=== 所有功能测试完成 ===")
}
