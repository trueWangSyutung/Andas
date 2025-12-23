package cn.ac.oac.libs.andas.entity

import cn.ac.oac.libs.andas.types.AndaTypes
import cn.ac.oac.libs.andas.core.NativeMath
import cn.ac.oac.libs.andas.core.NativeData
import cn.ac.oac.libs.andas.core.NativeBatch
import java.io.File
import java.io.FileWriter
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.collections.sorted

/**
 * DataFrame 类似于二维表格，是Andas库的核心数据结构
 * 提供类似pandas.DataFrame的数据操作接口
 */
class DataFrame {
    private val data: MutableMap<String, Series<Any>> = mutableMapOf()
    private var columns: List<String> = listOf()
    private var index: List<Any> = listOf()
    
    /**
     * 通过列数据构造DataFrame
     */
    constructor(columnsData: Map<String, List<Any?>>) {
        if (columnsData.isEmpty()) {
            return
        }
        
        // 验证所有列长度一致
        val firstSize = columnsData.values.first().size
        columnsData.values.forEach { 
            if (it.size != firstSize) {
                throw IllegalArgumentException("所有列的长度必须一致")
            }
        }
        
        this.columns = columnsData.keys.toList()
        this.index = (0 until firstSize).toList()
        
        columnsData.forEach { (colName, colData) ->
            data[colName] = Series(colData, index, colName)
        }
    }
    
    /**
     * 通过行数据构造DataFrame
     */
    constructor(rowsData: List<Map<String, Any?>>) {
        if (rowsData.isEmpty()) {
            return
        }
        
        // 提取所有列名
        val allColumns = mutableSetOf<String>()
        rowsData.forEach { row ->
            allColumns.addAll(row.keys)
        }
        
        this.columns = allColumns.toList()
        this.index = rowsData.indices.map { it }
        
        // 转换为列数据
        columns.forEach { colName ->
            val colData = rowsData.map { row -> row[colName] }
            data[colName] = Series(colData, index, colName)
        }
    }
    
    /**
     * 私有构造函数 - 用于内部创建DataFrame
     */
    private constructor(
        data: Map<String, Series<Any>>,
        columns: List<String>,
        index: List<Any>
    ) {
        this.data.putAll(data)
        this.columns = columns
        this.index = index
    }

    /**
     * 获取形状 (行数, 列数)
     */
    fun shape(): Pair<Int, Int> = index.size to columns.size
    
    /**
     * 获取列名列表
     */
    fun columns(): List<String> = columns.toList()
    
    /**
     * 获取索引列表
     */
    fun index(): List<Any> = index.toList()
    
