package cn.ac.oac.libs.andas.entity

import android.util.Log
import cn.ac.oac.libs.andas.types.AndaTypes
import cn.ac.oac.libs.andas.types.typeName
import cn.ac.oac.libs.andas.core.NativeMath
import cn.ac.oac.libs.andas.core.NativeData
import cn.ac.oac.libs.andas.core.NativeBatch
import java.util.*

/**
 * Series 类似于一维数组，可以存储任何数据类型，并通过标签（索引）访问元素
 * 它是 Andas（Android Pandas）库中的核心数据结构之一
 *
 * @param T 数据类型
 */
class Series<T> {
    private var data: List<T?>
    private var index: List<Any> // 只能是 Int 或 String
    private var name: String?
    private var dtype: AndaTypes?
    // 添加索引到位置的映射，用于快速查找
    private var indexToPosition: Map<Any, Int>? = null

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
    ) {
        this.data = data.toList()
        val rawIndex = index ?: (0 until data.size).toList()
        
        // 验证索引类型：只能是Int或String
        validateIndexType(rawIndex)
        
        this.index = rawIndex
        this.name = name
        this.dtype = dtype ?: if (data.isNotEmpty()) guessDtype(data.first()) else null
        
        // 验证索引和数据长度是否一致
        if (this.index.size != this.data.size) {
            throw IllegalArgumentException("Index和Data的长度必须一致")
        }

        this.indexToPosition = buildIndexToPositionMap()
    }

    /**
     * 构造函数 - 通过Map创建Series
     *
     * @param map 键值对映射，键作为索引，值作为数据，索引只能是Int或String
     * @param name Series名称
     */
    constructor(
        map: Map<Any, T?>,
        name: String? = null
    ) {
        this.data = map.values.toList()
        val rawIndex = map.keys.toList()
        
        // 验证索引类型：只能是Int或String
        validateIndexType(rawIndex)
        
        this.index = rawIndex
        this.name = name
        this.dtype = if (!this.data.isEmpty() && this.data.first() != null) guessDtype(this.data.first()) else null
        this.indexToPosition = buildIndexToPositionMap()
    }
    
    /**
     * 验证索引类型：只能是Int或String
     */
    private fun validateIndexType(indexList: List<Any>) {
        for (item in indexList) {
            when (item) {
                is Int, is String -> return
                else -> throw IllegalArgumentException("索引类型必须是Int或String，但得到了: ${item::class.simpleName}")
            }
        }
    }
    
    /**
     * 构建索引到位置的映射表
     */
    private fun buildIndexToPositionMap(): Map<Any, Int> {
        val map = mutableMapOf<Any, Int>()
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
        val newData = data ?: this.data
        val newIndex = index ?: this.index
        val newSeries = Series(newData, newIndex, name ?: this.name, dtype ?: this.dtype)
        return newSeries
    }


    /**
     * 通过标签索引获取元素
     *
     * @param label 标签索引
     * @return 对应标签的元素
     */
    operator fun get(label: String): T? {
        // label: Any 只能是 Int 和 String
        val position = indexToPosition?.get(label)
        if (position == null) {
            throw NoSuchElementException("未找到索引: $label")
        }
        return data[position]
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
        val actualN = kotlin.math.min(n, data.size)
        return Series(
            data.take(actualN),
            index.take(actualN),
            name,
            dtype
        )
    }

    /**
     * 获取后n个元素
     *
     * @param n 元素个数，默认为5
     * @return 包含后n个元素的新Series
     */
    fun tail(n: Int = 5): Series<T> {
        val actualN = kotlin.math.min(n, data.size)
        return Series(
            data.takeLast(actualN),
            index.takeLast(actualN),
            name,
            dtype
        )
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
        val sortedPairs = index.zip(data).sortedBy { it.first.toString() }
        val sortedIndex = sortedPairs.map { it.first }
        val sortedData = sortedPairs.map { it.second }
        return Series(sortedData, sortedIndex, name, dtype)
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
        val cleanData = mutableListOf<T?>()
        val cleanIndex = mutableListOf<Any>()
        
        for (i in data.indices) {
            if (data[i] != null) {
                cleanData.add(data[i])
                cleanIndex.add(index[i])
            }
        }
        
        return Series(cleanData, cleanIndex, name, dtype)
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
    fun sumKt(): Double {
        if (data.isEmpty()) return 0.0

        val doubleArray = data
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()

        var sum = 0.0;
        for (d in doubleArray){
            sum=sum+d
        }


        return sum
    }
    /**
     * 使用原生方法计算和（高性能）
     * 仅适用于数值类型的Series
     */
    fun sum(): Double {
        if (data.isEmpty()) return 0.0
        
        val doubleArray = data
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        return NativeMath.sumDoubleArray(doubleArray)
    }
    
    /**
     * 使用原生方法计算均值（高性能）
     * 仅适用于数值类型的Series
     */
    fun mean(): Double {
        if (data.isEmpty()) return 0.0
        
        val doubleArray = data
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        return NativeMath.meanDoubleArray(doubleArray)
    }
    
    /**
     * 使用原生方法计算最大值（高性能）
     * 仅适用于数值类型的Series
     */
    fun max(): Double {
        if (data.isEmpty()) return 0.0
        
        val doubleArray = data
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        return NativeMath.maxDoubleArray(doubleArray)
    }
    
    /**
     * 使用原生方法计算最小值（高性能）
     * 仅适用于数值类型的Series
     */
    fun min(): Double {
        if (data.isEmpty()) return 0.0
        
        val doubleArray = data
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        return NativeMath.minDoubleArray(doubleArray)
    }
    
    /**
     * 使用原生方法计算方差（高性能）
     * 仅适用于数值类型的Series
     */
    fun variance(): Double {
        if (data.isEmpty()) return 0.0
        
        val doubleArray = data
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        return NativeMath.variance(doubleArray)
    }
    
    /**
     * 使用原生方法计算标准差（高性能）
     * 仅适用于数值类型的Series
     */
    fun std(): Double {
        if (data.isEmpty()) return 0.0
        
        val doubleArray = data
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        return NativeMath.std(doubleArray)
    }
    
    /**
     * 使用原生方法进行归一化（高性能）
     * 仅适用于数值类型的Series
     */
    fun normalize(): Series<Double> {
        if (data.isEmpty()) {
            return Series(emptyList(), index, if (name != null) "${name}_normalized" else null)
        }
        
        val doubleArray = data
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val normalized = NativeMath.normalize(doubleArray)
        
        return Series(normalized.toList(), index, if (name != null) "${name}_normalized" else null)
    }
    
    /**
     * 使用原生方法进行排序（高性能）
     * 仅适用于数值类型的Series
     */
    fun sortValuesNative(descending: Boolean = false): Series<T> {
        if (data.isEmpty()) return this
        
        val doubleArray = data
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val indices = if (descending) {
            NativeMath.argsort(doubleArray).reversed()
        } else {
            NativeMath.argsort(doubleArray).toList()
        }
        
        val sortedData = indices.map { data[it] }
        val sortedIndex = indices.map { index[it] }
        
        return Series(sortedData, sortedIndex, name, dtype)
    }
    
    /**
     * 使用原生方法进行布尔筛选（高性能）
     * 仅适用于数值类型的Series
     */
    fun filterGreaterThan(threshold: Double): Series<T> {
        if (data.isEmpty()) return this
        
        val doubleArray = data
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val mask = NativeMath.greaterThan(doubleArray, threshold)
        val indices = NativeData.where(mask)
        
        val filteredData = indices.map { data[it.toInt()] }
        val filteredIndex = indices.map { index[it.toInt()] }
        
        return Series(filteredData, filteredIndex, name, dtype)
    }
    
    /**
     * 使用原生方法查找空值索引（高性能）
     */
    fun findNullIndices(): List<Int> {
        val doubleArray = data.map { 
            if (it == null) Double.NaN 
            else (it as Number).toDouble() 
        }.toDoubleArray()
        
        return NativeData.findNullIndices(doubleArray).map { it.toInt() }
    }
    
    /**
     * 使用原生方法丢弃空值（高性能）
     */
    fun dropNullValues(): Series<T> {
        val doubleArray = data.map { 
            if (it == null) Double.NaN 
            else (it as Number).toDouble() 
        }.toDoubleArray()
        
        val result = NativeData.dropNullValues(doubleArray)
        
        // 找到非空值的原始索引
        val nonNullIndices = mutableListOf<Int>()
        for (i in data.indices) {
            if (data[i] != null) {
                nonNullIndices.add(i)
            }
        }
        
        val cleanData = nonNullIndices.map { data[it] }
        val cleanIndex = nonNullIndices.map { index[it] }
        
        return Series(cleanData, cleanIndex, name, dtype)
    }
    
    /**
     * 使用原生方法填充空值（高性能）
     */
    fun fillNullWithConstant(value: Double): Series<Double> {
        val doubleArray = data.map { 
            if (it == null) Double.NaN 
            else (it as Number).toDouble() 
        }.toDoubleArray()
        
        val result = NativeData.fillNullWithConstant(doubleArray, value)
        
        return Series(result.toList(), index, if (name != null) "${name}_filled" else null)
    }
    
    /**
     * 使用原生方法进行排序索引（高性能）
     */
    fun sortIndices(descending: Boolean = false): List<Int> {
        if (data.isEmpty()) return emptyList()
        
        val doubleArray = data
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        return NativeData.sortIndices(doubleArray, descending).toList()
    }
    
    /**
     * 使用原生方法进行统计描述（高性能）
     * 仅适用于数值类型的Series
     */
    fun describe(): Map<String, Double> {
        if (data.isEmpty()) {
            return mapOf(
                "count" to 0.0,
                "mean" to 0.0,
                "std" to 0.0,
                "min" to 0.0,
                "max" to 0.0
            )
        }
        
        val doubleArray = data
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val result = NativeData.describe(doubleArray)
        
        return mapOf(
            "count" to result[0],
            "mean" to result[1],
            "std" to result[2],
            "min" to result[3],
            "max" to result[4]
        )
    }
    
    /**
     * 使用原生方法进行数据采样（高性能）
     */
    fun sample(sampleSize: Int): Series<T> {
        if (data.isEmpty() || sampleSize <= 0) {
            return Series(emptyList(), emptyList(), name, dtype)
        }
        
        if (sampleSize >= data.size) {
            return this.copy()
        }
        
        val doubleArray = data.map { 
            if (it == null) Double.NaN 
            else (it as Number).toDouble() 
        }.toDoubleArray()
        
        val sampleIndices = NativeData.sample(doubleArray, sampleSize).map { it.toInt() }
        
        val sampledData = sampleIndices.map { data[it] }
        val sampledIndex = sampleIndices.map { index[it] }
        
        return Series(sampledData, sampledIndex, name, dtype)
    }
    
    /**
     * 使用原生方法批量处理（高性能）
     */
    fun processBatch(batchSize: Int = 1000): Series<Double> {
        if (data.isEmpty()) {
            return Series(emptyList(), index, if (name != null) "${name}_processed" else null)
        }
        
        val doubleArray = data
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val result = NativeBatch.processBatch(doubleArray, batchSize)
        
        return Series(result.map { it as Double? }, index, if (name != null) "${name}_processed" else null)
    }
    
    /**
     * 检查原生库是否可用
     */
    fun isNativeAvailable(): Boolean {
        return try {
            NativeMath.isAvailable() && NativeData.isAvailable()
        } catch (e: Exception) {
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