    /**
     * 获取数据类型信息
     */
    fun dtypes(): Map<String, AndaTypes?> {
        return columns.associateWith { colName ->
            data[colName]?.dtype()
        }
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
    
    /**
     * 性能基准测试
     */
    fun benchmarkOperation(operationType: Int, dataSize: Int): Long {
        return NativeMath.Benchmark.measureOperationTime(operationType, dataSize)
    }
    
    /**
     * 估算内存使用量（简化估算）
     */
    private fun estimateMemoryUsage(): String {
        var totalBytes = 0L
        
        columns.forEach { colName ->
            val series = data[colName]!!
            val values = series.values()
            
            values.forEach { value ->
                when (value) {
                    is Int -> totalBytes += 4
                    is Long -> totalBytes += 8
                    is Double -> totalBytes += 8
                    is Boolean -> totalBytes += 1
                    is String -> totalBytes += value.length * 2
                    else -> totalBytes += 8
                }
            }
        }
        
        // 格式化为易读的单位
        return when {
            totalBytes < 1024 -> "$totalBytes B"
            totalBytes < 1024 * 1024 -> "${"%.2f".format(totalBytes / 1024.0)} KB"
            totalBytes < 1024 * 1024 * 1024 -> "${"%.2f".format(totalBytes / (1024.0 * 1024))} MB"
            else -> "${"%.2f".format(totalBytes / (1024.0 * 1024 * 1024))} GB"
        }
    }
    
    /**
     * 计算 DataFrame 列之间的相关系数矩阵
     */
    fun corr(): DataFrame {
        if (columns.isEmpty()) {
            return DataFrame(emptyList<Map<String, Any?>>())
        }
        
        // 只考虑数值类型的列
        val numericColumns = columns.filter { colName ->
            val series = data[colName]!!
            val values = series.values().filterNotNull()
            values.isEmpty() || values.first() is Number
        }
        
        if (numericColumns.size < 2) {
            throw IllegalArgumentException("需要至少两个数值列来计算相关系数")
        }
        
        // 计算相关系数矩阵
        val corrData = mutableMapOf<String, List<Double>>()
        
        for (col1 in numericColumns) {
            val rowValues = mutableListOf<Double>()
            for (col2 in numericColumns) {
                val corr = calculateCorrelation(col1, col2)
                rowValues.add(corr)
            }
            corrData[col1] = rowValues
        }
        
        // 创建相关系数矩阵 DataFrame
        val corrDF = DataFrame(corrData)
        // 重命名行索引为列名
        val renamedDF = DataFrame(
            numericColumns.map { rowName ->
                numericColumns.mapIndexed { i, colName -> 
                    colName to corrData[rowName]!![i] 
                }.toMap()
            }
        )
        
        return renamedDF
    }
    
    /**
     * 计算两列之间的相关系数
     */
    private fun calculateCorrelation(col1: String, col2: String): Double {
        val series1 = data[col1]!!
        val series2 = data[col2]!!
        
        val values1 = series1.values().filterNotNull().map { (it as Number).toDouble() }
        val values2 = series2.values().filterNotNull().map { (it as Number).toDouble() }
        
        if (values1.size != values2.size || values1.isEmpty()) {
            return Double.NaN
        }
        
        // 计算皮尔逊相关系数
        val n = values1.size
        val mean1 = values1.sum() / n
        val mean2 = values2.sum() / n
        
        var numerator = 0.0
        var sumSq1 = 0.0
        var sumSq2 = 0.0
        
        for (i in 0 until n) {
            val diff1 = values1[i] - mean1
            val diff2 = values2[i] - mean2
            numerator += diff1 * diff2
            sumSq1 += diff1 * diff1
            sumSq2 += diff2 * diff2
        }
        
        val denominator = kotlin.math.sqrt(sumSq1 * sumSq2)
        
        return if (denominator == 0.0) 0.0 else numerator / denominator
    }
    
    /**
     * 获取前n行
     */
    fun head(n: Int = 5): DataFrame {
        val actualN = kotlin.math.min(n, index.size)
        val newIndex = index.take(actualN)
        val newData = columns.associate { colName ->
            colName to data[colName]!!.head(actualN)
        }
        return DataFrame(newData, columns, newIndex)
    }
    
    /**
     * 获取后n行
     */
    fun tail(n: Int = 5): DataFrame {
        val actualN = kotlin.math.min(n, index.size)
        val newIndex = index.takeLast(actualN)
        val newData = columns.associate { colName ->
            colName to data[colName]!!.tail(actualN)
        }
        return DataFrame(newData, columns, newIndex)
    }
    
    /**
     * 选择指定列
     */
    fun selectColumns(vararg colNames: String): DataFrame {
        val invalidCols = colNames.filter { it !in columns }
        if (invalidCols.isNotEmpty()) {
            throw IllegalArgumentException("不存在的列: $invalidCols")
        }
        
        val newData = colNames.associate { colName ->
            colName to data[colName]!!
        }
        return DataFrame(newData, colNames.toList(), index)
    }
    
    /**
     * 通过列名获取列数据
     */
    operator fun get(key: String): Series<Any> {
        if (key !in columns) {
            throw IllegalArgumentException("列不存在: $key")
        }
        return data[key]!!
    }

    /**
     * 通过列名列表获取多列数据
     */
    operator fun get(keys: List<String>): DataFrame {
        val invalidCols = keys.filter { it !in columns }
        if (invalidCols.isNotEmpty()) {
            throw IllegalArgumentException("不存在的列: $invalidCols")
        }
        
        val newData = keys.associate { colName ->
            colName to data[colName]!!
        }
        return DataFrame(newData, keys, index)
    }

    /**
     * 通过行索引获取行数据
     */
    operator fun get(key: Int): Map<String, Series<Any>?> {
        return getRow(key)
    }
    
    /**
     * 设置列数据
     */
    operator fun set(key: String, value: Series<Any>) {
        if (value.size() != index.size) {
            throw IllegalArgumentException("新列长度必须与现有行数一致，当前: ${value.size()}, 需要: ${index.size}")
        }
        data[key] = value
        if (key !in columns) {
            columns = columns + key
        }
    }
    
    /**
     * 设置行数据
     */
    operator fun set(key: Int, value: Map<String, Any?>) {
        if (key < 0 || key >= index.size) {
            throw IndexOutOfBoundsException("行索引超出范围: $key")
        }
        
        // 更新指定行的数据
        value.forEach { (colName, cellValue) ->
            if (colName in columns) {
                // 获取当前列的所有值
                val currentValues = data[colName]!!.values().toMutableList()
                // 更新指定位置的值
                currentValues[key] = cellValue
                // 创建新的 Series
                data[colName] = Series(currentValues, index, colName)
            } else {
                // 如果列不存在，创建新列（用 null 填充之前的行）
                val newValues = List(index.size) { i -> if (i == key) cellValue else null }
                data[colName] = Series(newValues, index, colName)
                columns = columns + colName
            }
        }
    }

    /**
     * 按条件筛选行
     */
    fun filter(predicate: (Map<String, Series<Any>?>) -> Boolean): DataFrame {
        val filteredData = mutableListOf<Map<String, Any?>>()
        val filteredIndex = mutableListOf<Any>()
        
        for (i in index.indices) {
            val row = getRow(i)
            if (predicate(row)) {
                // 将 Series<Any> 转换回 Map<String, Any?> 用于创建新 DataFrame
                val rowValues = row.mapValues { (_, series) -> series?.get(0) }
                filteredData.add(rowValues)
                filteredIndex.add(index[i])
            }
        }
        
        return DataFrame(filteredData)
    }
    
    /**
     * 获取指定行 - 返回 Map<String, Series<Any>?>
     */
    fun getRow(rowIndex: Int): Map<String, Series<Any>?> {
        if (rowIndex < 0 || rowIndex >= index.size) {
            throw IndexOutOfBoundsException("行索引超出范围: $rowIndex")
        }
        
        return columns.associate { colName ->
            // 为每个单元格创建一个只包含单个元素的 Series
            val cellValue = data[colName]!![rowIndex]
            val cellSeries = if (cellValue != null) {
                Series(listOf(cellValue), listOf(0), colName)
            } else {
                null
            }
            colName to cellSeries
        }
    }
    
    /**
     * 按标签获取行 - 返回 Map<String, Series<Any>?>
     */
    fun loc(label: Any): Map<String, Series<Any>?> {
        val position = index.indexOf(label)
        if (position == -1) {
            throw NoSuchElementException("未找到索引: $label")
        }
        return getRow(position)
    }
    
    /**
     * 按位置获取行 - 返回 Map<String, Series<Any>?>
     */
    fun iloc(position: Int): Map<String, Series<Any>?> = getRow(position)
    
    /**
     * 获取指定单元格值
     */
    fun at(rowIndex: Int, colName: String): Any? {
        return data[colName]?.get(rowIndex)
    }
    
    /**
     * 按标签获取单元格值
     */
    fun at(label: Any, colName: String): Any? {
        val position = index.indexOf(label)
        if (position == -1) {
            throw NoSuchElementException("未找到索引: $label")
        }
        return data[colName]?.get(position)
    }
    
    /**
     * 检查空值
     */
    fun isnull(): DataFrame {
        val newData = mutableMapOf<String, Series<Any>>()
        for (colName in columns) {
            val boolSeries = data[colName]!!.isnull()
            // 转换为Series<Any>以兼容DataFrame
            val anySeries = Series(
                boolSeries.values().map { it as Any? },
                boolSeries.index(),
                boolSeries.name()
            )
            newData[colName] = anySeries
        }
        return DataFrame(newData, columns, index)
    }
    
    /**
     * 检查非空值
     */
    fun notnull(): DataFrame {
        val newData = mutableMapOf<String, Series<Any>>()
        for (colName in columns) {
            val boolSeries = data[colName]!!.notnull()
            // 转换为Series<Any>以兼容DataFrame
            val anySeries = Series(
                boolSeries.values().map { it as Any? },
                boolSeries.index(),
                boolSeries.name()
            )
            newData[colName] = anySeries
        }
        return DataFrame(newData, columns, index)
    }
    
    /**
     * 丢弃包含空值的行
     */
    fun dropna(): DataFrame {
        val rowsToKeep = mutableListOf<Int>()
        
        for (i in index.indices) {
            var hasNull = false
            for (colName in columns) {
                if (data[colName]!![i] == null) {
                    hasNull = true
                    break
                }
            }
            if (!hasNull) {
                rowsToKeep.add(i)
            }
        }
        
        val filteredData = rowsToKeep.map { i ->
            columns.associate { colName -> colName to data[colName]!![i] }
        }
        
        return DataFrame(filteredData)
    }
    
    /**
     * 填充空值
     */
    fun fillna(value: Any): DataFrame {
        val newData = columns.associate { colName ->
            colName to data[colName]!!.fillna(value)
        }
        return DataFrame(newData, columns, index)
    }
    
    /**
     * 按列分组
     */
    fun groupBy(vararg groupCols: String): GroupBy {
        return GroupBy(this, groupCols.toList())
    }
    
    /**
     * 聚合操作
     */
    fun agg(operations: Map<String, String>): DataFrame {
        val resultData = mutableMapOf<String, List<Any?>>()
        val resultIndex = mutableListOf<String>()
        
        // 执行所有操作
        operations.forEach { (opName, op) ->
            resultIndex.add(opName)
        }
        
        columns.forEach { colName ->
            val series = data[colName]!!
            val colResults = mutableListOf<Any?>()
            
            operations.forEach { (opName, op) ->
                val value = when (op.lowercase()) {
                    "sum" -> {
                        val values = series.values().filterNotNull()
                        // 检查是否为数值类型
                        if (values.isEmpty()) {
                            null
                        } else if (values.first() is Number) {
                            values.sumOf { (it as Number).toDouble() }
                        } else {
                            null // 非数值类型不支持sum
                        }
                    }
                    "mean" -> {
                        val values = series.values().filterNotNull()
                        if (values.isEmpty() || values.first() !is Number) {
                            null
                        } else {
                            values.sumOf { (it as Number).toDouble() } / values.size
                        }
                    }
                    "min" -> {
                        val values = series.values().filterNotNull()
                        if (values.isEmpty()) {
                            null
                        } else if (values.first() is Number) {
                            values.minOfOrNull { (it as Number).toDouble() }
                        } else {
                            values.minOfOrNull { it.toString() }
                        }
                    }
                    "max" -> {
                        val values = series.values().filterNotNull()
                        if (values.isEmpty()) {
                            null
                        } else if (values.first() is Number) {
                            values.maxOfOrNull { (it as Number).toDouble() }
                        } else {
                            values.maxOfOrNull { it.toString() }
                        }
                    }
                    "count" -> series.values().count { it != null }
                    else -> throw IllegalArgumentException("不支持的聚合操作: $op")
                }
                colResults.add(value)
            }
            
            resultData[colName] = colResults
        }
        
        return DataFrame(resultData)
    }
    
    /**
     * 重命名列
     */
    fun rename(columnsMap: Map<String, String>): DataFrame {
        val newColumns = columns.map { col ->
            columnsMap[col] ?: col
        }
        
        val newData = columns.associate { colName ->
            val newName = columnsMap[colName] ?: colName
            newName to data[colName]!!.copy(name = newName)
        }
        
        return DataFrame(newData, newColumns, index)
    }
    
    /**
     * 添加列
     */
    fun addColumn(name: String, data: List<Any?>): DataFrame {
        if (data.size != index.size) {
            throw IllegalArgumentException("新列长度必须与现有行数一致")
        }
        
        val newData = this.data.toMutableMap()
        newData[name] = Series(data, index, name)
        
        val newColumns = if (name in columns) columns else columns + name
        
        return DataFrame(newData, newColumns, index)
    }

    /**
     * 添加列（通过转换函数）
     */
    fun addColumn(name: String, transform: (Map<String, Any?>) -> Any?): DataFrame {
        val newData = this.data.toMutableMap()
        val newColumnData = this.index.map { rowIndex ->
            val row = this.columns.associate { colName ->
                colName to this.data[colName]?.get(rowIndex)
            }
            transform(row)
        }
        newData[name] = Series(newColumnData, this.index, name)

        val newColumns = if (name in columns) columns else columns + name
        return DataFrame(newData, newColumns, index)
    }

    /**
     * 删除列
     */
    fun dropColumns(vararg colNames: String): DataFrame {
        val invalidCols = colNames.filter { it !in columns }
        if (invalidCols.isNotEmpty()) {
            throw IllegalArgumentException("不存在的列: $invalidCols")
        }
        
        val newColumns = columns.filter { it !in colNames }
        val newData = data.filterKeys { it !in colNames }
        
        return DataFrame(newData, newColumns, index)
    }
    
    /**
     * 复制DataFrame，可选修改属性
     */
    fun copy(
        data: Map<String, Series<Any>>? = null,
        columns: List<String>? = null,
        index: List<Any>? = null
    ): DataFrame {
        return DataFrame(
            data ?: this.data,
            columns ?: this.columns,
            index ?: this.index
        )
    }
    
    /**
     * 转换为字符串表示
     */
    override fun toString(): String {
        if (columns.isEmpty()) {
            return "Empty DataFrame"
        }
        
        val builder = StringBuilder()
        val maxRows = kotlin.math.min(10, index.size)
        
        // 表头
        builder.append(columns.joinToString("\t"))
        builder.append("\n")
        
        // 数据行
        for (i in 0 until maxRows) {
            val row = columns.map { colName ->
                data[colName]!![i]?.toString() ?: "null"
            }
            builder.append(row.joinToString("\t"))
            builder.append("\n")
        }
        
        if (index.size > maxRows) {
            builder.append("...\n")
        }
        
        builder.append("Shape: ${shape()}")
        return builder.toString()
    }
    
    // ==================== JNI 原生高性能操作 ====================
    
    /**
     * 计算指定数值列的和 - 优先使用原生方法
     */
    fun sum(colName: String): Double {
        if (isNativeAvailable()) {
            return sumNative(colName)
        }
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        var sum = 0.0
        doubleArray.forEach {
            sum = sum + it
        }
        return sum
    }
    
    /**
     * 使用原生方法计算指定数值列的和（高性能）
     */
    private fun sumNative(colName: String): Double {
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        return NativeMath.sumDoubleArray(doubleArray)
    }
    
    /**
     * 计算指定数值列的均值 - 优先使用原生方法
     */
    fun mean(colName: String): Double {
        if (isNativeAvailable()) {
            return meanNative(colName)
        }
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        if (doubleArray.isEmpty()) return Double.NaN
        
        return doubleArray.sum() / doubleArray.size
    }
    
    /**
     * 使用原生方法计算指定数值列的均值（高性能）
     */
    private fun meanNative(colName: String): Double {
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        return NativeMath.meanDoubleArray(doubleArray)
    }
    
    /**
     * 计算指定数值列的最大值 - 优先使用原生方法
     */
    fun max(colName: String): Double {
        if (isNativeAvailable()) {
            return maxNative(colName)
        }
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        if (doubleArray.isEmpty()) return Double.NaN
        
        return doubleArray.maxOrNull() ?: Double.NaN
    }
    
    /**
     * 使用原生方法计算指定数值列的最大值（高性能）
     */
    private fun maxNative(colName: String): Double {
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        return NativeMath.maxDoubleArray(doubleArray)
    }
    
    /**
     * 计算指定数值列的最小值 - 优先使用原生方法
     */
    fun min(colName: String): Double {
        if (isNativeAvailable()) {
            return minNative(colName)
        }
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        if (doubleArray.isEmpty()) return Double.NaN
        
        return doubleArray.minOrNull() ?: Double.NaN
    }
    
    /**
     * 使用原生方法计算指定数值列的最小值（高性能）
     */
    private fun minNative(colName: String): Double {
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        return NativeMath.minDoubleArray(doubleArray)
    }
    
    /**
     * 计算指定数值列的方差 - 优先使用原生方法
     */
    fun variance(colName: String): Double {
        if (isNativeAvailable()) {
            return varianceNative(colName)
        }
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        if (doubleArray.size < 2) return Double.NaN
        
        val mean = doubleArray.sum() / doubleArray.size
        val sumSquaredDiff = doubleArray.sumOf { (it - mean) * (it - mean) }
        return sumSquaredDiff / (doubleArray.size - 1)
    }
    
    /**
     * 使用原生方法计算指定数值列的方差（高性能）
     */
    private fun varianceNative(colName: String): Double {
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        return NativeMath.variance(doubleArray)
    }
    
    /**
     * 计算指定数值列的标准差 - 优先使用原生方法
     */
    fun std(colName: String): Double {
        if (isNativeAvailable()) {
            return stdNative(colName)
        }
        val variance = variance(colName)
        return kotlin.math.sqrt(variance)
    }
    
    /**
     * 使用原生方法计算指定数值列的标准差（高性能）
     */
    private fun stdNative(colName: String): Double {
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        return NativeMath.std(doubleArray)
    }
    
    /**
     * 对指定数值列进行标准归一化 - 优先使用原生方法
     */
    fun normalize(colName: String): DataFrame {
        if (isNativeAvailable()) {
            return normalizeNative(colName)
        }
        
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        if (doubleArray.isEmpty()) return this
        
        val mean = doubleArray.sum() / doubleArray.size
        val std = kotlin.math.sqrt(doubleArray.sumOf { (it - mean) * (it - mean) } / doubleArray.size)
        
        if (std == 0.0) return this
        
        val normalized = doubleArray.map { (it - mean) / std }
        val newValues = normalized.map { it as Any? }
        val newSeries = Series(newValues, index, colName)
        
        val newData = data.toMutableMap()
        newData[colName] = newSeries
        
        return DataFrame(newData, columns, index)
    }
    
    /**
     * 使用原生方法对指定数值列进行归一化（高性能）
     */
    private fun normalizeNative(colName: String): DataFrame {
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val normalized = NativeMath.normalize(doubleArray)
        
        val newValues = normalized.map { it as Any? }
        val newSeries = Series(newValues, index, colName)
        
        val newData = data.toMutableMap()
        newData[colName] = newSeries
        
        return DataFrame(newData, columns, index)
    }
    
    /**
     * 向量化加法 - 优先使用原生方法
     */
    fun vectorizedAdd(col1: String, col2: String, resultCol: String): DataFrame {
        if (isNativeAvailable()) {
            return vectorizedAddNative(col1, col2, resultCol)
        }
        
        val series1 = data[col1] ?: throw IllegalArgumentException("列不存在: $col1")
        val series2 = data[col2] ?: throw IllegalArgumentException("列不存在: $col2")
        
        val array1 = series1.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        val array2 = series2.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (array1.size != array2.size) {
            throw IllegalArgumentException("列长度不匹配: $col1 (${array1.size}) vs $col2 (${array2.size})")
        }
        
        val result = array1.zip(array2) { a, b -> a + b }
        val newValues = result.map { it as Any? }
        val newSeries = Series(newValues, index, resultCol)
        
        val newData = data.toMutableMap()
        newData[resultCol] = newSeries
        
        val newColumns = if (resultCol in columns) columns else columns + resultCol
        
        return DataFrame(newData, newColumns, index)
    }
    
    /**
     * 使用原生方法进行向量化加法（高性能）
     */
    private fun vectorizedAddNative(col1: String, col2: String, resultCol: String): DataFrame {
        val series1 = data[col1] ?: throw IllegalArgumentException("列不存在: $col1")
        val series2 = data[col2] ?: throw IllegalArgumentException("列不存在: $col2")
        
        val array1 = series1.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val array2 = series2.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val result = NativeMath.vectorizedAdd(array1, array2)
        
        val newValues = result.map { it as Any? }
        val newSeries = Series(newValues, index, resultCol)
        
        val newData = data.toMutableMap()
        newData[resultCol] = newSeries
        
        val newColumns = if (resultCol in columns) columns else columns + resultCol
        
        return DataFrame(newData, newColumns, index)
    }
    
    /**
     * 向量化乘法 - 优先使用原生方法
     */
    fun vectorizedMultiply(col1: String, col2: String, resultCol: String): DataFrame {
        if (isNativeAvailable()) {
            return vectorizedMultiplyNative(col1, col2, resultCol)
        }
        
        val series1 = data[col1] ?: throw IllegalArgumentException("列不存在: $col1")
        val series2 = data[col2] ?: throw IllegalArgumentException("列不存在: $col2")
        
        val array1 = series1.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        val array2 = series2.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (array1.size != array2.size) {
            throw IllegalArgumentException("列长度不匹配: $col1 (${array1.size}) vs $col2 (${array2.size})")
        }
        
        val result = array1.zip(array2) { a, b -> a * b }
        val newValues = result.map { it as Any? }
        val newSeries = Series(newValues, index, resultCol)
        
        val newData = data.toMutableMap()
        newData[resultCol] = newSeries
        
        val newColumns = if (resultCol in columns) columns else columns + resultCol
        
        return DataFrame(newData, newColumns, index)
    }
    
    /**
     * 使用原生方法进行向量化乘法（高性能）
     */
    private fun vectorizedMultiplyNative(col1: String, col2: String, resultCol: String): DataFrame {
        val series1 = data[col1] ?: throw IllegalArgumentException("列不存在: $col1")
        val series2 = data[col2] ?: throw IllegalArgumentException("列不存在: $col2")
        
        val array1 = series1.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val array2 = series2.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val result = NativeMath.vectorizedMultiply(array1, array2)
        
        val newValues = result.map { it as Any? }
        val newSeries = Series(newValues, index, resultCol)
        
        val newData = data.toMutableMap()
        newData[resultCol] = newSeries
        
        val newColumns = if (resultCol in columns) columns else columns + resultCol
        
        return DataFrame(newData, newColumns, index)
    }
    
    /**
     * 计算点积 - 优先使用原生方法
     */
    fun dotProduct(col1: String, col2: String): Double {
        if (isNativeAvailable()) {
            return dotProductNative(col1, col2)
        }
        
        val series1 = data[col1] ?: throw IllegalArgumentException("列不存在: $col1")
        val series2 = data[col2] ?: throw IllegalArgumentException("列不存在: $col2")
        
        val array1 = series1.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        val array2 = series2.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (array1.size != array2.size) {
            throw IllegalArgumentException("列长度不匹配: $col1 (${array1.size}) vs $col2 (${array2.size})")
        }
        
        return array1.zip(array2) { a, b -> a * b }.sum()
    }
    
    /**
     * 使用原生方法计算点积（高性能）
     */
    private fun dotProductNative(col1: String, col2: String): Double {
        val series1 = data[col1] ?: throw IllegalArgumentException("列不存在: $col1")
        val series2 = data[col2] ?: throw IllegalArgumentException("列不存在: $col2")
        
        val array1 = series1.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val array2 = series2.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        return NativeMath.dotProduct(array1, array2)
    }
    
    /**
     * 计算范数 - 优先使用原生方法
     */
    fun norm(colName: String): Double {
        if (isNativeAvailable()) {
            return normNative(colName)
        }
        
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (doubleArray.isEmpty()) return 0.0
        
        return kotlin.math.sqrt(doubleArray.sumOf { it * it })
    }
    
    /**
     * 使用原生方法计算范数（高性能）
     */
    private fun normNative(colName: String): Double {
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        return NativeMath.norm(doubleArray)
    }
    
    /**
     * 排序 - 优先使用原生方法
     */
    fun sortValues(by: String, descending: Boolean = false): DataFrame {
        if (isNativeAvailable()) {
            return sortValuesNative(by, descending)
        }
        
        if (by !in columns) {
            throw IllegalArgumentException("列不存在: $by")
        }
        
        val series = data[by]!!
        val sortedPairs = index.zip(series.values()).sortedBy { 
            it.second?.toString() 
        }
        
        val finalPairs = if (descending) {
            sortedPairs.reversed()
        } else {
            sortedPairs
        }
        
        val newIndex = finalPairs.map { it.first }
        val sortedRows = newIndex.map { label ->
            val pos = index.indexOf(label)
            columns.associate { colName -> colName to data[colName]!![pos] }
        }
        
        return DataFrame(sortedRows)
    }
    
    /**
     * 使用原生方法进行排序（高性能）
     */
    private fun sortValuesNative(colName: String, descending: Boolean = false): DataFrame {
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val indices = if (descending) {
            NativeMath.argsort(doubleArray).reversed()
        } else {
            NativeMath.argsort(doubleArray).toList()
        }
        
        val newIndex = indices.map { index[it] }
        val sortedRows = newIndex.map { label ->
            val pos = index.indexOf(label)
            columns.associate { colName -> colName to data[colName]!![pos] }
        }
        
        return DataFrame(sortedRows)
    }
    
    /**
     * 布尔筛选（大于阈值）- 优先使用原生方法
     */
    fun filterGreaterThan(colName: String, threshold: Double): DataFrame {
        if (isNativeAvailable()) {
            return filterGreaterThanNative(colName, threshold)
        }
        
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val values = series.values()
        
        val filteredIndices = values.mapIndexedNotNull { index, value ->
            if (value != null && (value as Number).toDouble() > threshold) index else null
        }
        
        val filteredRows = filteredIndices.map { i ->
            columns.associate { colName -> colName to data[colName]!![i] }
        }
        
        return DataFrame(filteredRows)
    }
    
    /**
     * 使用原生方法进行布尔筛选（高性能）
     */
    private fun filterGreaterThanNative(colName: String, threshold: Double): DataFrame {
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val indices = NativeMath.greaterThan(doubleArray, threshold)
        
        val filteredRows = indices.map { i ->
            columns.associate { colName -> colName to data[colName]!![i] }
        }
        
        return DataFrame(filteredRows)
    }
    
    /**
     * 查找空值索引 - 优先使用原生方法
     */
    fun findNullIndices(colName: String): List<Int> {
        if (isNativeAvailable()) {
            return findNullIndicesNative(colName)
        }
        
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        return series.values()
            .mapIndexedNotNull { index, value -> if (value == null) index else null }
    }
    
    /**
     * 使用原生方法查找空值索引（高性能）
     */
    private fun findNullIndicesNative(colName: String): List<Int> {
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .map { 
                if (it == null) Double.NaN 
                else (it as Number).toDouble() 
            }
            .toDoubleArray()
        
        return NativeData.findNullIndices(doubleArray).toList()
    }
    
    /**
     * 丢弃空值 - 优先使用原生方法
     */
    fun dropNullValues(colName: String): DataFrame {
        if (isNativeAvailable()) {
            return dropNullValuesNative(colName)
        }
        
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val nonNullIndices = series.values()
            .mapIndexedNotNull { index, value -> if (value != null) index else null }
        
        val newRows = nonNullIndices.map { i ->
            columns.associate { colName -> 
                colName to data[colName]!![i] 
            }
        }
        
        return DataFrame(newRows)
    }
    
    /**
     * 使用原生方法丢弃空值（高性能）
     */
    private fun dropNullValuesNative(colName: String): DataFrame {
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .map { 
                if (it == null) Double.NaN 
                else (it as Number).toDouble() 
            }
            .toDoubleArray()
        
        val result = NativeData.dropNullValues(doubleArray)
        
        // 找到非空值的原始索引
        val nonNullIndices = mutableListOf<Int>()
        for (i in series.values().indices) {
            if (series.values()[i] != null) {
                nonNullIndices.add(i)
            }
        }
        
        // 创建新的DataFrame，只包含非空行
        val newRows = nonNullIndices.map { i ->
            columns.associate { colName -> 
                colName to data[colName]!![i] 
            }
        }
        
        return DataFrame(newRows)
    }
    
    /**
     * 填充空值 - 优先使用原生方法
     */
    fun fillNull(colName: String, value: Any): DataFrame {
        if (isNativeAvailable() && value is Number) {
            return fillNullWithConstantNative(colName, value.toDouble())
        }
        
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val newValues = series.values().map { if (it == null) value else it }
        val newSeries = Series(newValues, index, colName)
        
        val newData = data.toMutableMap()
        newData[colName] = newSeries
        
        return DataFrame(newData, columns, index)
    }
    
    /**
     * 使用原生方法填充空值（高性能）
     */
    private fun fillNullWithConstantNative(colName: String, value: Double): DataFrame {
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .map { 
                if (it == null) Double.NaN 
                else (it as Number).toDouble() 
            }
            .toDoubleArray()
        
        val result = NativeData.fillNullWithConstant(doubleArray, value)
        
        val newValues = result.map { it as Any? }
        val newSeries = Series(newValues, index, colName)
        
        val newData = data.toMutableMap()
        newData[colName] = newSeries
        
        return DataFrame(newData, columns, index)
    }
    
    /**
     * 分组求和 - 优先使用原生方法
     */
    fun groupBySum(groupCol: String, valueCol: String): DataFrame {
        if (isNativeAvailable()) {
            return groupBySumNative(groupCol, valueCol)
        }
        
        val groupSeries = data[groupCol] ?: throw IllegalArgumentException("列不存在: $groupCol")
        val valueSeries = data[valueCol] ?: throw IllegalArgumentException("列不存在: $valueCol")
        
        // 分组并求和
        val groupMap = mutableMapOf<Any?, Double>()
        
        for (i in index.indices) {
            val groupKey = groupSeries[i]
            val value = valueSeries[i]
            
            if (groupKey != null && value != null) {
                val currentValue = groupMap[groupKey] ?: 0.0
                groupMap[groupKey] = currentValue + (value as Number).toDouble()
            }
        }
        
        // 转换为DataFrame
        val resultRows = groupMap.map { (key, sum) ->
            mapOf(
                groupCol to key,
                valueCol to sum
            )
        }
        
        return DataFrame(resultRows)
    }
    
    /**
     * 使用原生方法进行分组求和（高性能）
     */
    private fun groupBySumNative(groupCol: String, valueCol: String): DataFrame {
        val groupSeries = data[groupCol] ?: throw IllegalArgumentException("列不存在: $groupCol")
        val valueSeries = data[valueCol] ?: throw IllegalArgumentException("列不存在: $valueCol")
        
        // 将分组列转换为整数索引
        val uniqueGroups = groupSeries.values().filterNotNull().toSet().sortedWith(compareBy { it.toString() })
        val groupToIndex = uniqueGroups.mapIndexed { index, value -> value to index }.toMap()
        
        val groups = groupSeries.values().map { 
            groupToIndex[it] ?: -1 
        }.toIntArray()
        
        val values = valueSeries.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val result = NativeData.groupBySum(values, groups)
        
        // 转换为DataFrame
        val resultRows = result.map { (key, sum) ->
            mapOf(
                groupCol to uniqueGroups[key.toInt()],
                valueCol to sum
            )
        }
        
        return DataFrame(resultRows)
    }
    
    /**
     * 排序索引 - 优先使用原生方法
     */
    fun sortIndices(colName: String, descending: Boolean = false): List<Int> {
        if (isNativeAvailable()) {
            return sortIndicesNative(colName, descending)
        }
        
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val values = series.values()
        
        return values.mapIndexed { index, value -> index to value }
            .sortedBy { it.second?.toString() }
            .let { if (descending) it.reversed() else it }
            .map { it.first }
    }
    
    /**
     * 使用原生方法进行排序索引（高性能）
     */
    private fun sortIndicesNative(colName: String, descending: Boolean = false): List<Int> {
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        return NativeData.sortIndices(doubleArray, descending).toList()
    }
    
    /**
     * 数据合并 - 优先使用原生方法
     */
    fun merge(other: DataFrame, on: String, how: String = "inner"): DataFrame {
        if (isNativeAvailable()) {
            return mergeNative(other, on)
        }
        
        val resultRows = mutableListOf<Map<String, Any?>>()
        
        for (i in index.indices) {
            val thisValue = at(i, on)
            for (j in other.index.indices) {
                val otherValue = other.at(j, on)
                if (thisValue == otherValue) {
                    val row = mutableMapOf<String, Any?>()
                    // 添加当前DataFrame的所有列
                    columns.forEach { colName ->
                        row[colName] = at(i, colName)
                    }
                    // 添加另一个DataFrame的所有列（排除连接列）
                    other.columns().forEach { colName ->
                        if (colName != on) {
                            row[colName] = other.at(j, colName)
                        }
                    }
                    resultRows.add(row)
                }
            }
        }
        
        return DataFrame(resultRows)
    }
    
    /**
     * 使用原生方法进行数据合并（高性能）
     */
    private fun mergeNative(other: DataFrame, on: String): DataFrame {
        val leftSeries = this.data[on] ?: throw IllegalArgumentException("列不存在: $on")
        val rightSeries = other.data[on] ?: throw IllegalArgumentException("列不存在: $on")
        
        val leftArray = leftSeries.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val rightArray = rightSeries.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val indices = NativeData.mergeIndices(leftArray, rightArray)
        
        // indices数组包含成对的索引：[leftIndex1, rightIndex1, leftIndex2, rightIndex2, ...]
        val resultRows = mutableListOf<Map<String, Any?>>()
        
        for (i in indices.indices step 2) {
            val leftIndex = indices[i]
            val rightIndex = indices[i + 1]
            
            val row = mutableMapOf<String, Any?>()
            // 添加左DataFrame的所有列
            columns.forEach { colName ->
                row[colName] = this.at(leftIndex, colName)
            }
            // 添加右DataFrame的所有列（排除连接列）
            other.columns().forEach { colName ->
                if (colName != on) {
                    row[colName] = other.at(rightIndex, colName)
                }
            }
            resultRows.add(row)
        }
        
        return DataFrame(resultRows)
    }
    
    /**
     * 布尔索引 - 优先使用原生方法
     */
    fun where(colName: String, threshold: Double): DataFrame {
        if (isNativeAvailable()) {
            return whereNative(colName, threshold)
        }
        
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val values = series.values()
        
        val filteredIndices = values.mapIndexedNotNull { index, value ->
            if (value != null && (value as Number).toDouble() > threshold) index else null
        }
        
        val filteredRows = filteredIndices.map { i ->
            columns.associate { colName -> colName to data[colName]!![i] }
        }
        
        return DataFrame(filteredRows)
    }
    
    /**
     * 使用原生方法进行布尔索引（高性能）
     */
    private fun whereNative(colName: String, threshold: Double): DataFrame {
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val mask = NativeMath.greaterThan(doubleArray, threshold)
        val indices = NativeData.where(mask)
        
        val filteredRows = indices.map { i ->
            columns.associate { colName -> colName to data[colName]!![i] }
        }
        
        return DataFrame(filteredRows)
    }
    
    /**
     * 统计描述 - 优先使用原生方法
     */
    fun describe(colName: String): Map<String, Double> {
        if (isNativeAvailable()) {
            return describeNative(colName)
        }
        
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (doubleArray.isEmpty()) {
            return mapOf(
                "count" to 0.0,
                "mean" to Double.NaN,
                "std" to Double.NaN,
                "min" to Double.NaN,
                "max" to Double.NaN
            )
        }
        
        val count = doubleArray.size.toDouble()
        val mean = doubleArray.sum() / count
        val variance = doubleArray.sumOf { (it - mean) * (it - mean) } / count
        val std = kotlin.math.sqrt(variance)
        val min = doubleArray.minOrNull() ?: Double.NaN
        val max = doubleArray.maxOrNull() ?: Double.NaN
        
        return mapOf(
            "count" to count,
            "mean" to mean,
            "std" to std,
            "min" to min,
            "max" to max
        )
    }
    
    /**
     * 使用原生方法进行统计描述（高性能）
     */
    private fun describeNative(colName: String): Map<String, Double> {
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
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
     * 数据采样 - 优先使用原生方法
     */
    fun sample(sampleSize: Int): DataFrame {
        if (isNativeAvailable()) {
            return sampleNative(sampleSize)
        }
        
        if (index.isEmpty()) {
            return this
        }
        
        val actualSampleSize = kotlin.math.min(sampleSize, index.size)
        val indices = (0 until index.size).shuffled().take(actualSampleSize)
        
        val sampledRows = indices.map { i ->
            columns.associate { colName -> 
                colName to data[colName]!![i] 
            }
        }
        
        return DataFrame(sampledRows)
    }
    
    /**
     * 使用原生方法进行数据采样（高性能）
     */
    private fun sampleNative(sampleSize: Int): DataFrame {
        if (index.isEmpty()) {
            return this
        }
        
        // 获取所有列的数值数组
        val arrays = columns.map { colName ->
            val series = data[colName]!!
            series.values()
                .map { 
                    if (it == null) Double.NaN 
                    else (it as Number).toDouble() 
                }
                .toDoubleArray()
        }
        
        if (arrays.isEmpty()) {
            return this
        }
        
        // 对第一列进行采样，获取索引
        val sampleIndices = NativeData.sample(arrays[0], sampleSize).map { it.toInt() }
        
        // 根据采样索引构建新的DataFrame
        val sampledRows = sampleIndices.map { i ->
            columns.associate { colName -> 
                colName to data[colName]!![i] 
            }
        }
        
        return DataFrame(sampledRows)
    }
    
    /**
     * 批量处理 - 优先使用原生方法
     */
    fun processBatch(colName: String, batchSize: Int = 1000): DataFrame {
        if (isNativeAvailable()) {
            return processBatchNative(colName, batchSize)
        }
        
        // Kotlin实现：简单地返回原数据（因为批量处理主要是为了性能优化）
        // 在实际应用中，这里可以实现分块处理逻辑
        return this
    }
    
    /**
     * 使用原生方法批量处理（高性能）
     */
    private fun processBatchNative(colName: String, batchSize: Int = 1000): DataFrame {
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val doubleArray = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
            .toDoubleArray()
        
        val result = NativeBatch.processBatch(doubleArray, batchSize)
        
        val newValues = result.map { it as Any? }
        val newSeries = Series(newValues, index, colName)
        
        val newData = data.toMutableMap()
        newData[colName] = newSeries
        
        return DataFrame(newData, columns, index)
    }
    
    /**
     * 常用的合并操作 - 合并多个DataFrame
     */
    fun mergeMultiple(dfs: List<DataFrame>, on: String): DataFrame {
        var result = this
        for (df in dfs) {
            result = result.merge(df, on)
        }
        return result
    }
    
    /**
     * 连接操作（类似SQL JOIN）- 支持多种连接类型
     */
    fun join(other: DataFrame, on: String, how: String = "inner"): DataFrame {
        return when (how.lowercase()) {
            "inner" -> merge(other, on)
            "left" -> leftJoin(other, on)
            "right" -> rightJoin(other, on)
            "outer" -> outerJoin(other, on)
            else -> throw IllegalArgumentException("不支持的连接类型: $how")
        }
    }
    
    /**
     * 左连接
     */
    private fun leftJoin(other: DataFrame, on: String): DataFrame {
        val resultRows = mutableListOf<Map<String, Any?>>()
        
        for (i in index.indices) {
            val thisValue = at(i, on)
            var matched = false
            
            for (j in other.index.indices) {
                val otherValue = other.at(j, on)
                if (thisValue == otherValue) {
                    val row = mutableMapOf<String, Any?>()
                    columns.forEach { colName ->
                        row[colName] = at(i, colName)
                    }
                    other.columns().forEach { colName ->
                        if (colName != on) {
                            row[colName] = other.at(j, colName)
                        }
                    }
                    resultRows.add(row)
                    matched = true
                }
            }
            
            // 如果没有匹配，仍然添加左表的行，右表列为null
            if (!matched) {
                val row = mutableMapOf<String, Any?>()
                columns.forEach { colName ->
                    row[colName] = at(i, colName)
                }
                other.columns().forEach { colName ->
                    if (colName != on) {
                        row[colName] = null
                    }
                }
                resultRows.add(row)
            }
        }
        
        return DataFrame(resultRows)
    }
    
    /**
     * 右连接
     */
    private fun rightJoin(other: DataFrame, on: String): DataFrame {
        val resultRows = mutableListOf<Map<String, Any?>>()
        
        for (j in other.index.indices) {
            val otherValue = other.at(j, on)
            var matched = false
            
            for (i in index.indices) {
                val thisValue = at(i, on)
                if (thisValue == otherValue) {
                    val row = mutableMapOf<String, Any?>()
                    columns.forEach { colName ->
                        row[colName] = at(i, colName)
                    }
                    other.columns().forEach { colName ->
                        if (colName != on) {
                            row[colName] = other.at(j, colName)
                        }
                    }
                    resultRows.add(row)
                    matched = true
                }
            }
            
            // 如果没有匹配，仍然添加右表的行，左表列为null
            if (!matched) {
                val row = mutableMapOf<String, Any?>()
                columns.forEach { colName ->
                    row[colName] = null
                }
                other.columns().forEach { colName ->
                    if (colName != on) {
                        row[colName] = other.at(j, colName)
                    }
                }
                resultRows.add(row)
            }
        }
        
        return DataFrame(resultRows)
    }
    
    /**
     * 全外连接
     */
    private fun outerJoin(other: DataFrame, on: String): DataFrame {
        val resultRows = mutableListOf<Map<String, Any?>>()
        val matchedLeft = mutableSetOf<Int>()
        val matchedRight = mutableSetOf<Int>()
        
        // 先处理匹配的行
        for (i in index.indices) {
            val thisValue = at(i, on)
            for (j in other.index.indices) {
                val otherValue = other.at(j, on)
                if (thisValue == otherValue) {
                    val row = mutableMapOf<String, Any?>()
                    columns.forEach { colName ->
                        row[colName] = at(i, colName)
                    }
                    other.columns().forEach { colName ->
                        if (colName != on) {
                            row[colName] = other.at(j, colName)
                        }
                    }
                    resultRows.add(row)
                    matchedLeft.add(i)
                    matchedRight.add(j)
                }
            }
        }
        
        // 添加左表未匹配的行
        for (i in index.indices) {
            if (i !in matchedLeft) {
                val row = mutableMapOf<String, Any?>()
                columns.forEach { colName ->
                    row[colName] = at(i, colName)
                }
                other.columns().forEach { colName ->
                    if (colName != on) {
                        row[colName] = null
                    }
                }
                resultRows.add(row)
            }
        }
        
        // 添加右表未匹配的行
        for (j in other.index.indices) {
            if (j !in matchedRight) {
                val row = mutableMapOf<String, Any?>()
                columns.forEach { colName ->
                    row[colName] = null
                }
                other.columns().forEach { colName ->
                    if (colName != on) {
                        row[colName] = other.at(j, colName)
                    }
                }
                resultRows.add(row)
            }
        }
        
        return DataFrame(resultRows)
    }
    


    companion object {
        /**
         * 从CSV文件读取数据
         */
        fun readCSV(
            file: File, 
            delimiter: String = ",", 
            header: Boolean = true, 
            autoType: Boolean = true,
            encoding: String = "UTF-8",
            skipLines: Int = 0,
            nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
            trimValues: Boolean = true
        ): DataFrame {
            if (!file.exists()) {
                throw IllegalArgumentException("文件不存在: ${file.absolutePath}")
            }
            
            if (!file.canRead()) {
                throw SecurityException("无法读取文件: ${file.absolutePath}")
            }
            
            val lines = try {
                file.readLines(charset(encoding))
            } catch (e: Exception) {
                throw RuntimeException("读取文件失败: ${e.message}", e)
            }
            
            if (lines.isEmpty()) {
                return DataFrame(emptyList<Map<String, Any?>>())
            }
            
            // 跳过指定行数
            val effectiveLines = lines.drop(skipLines)
            if (effectiveLines.isEmpty()) {
                return DataFrame(emptyList<Map<String, Any?>>())
            }
            
            val firstLine = effectiveLines.first()
            val headers: List<String>
            val dataLines: List<String>
            
            if (header) {
                headers = parseCSVLine(firstLine, delimiter, trimValues)
                dataLines = effectiveLines.drop(1)
            } else {
                // 如果没有表头，使用默认列名
                val firstLineValues = parseCSVLine(firstLine, delimiter, trimValues)
                headers = firstLineValues.indices.map { "col$it" }
                dataLines = effectiveLines
            }
            
            if (headers.isEmpty()) {
                throw IllegalArgumentException("解析失败: 未找到有效的列名")
            }
            
            // 预先推断每列的数据类型（只使用第一行数据）
            val columnTypes = mutableMapOf<String, (String) -> Any?>()
            if (autoType && dataLines.isNotEmpty()) {
                val firstLineValues = parseCSVLine(dataLines.first(), delimiter, trimValues)
                val adjustedFirstValues = if (firstLineValues.size < headers.size) {
                    firstLineValues + List(headers.size - firstLineValues.size) { "" }
                } else if (firstLineValues.size > headers.size) {
                    firstLineValues.take(headers.size)
                } else {
                    firstLineValues
                }
                
                headers.forEachIndexed { index, header ->
                    val firstValue = adjustedFirstValues.getOrNull(index) ?: ""
                    // 为每列创建一个转换函数，基于第一个值推断类型
                    val inferredType = inferTypeFromSample(firstValue, nullValues)
                    columnTypes[header] = { value ->
                        if (value in nullValues) null 
                        else convertValueWithType(value, inferredType)
                    }
                }
            }
            
            // 解析数据行
            val rows = dataLines.mapIndexed { lineIndex, line ->
                try {
                    val values = parseCSVLine(line, delimiter, trimValues)
                    
                    // 处理列数不匹配的情况
                    val adjustedValues = if (values.size < headers.size) {
                        // 补充空值
                        values + List(headers.size - values.size) { "" }
                    } else if (values.size > headers.size) {
                        // 截断多余列
                        values.take(headers.size)
                    } else {
                        values
                    }
                    
                    val rowMap = headers.zip(adjustedValues).toMap()
                    
                    if (autoType) {
                        // 使用预先推断的类型进行转换
                        rowMap.mapValues { (key, value) ->
                            val converter = columnTypes[key]
                            if (converter != null) {
                                converter(value)
                            } else {
                                // 如果没有推断出类型，使用原始值
                                if (value in nullValues) null else value
                            }
                        }
                    } else {
                        // 不进行类型转换，但处理空值
                        rowMap.mapValues { (_, value) ->
                            if (value in nullValues) null else value
                        }
                    }
                } catch (e: Exception) {
                    throw RuntimeException("解析第 ${lineIndex + skipLines + (if (header) 2 else 1)} 行失败: $line", e)
                }
            }
            
            // 如果没有数据行但有表头，创建一个空DataFrame但保留列信息
            if (rows.isEmpty()) {
                // 创建空的DataFrame，但保留列名
                val emptyData = headers.associateWith { emptyList<Any?>() }
                return DataFrame(emptyData)
            }
            
            return DataFrame(rows)
        }
        
        /**
         * 解析单行CSV数据，支持引号内的分隔符
         */
        fun parseCSVLine(line: String, delimiter: String, trim: Boolean): List<String> {
            val result = mutableListOf<String>()
            val current = StringBuilder()
            var inQuotes = false
            var quoteChar: Char? = null
            
            var i = 0
            while (i < line.length) {
                val char = line[i]
                
                when {
                    (char == '"' || char == '\'') -> {
                        if (!inQuotes) {
                            inQuotes = true
                            quoteChar = char
                        } else if (char == quoteChar) {
                            // 检查是否是转义引号
                            if (i + 1 < line.length && line[i + 1] == quoteChar) {
                                current.append(quoteChar)
                                i++ // 跳过下一个引号
                            } else {
                                inQuotes = false
                                quoteChar = null
                            }
                        } else {
                            current.append(char)
                        }
                    }
                    char == delimiter[0] && !inQuotes -> {
                        val value = if (trim) current.toString().trim() else current.toString()
                        result.add(value)
                        current.clear()
                    }
                    else -> {
                        current.append(char)
                    }
                }
                i++
            }
            
            // 添加最后一个值
            val value = if (trim) current.toString().trim() else current.toString()
            result.add(value)
            
            return result
        }
        
        /**
         * 基于单个样本值推断类型
         */
        fun inferTypeFromSample(value: String, nullValues: List<String>): String {
            if (value in nullValues) {
                return "null"
            }
            
            // 尝试转换为整数
            if (value.matches(Regex("^-?\\d+$"))) {
                return try {
                    value.toInt()
                    "int"
                } catch (e: NumberFormatException) {
                    try {
                        value.toLong()
                        "long"
                    } catch (e2: NumberFormatException) {
                        "string"
                    }
                }
            }
            
            // 尝试转换为浮点数
            if (value.matches(Regex("^-?\\d+\\.\\d+$")) || 
                value.matches(Regex("^-?\\d+\\.\\d+[eE][+-]?\\d+$")) ||
                value.matches(Regex("^-?\\d+[eE][+-]?\\d+$"))) {
                return "double"
            }
            
            // 尝试转换为布尔值
            if (value.equals("true", ignoreCase = true) || 
                value.equals("false", ignoreCase = true) ||
                value.equals("yes", ignoreCase = true) ||
                value.equals("no", ignoreCase = true) ||
                value.equals("1", ignoreCase = true) ||
                value.equals("0", ignoreCase = true)) {
                return "boolean"
            }
            
            // 尝试转换为日期（简单格式）
            if (value.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")) ||
                value.matches(Regex("^\\d{2}/\\d{2}/\\d{4}$"))) {
                return "date"
            }
            
            // 默认返回字符串
            return "string"
        }
        
        /**
         * 根据推断的类型转换值
         */
        fun convertValueWithType(value: String, type: String): Any? {
            return when (type) {
                "int" -> value.toIntOrNull() ?: value
                "long" -> value.toLongOrNull() ?: value
                "double" -> value.toDoubleOrNull() ?: value
                "boolean" -> value.equals("true", ignoreCase = true) || 
                           value.equals("yes", ignoreCase = true) || 
                           value.equals("1", ignoreCase = true)
                "date" -> value // 保持字符串
                "string" -> value
                else -> value
            }
        }
    }
    
    /**
     * 导出为CSV文件
     */
    fun toCSV(file: File) {
        FileWriter(file).use { writer ->
            // 写入表头
            writer.write(columns.joinToString(","))
            writer.write("\n")
            
            // 写入数据
            for (i in index.indices) {
                val row = columns.map { colName ->
                    data[colName]!![i]?.toString() ?: ""
                }
                writer.write(row.joinToString(","))
                writer.write("\n")
            }
        }
    }
}

/**
 * 分组操作类
 */
class GroupBy(
    private val df: DataFrame,
    private val groupCols: List<String>
) {
    /**
     * 聚合操作
     */
    fun agg(operations: Map<String, String>): DataFrame {
        // 获取分组键的唯一值
        val groupKeyValues = mutableSetOf<List<Any?>>()
        
        for (rowIndex in df.index().indices) {
            val key = groupCols.map { colName -> df.at(rowIndex, colName) }
            groupKeyValues.add(key)
        }
        
        // 对每个分组进行聚合
        val resultRows = mutableListOf<Map<String, Any?>>()
        
        for (keyValue in groupKeyValues) {
            // 筛选出该分组的所有行
            val groupRows = mutableListOf<Map<String, Any?>>()
            for (rowIndex in df.index().indices) {
                val rowKey = groupCols.map { colName -> df.at(rowIndex, colName) }
                if (rowKey == keyValue) {
                    // 将 Series<Any> 转换为 Map<String, Any?>
                    val row = df.iloc(rowIndex)
                    val rowValues = row.mapValues { (_, series) -> series?.get(0) }
                    groupRows.add(rowValues)
                }
            }
            
            // 创建临时DataFrame进行聚合
            val tempDF = DataFrame(groupRows)
            val aggResult = tempDF.agg(operations)
            
            // 添加分组键到结果
            val resultRow = mutableMapOf<String, Any?>()
            groupCols.forEachIndexed { i, colName ->
                resultRow[colName] = keyValue[i]
            }
            // 添加聚合结果
            aggResult.columns().forEach { colName ->
                if (colName !in groupCols) {
                    resultRow[colName] = aggResult.iloc(0)[colName]
                }
            }
            
            resultRows.add(resultRow)
        }
        
        return DataFrame(resultRows)
    }
    
    /**
     * 求和
     */
    fun sum(): DataFrame {
        return agg(groupCols.associateWith { "sum" })
    }
    
    /**
     * 平均值
     */
    fun mean(): DataFrame {
        return agg(groupCols.associateWith { "mean" })
    }
    
    /**
     * 计数
     */
    fun count(): DataFrame {
        return agg(groupCols.associateWith { "count" })
    }
}


// 扩展函数 - 便捷IO操作
fun DataFrame.saveToPrivateStorage(context: android.content.Context, fileName: String) {
    DataFrameIO.saveToPrivateStorage(context, fileName, this)
}

fun DataFrame.saveToExternalStorage(filePath: String) {
    DataFrameIO.saveToExternalStorage(filePath, this)
}
