package cn.ac.oac.libs.andas.entity

import cn.ac.oac.libs.andas.types.AndaTypes
import cn.ac.oac.libs.andas.core.NativeMath
import cn.ac.oac.libs.andas.core.NativeBatch
import java.io.File

/**
 * DataFrame 类似于二维表格，是Andas库的核心数据结构
 * 提供类似pandas.DataFrame的数据操作接口
 * 
 * index 通过 data 中的 Series 获取，不单独存储
 */
class DataFrame {
    private val data: MutableMap<String, Series<Any>> = LinkedHashMap()
    private var columns: List<String> = emptyList()

    /**
     * 通过列数据构造DataFrame
     */
    constructor(columnsData: Map<String, List<Any?>>) {
        if (columnsData.isEmpty()) {
            return
        }

        this.columns = columnsData.keys.toList()

        // 找到最长列的长度作为索引长度
        val maxSize = columnsData.values.maxOfOrNull { it.size } ?: 0
        val defaultIndex = defaultIndexFor(maxSize)

        // 验证索引类型（范围索引无需逐项检查）
        validateIndexType(defaultIndex)

        columnsData.forEach { (colName, colData) ->
            // 如果列长度不足，用null填充
            val paddedData = if (colData.size < maxSize) {
                colData + List(maxSize - colData.size) { null }
            } else {
                colData
            }
            data[colName] = Series.wrap(paddedData, defaultIndex, colName)
        }
    }

    /**
     * 通过行数据构造DataFrame
     */
    constructor(rowsData: List<Map<String, Any?>>) {
        if (rowsData.isEmpty()) {
            return
        }

        // 提取所有列名（保持首次出现顺序）
        val allColumns = LinkedHashSet<String>()
        rowsData.forEach { row -> allColumns.addAll(row.keys) }
        this.columns = allColumns.toList()

        val rowCount = rowsData.size
        val defaultIndex = defaultIndexFor(rowCount)

        // 一次遍历完成行列转置，避免每列都重新扫描全部行、也避免每行创建 Map
        val arrays = Array(columns.size) { arrayOfNulls<Any?>(rowCount) }
        for (r in 0 until rowCount) {
            val row = rowsData[r]
            for (c in columns.indices) {
                arrays[c][r] = row[columns[c]]
            }
        }
        for (c in columns.indices) {
            data[columns[c]] = Series.wrap(arrays[c].asList(), defaultIndex, columns[c])
        }
    }

    /**
     * 私有构造函数 - 用于内部创建DataFrame
     */
    private constructor(
        data: Map<String, Series<Any>>,
        columns: List<String>
    ) {
        this.data.putAll(data)
        this.columns = columns
    }

    /**
     * 验证索引类型：只能是Int或String
     */
    private fun validateIndexType(indexList: List<Any>) {
        if (indexList is RangeIndexList) return
        for (item in indexList) {
            when (item) {
                is Int, is String -> continue
                else -> throw IllegalArgumentException("索引类型必须是Int或String，但得到了: ${item::class.simpleName}")
            }
        }
    }

    /** 大索引使用 O(1) 内存的虚拟范围索引，避免为每行创建装箱 Int。 */
    private fun defaultIndexFor(size: Int): List<Any> =
        if (size > 64) RangeIndexList(size) else (0 until size).toList()

    /**
     * 获取索引列表 - 从第一个Series获取
     */
    fun index(): List<Any> {
        if (data.isEmpty()) {
            return emptyList()
        }
        return data.values.first().index()
    }

    /**
     * 获取形状 (行数, 列数)
     */
    fun shape(): Pair<Int, Int> = index().size to columns.size
    
    /**
     * 获取列名列表
     */
    fun columns(): List<String> = columns.toList()
    
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
    fun isNativeAvailable(): Boolean = NativeAvailability.available
    
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
        val numericColumns = columns.filter { data[it]!!.isNumericColumn() }
        if (numericColumns.size < 2) {
            throw IllegalArgumentException("需要至少两个数值列来计算相关系数")
        }

        val arrays = numericColumns.associateWith { data[it]!!.toDoubleArrayNumeric() }
        val corrData = LinkedHashMap<String, List<Any?>>(numericColumns.size)
        for (c1 in numericColumns) {
            val rowValues = ArrayList<Any?>(numericColumns.size)
            for (c2 in numericColumns) {
                rowValues.add(correlation(arrays[c1]!!, arrays[c2]!!))
            }
            corrData[c1] = rowValues
        }
        return DataFrame(corrData)
    }

    /** Pearson 相关系数；任一位置为空（NaN）时跳过该行。 */
    private fun correlation(a: DoubleArray, b: DoubleArray): Double {
        if (a.size != b.size) return Double.NaN
        var n = 0L
        var meanA = 0.0
        var meanB = 0.0
        var m2A = 0.0
        var m2B = 0.0
        var cov = 0.0
        for (i in a.indices) {
            val va = a[i]
            val vb = b[i]
            if (va.isNaN() || vb.isNaN()) continue
            n++
            val da = va - meanA
            val db = vb - meanB
            meanA += da / n
            meanB += db / n
            m2A += da * (va - meanA)
            m2B += db * (vb - meanB)
            cov += da * (vb - meanB)
        }
        if (n < 2) return Double.NaN
        val denominator = kotlin.math.sqrt(m2A * m2B)
        return if (denominator == 0.0) 0.0 else cov / denominator
    }

    /**
     * 获取前n行
     */
    fun head(n: Int = 5): DataFrame {
        val indexList = index()
        val actualN = kotlin.math.min(n, indexList.size)
        val newIndex = indexList.take(actualN)
        val newData = columns.associate { colName ->
            colName to data[colName]!!.head(actualN)
        }
        return DataFrame(newData, columns)
    }
    
    /**
     * 获取后n行
     */
    fun tail(n: Int = 5): DataFrame {
        val indexList = index()
        val actualN = kotlin.math.min(n, indexList.size)
        val newIndex = indexList.takeLast(actualN)
        val newData = columns.associate { colName ->
            colName to data[colName]!!.tail(actualN)
        }
        return DataFrame(newData, columns)
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
        return DataFrame(newData, colNames.toList())
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
        return DataFrame(newData, keys)
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
        val indexList = index()
        if (value.size() != indexList.size) {
            throw IllegalArgumentException("新列长度必须与现有行数一致，当前: ${value.size()}, 需要: ${indexList.size}")
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
        val indexList = index()
        if (key < 0 || key >= indexList.size) {
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
                data[colName] = Series(currentValues, indexList, colName)
            } else {
                // 如果列不存在，创建新列（用 null 填充之前的行）
                val newValues = List(indexList.size) { i -> if (i == key) cellValue else null }
                data[colName] = Series(newValues, indexList, colName)
                columns = columns + colName
            }
        }
    }

    /**
     * 按条件筛选行
     */
    fun filter(predicate: (Map<String, Series<Any>?>) -> Boolean): DataFrame {
        val n = shape().first
        val positions = ArrayList<Int>()
        for (i in 0 until n) {
            if (predicate(RowView(i))) {
                positions.add(i)
            }
        }
        return takeRows(positions.toIntArray())
    }

    /**
     * 行视图：只在访问某列时才创建对应的单元素 Series。
     * 绝大多数谓词只读取 1~2 列，避免为每一行生成整行 Map 和全部列的 Series。
     */
    private inner class RowView(private val rowIndex: Int) : AbstractMap<String, Series<Any>?>() {
        private val cache = HashMap<String, Series<Any>?>(2)

        override val entries: Set<Map.Entry<String, Series<Any>?>>
            get() = columns.associateWith { get(it) }.entries

        override val size: Int
            get() = columns.size

        override fun containsKey(key: String): Boolean = key in columns

        override fun get(key: String): Series<Any>? {
            if (key !in columns) return null
            if (cache.containsKey(key)) return cache[key]
            val value = data[key]!![rowIndex]
            val series = if (value != null) Series.single(value, key) else null
            cache[key] = series
            return series
        }
    }

    /**
     * 获取指定行 - 返回 Map<String, Series<Any>?>
     */
    fun getRow(rowIndex: Int): Map<String, Series<Any>?> {
        if (rowIndex < 0 || rowIndex >= shape().first) {
            throw IndexOutOfBoundsException("行索引超出范围: $rowIndex")
        }
        return RowView(rowIndex)
    }
    
    /**
     * 按标签获取行 - 返回 Map<String, Series<Any>?>
     */
    fun loc(label: Any): Map<String, Series<Any>?> {
        val position = data.values.firstOrNull()?.positionOf(label) ?: -1
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
        val series = data[colName] ?: throw IllegalArgumentException("列不存在: $colName")
        val position = series.positionOf(label)
        if (position == -1) {
            throw NoSuchElementException("未找到索引: $label")
        }
        return series[position]
    }
    
    /**
     * 检查空值
     */
    fun isnull(): DataFrame {
        val indexList = index()
        val n = indexList.size
        val newData = LinkedHashMap<String, Series<Any>>(columns.size)
        for (colName in columns) {
            val series = data[colName]!!
            val values = BooleanArray(n)
            for (i in 0 until n) values[i] = series[i] == null
            newData[colName] = Series.fromBooleanArray(values, null, indexList, colName, AndaTypes.BOOL)
        }
        return DataFrame(newData, columns)
    }

    /**
     * 检查非空值
     */
    fun notnull(): DataFrame {
        val indexList = index()
        val n = indexList.size
        val newData = LinkedHashMap<String, Series<Any>>(columns.size)
        for (colName in columns) {
            val series = data[colName]!!
            val values = BooleanArray(n)
            for (i in 0 until n) values[i] = series[i] != null
            newData[colName] = Series.fromBooleanArray(values, null, indexList, colName, AndaTypes.BOOL)
        }
        return DataFrame(newData, columns)
    }

    /**
     * 丢弃包含空值的行
     */
    fun dropna(): DataFrame {
        val n = shape().first
        val positions = ArrayList<Int>()
        outer@ for (i in 0 until n) {
            for (colName in columns) {
                if (data[colName]!![i] == null) continue@outer
            }
            positions.add(i)
        }
        return takeRows(positions.toIntArray())
    }

    /**
     * 填充空值
     */
    fun fillna(value: Any): DataFrame {
        val newData = LinkedHashMap<String, Series<Any>>(columns.size)
        for (colName in columns) {
            val series = data[colName]!!
            if (series.nonNullCount() == series.size()) {
                newData[colName] = series
            } else if (value is Number && series.isNumericColumn()) {
                @Suppress("UNCHECKED_CAST")
                newData[colName] = series.fillNullWithConstant(value.toDouble()) as Series<Any>
            } else {
                val values = ArrayList<Any?>(series.size())
                for (i in 0 until series.size()) {
                    values.add(series[i] ?: value)
                }
                newData[colName] = Series.wrap(values, index(), colName, series.dtype())
            }
        }
        return DataFrame(newData, columns)
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
        val resultData = LinkedHashMap<String, List<Any?>>(columns.size)
        val ops = operations.values.map { it.lowercase() }

        for (colName in columns) {
            val series = data[colName]!!
            val numeric = series.isNumericColumn()
            val count = series.nonNullCount()

            var sum = 0.0
            var sumReady = false
            fun numericSum(): Double {
                if (!sumReady) {
                    sum = series.sumNumeric()
                    sumReady = true
                }
                return sum
            }

            val colResults = ArrayList<Any?>(ops.size)
            for (op in ops) {
                val value: Any? = when (op) {
                    "sum" -> if (numeric && count > 0) numericSum() else null
                    "mean" -> if (numeric && count > 0) numericSum() / count else null
                    "min" -> when {
                        count == 0 -> null
                        numeric -> series.minNumeric()
                        else -> series.values().filterNotNull().minOfOrNull { it.toString() }
                    }
                    "max" -> when {
                        count == 0 -> null
                        numeric -> series.maxNumeric()
                        else -> series.values().filterNotNull().maxOfOrNull { it.toString() }
                    }
                    "count" -> count
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
            newName to data[colName]!!.renamed(newName)
        }
        
        return DataFrame(newData, newColumns)
    }
    
    /**
     * 添加列
     */
    fun addColumn(name: String, data: List<Any?>): DataFrame {
        val indexList = index()
        if (data.size != indexList.size) {
            throw IllegalArgumentException("新列长度必须与现有行数一致")
        }
        
        val newData = this.data.toMutableMap()
        newData[name] = Series.wrap(data, indexList, name)
        
        val newColumns = if (name in columns) columns else columns + name
        
        return DataFrame(newData, newColumns)
    }

    /**
     * 添加列（通过转换函数）
     */
    fun addColumn(name: String, transform: (Map<String, Any?>) -> Any?): DataFrame {
        val n = shape().first
        val newData = this.data.toMutableMap()
        val newColumnData = ArrayList<Any?>(n)
        for (i in 0 until n) {
            newColumnData.add(transform(ValueRowView(i)))
        }
        newData[name] = Series.wrap(newColumnData, index(), name)

        val newColumns = if (name in columns) columns else columns + name
        return DataFrame(newData, newColumns)
    }

    /** 行值视图：按需读取单元格，避免为每一行构造完整 Map。 */
    private inner class ValueRowView(private val rowIndex: Int) : AbstractMap<String, Any?>() {
        override val entries: Set<Map.Entry<String, Any?>>
            get() = columns.associateWith { data[it]!![rowIndex] }.entries

        override val size: Int
            get() = columns.size

        override fun containsKey(key: String): Boolean = key in columns

        override fun get(key: String): Any? = data[key]?.get(rowIndex)
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
        
        return DataFrame(newData, newColumns)
    }
    
    /**
     * 复制DataFrame，可选修改属性
     */
    fun copy(
        data: Map<String, Series<Any>>? = null,
        columns: List<String>? = null
    ): DataFrame {
        return DataFrame(
            data ?: this.data,
            columns ?: this.columns
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
        val indexList = index()
        val maxRows = kotlin.math.min(10, indexList.size)
        
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
        
        if (indexList.size > maxRows) {
            builder.append("...\n")
        }
        
        builder.append("Shape: ${shape()}")
        return builder.toString()
    }
    
    // ==================== 原生/列式高性能操作 ====================

    private fun column(name: String): Series<Any> =
        data[name] ?: throw IllegalArgumentException("列不存在: $name")

    /** 按行位置抽取一个新的 DataFrame（保留原索引标签）。 */
    private fun takeRows(positions: IntArray): DataFrame {
        val newData = LinkedHashMap<String, Series<Any>>(columns.size)
        for (colName in columns) {
            newData[colName] = data[colName]!!.take(positions)
        }
        return DataFrame(newData, columns)
    }

    private fun appendNumericColumn(resultCol: String, values: DoubleArray): DataFrame {
        val newData = data.toMutableMap()
        newData[resultCol] = Series.fromDoubleArray(
            values, null, index(), resultCol, AndaTypes.FLOAT64
        )
        val newColumns = if (resultCol in columns) columns else columns + resultCol
        return DataFrame(newData, newColumns)
    }

    /**
     * 计算指定数值列的和
     */
    fun sum(colName: String): Double = column(colName).sumNumeric()

    /**
     * 计算指定数值列的均值
     */
    fun mean(colName: String): Double {
        val series = column(colName)
        val count = series.countNumeric()
        return if (count == 0) Double.NaN else series.sumNumeric() / count
    }

    /**
     * 计算指定数值列的最大值
     */
    fun max(colName: String): Double = column(colName).maxNumeric()

    /**
     * 计算指定数值列的最小值
     */
    fun min(colName: String): Double = column(colName).minNumeric()

    /**
     * 计算指定数值列的方差（样本方差，ddof=1）
     */
    fun variance(colName: String): Double {
        val series = column(colName)
        return if (series.countNumeric() < 2) Double.NaN else series.varianceNumeric()
    }

    /**
     * 计算指定数值列的标准差（样本标准差，ddof=1）
     */
    fun std(colName: String): Double {
        val variance = variance(colName)
        return if (variance.isNaN()) Double.NaN else kotlin.math.sqrt(variance)
    }

    /**
     * 对指定数值列进行标准归一化（z-score）
     */
    fun normalize(colName: String): DataFrame {
        val series = column(colName)
        if (series.size() == 0) return this

        val mean = series.mean() // 空值会被跳过；空列在上面已返回
        val std = kotlin.math.sqrt(series.populationVarianceNumeric())
        if (std == 0.0 || std.isNaN()) return this

        val values = DoubleArray(series.size())
        for (i in values.indices) {
            val v = series[i]
            values[i] = if (v is Number) (v.toDouble() - mean) / std else Double.NaN
        }
        return replaceColumn(colName, Series.fromDoubleArray(values, null, index(), colName, AndaTypes.FLOAT64))
    }

    private fun replaceColumn(colName: String, series: Series<Any>): DataFrame {
        val newData = data.toMutableMap()
        newData[colName] = series
        return DataFrame(newData, columns)
    }

    private fun numericPair(col1: String, col2: String): Pair<DoubleArray, DoubleArray> {
        val s1 = column(col1)
        val s2 = column(col2)
        if (s1.size() != s2.size()) {
            throw IllegalArgumentException("列长度不匹配: $col1 (${s1.size()}) vs $col2 (${s2.size()})")
        }
        return s1.toDoubleArrayNumeric() to s2.toDoubleArrayNumeric()
    }

    /**
     * 向量化加法
     */
    fun vectorizedAdd(col1: String, col2: String, resultCol: String): DataFrame {
        val (a, b) = numericPair(col1, col2)
        val result = if (a.size > 256 && NativeAvailability.available) {
            NativeMath.vectorizedAdd(a, b)
        } else {
            DoubleArray(a.size) { a[it] + b[it] }
        }
        return appendNumericColumn(resultCol, result)
    }

    /**
     * 向量化乘法
     */
    fun vectorizedMultiply(col1: String, col2: String, resultCol: String): DataFrame {
        val (a, b) = numericPair(col1, col2)
        val result = if (a.size > 256 && NativeAvailability.available) {
            NativeMath.vectorizedMultiply(a, b)
        } else {
            DoubleArray(a.size) { a[it] * b[it] }
        }
        return appendNumericColumn(resultCol, result)
    }

    /**
     * 计算点积
     */
    fun dotProduct(col1: String, col2: String): Double {
        val (a, b) = numericPair(col1, col2)
        if (a.size > 256 && NativeAvailability.available) {
            return NativeMath.dotProduct(a, b)
        }
        var sum = 0.0
        for (i in a.indices) sum += a[i] * b[i]
        return sum
    }

    /**
     * 计算范数
     */
    fun norm(colName: String): Double {
        val array = column(colName).toDoubleArrayNumeric()
        if (array.size > 256 && NativeAvailability.available) {
            return NativeMath.norm(array)
        }
        var sumSq = 0.0
        for (v in array) sumSq += v * v
        return kotlin.math.sqrt(sumSq)
    }

    /**
     * 排序（保留原索引；数值列按数值大小排序，空值排在最后）
     */
    fun sortValues(by: String, descending: Boolean = false): DataFrame {
        val positions = column(by).sortIndices(descending).toIntArray()
        return takeRows(positions)
    }

    /**
     * 布尔筛选（大于阈值）
     */
    fun filterGreaterThan(colName: String, threshold: Double): DataFrame =
        where(colName, threshold)

    /**
     * 查找空值索引
     */
    fun findNullIndices(colName: String): List<Int> = column(colName).findNullIndices()

    /**
     * 丢弃指定列为空的行
     */
    fun dropNullValues(colName: String): DataFrame {
        val positions = column(colName).nonNullPositions()
        return takeRows(positions)
    }

    /**
     * 填充空值
     */
    fun fillNull(colName: String, value: Any): DataFrame {
        val series = column(colName)
        if (series.nonNullCount() == series.size()) return this
        val newSeries: Series<Any> = if (value is Number && series.isNumericColumn()) {
            @Suppress("UNCHECKED_CAST")
            series.fillNullWithConstant(value.toDouble()) as Series<Any>
        } else {
            val values = ArrayList<Any?>(series.size())
            for (i in 0 until series.size()) {
                val v = series[i]
                values.add(v ?: value)
            }
            Series.wrap(values, index(), colName, series.dtype())
        }
        return replaceColumn(colName, newSeries)
    }

    /**
     * 分组求和（O(n) 哈希聚合）
     */
    fun groupBySum(groupCol: String, valueCol: String): DataFrame {
        val groupSeries = column(groupCol)
        val valueSeries = column(valueCol)
        val n = groupSeries.size()
        val sums = LinkedHashMap<Any?, Double>()

        for (i in 0 until n) {
            val key = groupSeries[i] ?: continue
            val value = valueSeries[i]
            if (value is Number) {
                sums[key] = (sums[key] ?: 0.0) + value.toDouble()
            }
        }

        val resultRows = ArrayList<Map<String, Any?>>(sums.size)
        for ((key, sum) in sums) {
            resultRows.add(mapOf(groupCol to key, valueCol to sum))
        }
        return DataFrame(resultRows)
    }

    /**
     * 排序索引
     */
    fun sortIndices(colName: String, descending: Boolean = false): List<Int> =
        column(colName).sortIndices(descending)

    /**
     * 数据合并（哈希连接，支持任意可哈希的键类型）
     */
    fun merge(other: DataFrame, on: String, how: String = "inner"): DataFrame =
        joinRows(other, on, how)

    /**
     * 布尔索引（列值大于阈值）
     */
    fun where(colName: String, threshold: Double): DataFrame {
        val series = column(colName)
        val positions = ArrayList<Int>()
        for (i in 0 until series.size()) {
            val v = series[i]
            if (v is Number && v.toDouble() > threshold) positions.add(i)
        }
        return takeRows(positions.toIntArray())
    }

    /**
     * 统计描述
     */
    fun describe(colName: String): Map<String, Double> = column(colName).describe()

    /**
     * 数据采样（保留原索引）
     */
    fun sample(sampleSize: Int): DataFrame {
        val n = shape().first
        if (n == 0 || sampleSize <= 0) return takeRows(IntArray(0))
        if (sampleSize >= n) return this

        val random = java.util.Random()
        val picked = java.util.HashSet<Int>(sampleSize * 2)
        while (picked.size < sampleSize) {
            picked.add(random.nextInt(n))
        }
        return takeRows(picked.toIntArray())
    }

    /**
     * 批量处理（兼容接口）
     */
    fun processBatch(colName: String, batchSize: Int = 1000): DataFrame {
        val series = column(colName)
        val values = series.toDoubleArrayNumeric()
        val result = if (NativeAvailability.available) {
            NativeBatch.processBatch(values, batchSize)
        } else {
            values
        }
        return replaceColumn(
            colName,
            Series.fromDoubleArray(result, null, index(), colName, AndaTypes.FLOAT64)
        )
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
    fun join(other: DataFrame, on: String, how: String = "inner"): DataFrame =
        joinRows(other, on, how)

    /**
     * 哈希连接：O(n + m + 输出行数)，避免原来的 O(n × m) 双重循环。
     * 支持 Int/String 等任意可哈希键，空键不参与匹配（与 SQL 语义一致）。
     */
    private fun joinRows(other: DataFrame, on: String, how: String = "inner"): DataFrame {
        if (on !in columns) throw IllegalArgumentException("列不存在: $on")
        if (on !in other.columns()) throw IllegalArgumentException("列不存在: $on")

        val joinType = how.lowercase()
        if (joinType !in setOf("inner", "left", "right", "outer")) {
            throw IllegalArgumentException("不支持的连接类型: $how")
        }

        val leftN = shape().first
        val rightN = other.shape().first
        val rightColumns = other.columns()
        val resultRows = ArrayList<Map<String, Any?>>()

        val rightMap = HashMap<Any, MutableList<Int>>()
        val rightKeys = arrayOfNulls<Any>(rightN)
        for (j in 0 until rightN) {
            val key = other.at(j, on)
            rightKeys[j] = key
            if (key != null) {
                rightMap.getOrPut(key) { ArrayList(1) }.add(j)
            }
        }

        val matchedRight = if (joinType == "outer") BooleanArray(rightN) else null

        // 左表驱动：inner / left / outer
        if (joinType != "right") {
            for (i in 0 until leftN) {
                val key = at(i, on)
                val matches = if (key == null) null else rightMap[key]
                if (!matches.isNullOrEmpty()) {
                    for (j in matches) {
                        resultRows.add(combinedRow(other, i, j, on))
                        matchedRight?.set(j, true)
                    }
                } else if (joinType == "left" || joinType == "outer") {
                    resultRows.add(combinedRow(other, i, -1, on))
                }
            }
        }

        // 右表驱动 / 全外连接补右表未匹配行
        if (joinType == "right" || joinType == "outer") {
            if (joinType == "right") {
                val leftMap = HashMap<Any, MutableList<Int>>()
                for (i in 0 until leftN) {
                    val key = at(i, on) ?: continue
                    leftMap.getOrPut(key) { ArrayList(1) }.add(i)
                }
                for (j in 0 until rightN) {
                    val key = rightKeys[j]
                    val matches = if (key == null) null else leftMap[key]
                    if (!matches.isNullOrEmpty()) {
                        for (i in matches) resultRows.add(combinedRow(other, i, j, on))
                    } else {
                        resultRows.add(combinedRow(other, -1, j, on))
                    }
                }
            } else {
                for (j in 0 until rightN) {
                    if (matchedRight != null && !matchedRight[j]) {
                        resultRows.add(combinedRow(other, -1, j, on))
                    }
                }
            }
        }

        return DataFrame(resultRows)
    }

    private fun combinedRow(
        other: DataFrame,
        leftPos: Int,
        rightPos: Int,
        on: String
    ): Map<String, Any?> {
        val row = LinkedHashMap<String, Any?>(columns.size + other.columns().size)
        if (leftPos >= 0) {
            for (colName in columns) row[colName] = at(leftPos, colName)
        } else {
            for (colName in columns) row[colName] = null
        }
        if (rightPos >= 0) {
            if (leftPos < 0) row[on] = other.at(rightPos, on)
            for (colName in other.columns()) {
                if (colName != on) row[colName] = other.at(rightPos, colName)
            }
        } else {
            for (colName in other.columns()) {
                if (colName != on) row[colName] = null
            }
        }
        return row
    }

    companion object {
        /**
         * 从CSV文件读取数据
         */
        /**
         * 从CSV文件读取数据（流式解析，内存占用与文件大小无关）
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
            return CsvReader.read(
                file,
                CsvReader.Options(
                    delimiter = delimiter,
                    header = header,
                    autoType = autoType,
                    encoding = encoding,
                    skipLines = skipLines,
                    nullValues = nullValues,
                    trimValues = trimValues
                )
            )
        }

        /** 库内部使用：从列式 Series 直接组装 DataFrame，避免再次转置。 */
        internal fun fromSeries(
            columnOrder: List<String>,
            seriesMap: Map<String, Series<Any>>
        ): DataFrame {
            return DataFrame(seriesMap, columnOrder)
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
        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(columns.joinToString(",") { escapeCsvField(it) })
            writer.newLine()

            val n = shape().first
            val line = StringBuilder(128)
            for (i in 0 until n) {
                line.setLength(0)
                for (c in columns.indices) {
                    if (c > 0) line.append(',')
                    val value = data[columns[c]]!![i]
                    if (value != null) line.append(escapeCsvField(value.toString()))
                }
                writer.write(line.toString())
                writer.newLine()
            }
        }
    }

    private fun escapeCsvField(value: String): String {
        var needsQuote = false
        for (ch in value) {
            if (ch == ',' || ch == '"' || ch == '\n' || ch == '\r') {
                needsQuote = true
                break
            }
        }
        if (!needsQuote) return value
        return buildString(value.length + 8) {
            append('"')
            for (ch in value) {
                if (ch == '"') append('"')
                append(ch)
            }
            append('"')
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
     * 聚合操作：一次哈希分组 + 单次扫描，复杂度 O(n × 非分组列数)。
     */
    fun agg(operations: Map<String, String>): DataFrame {
        if (groupCols.any { it !in df.columns() }) {
            throw IllegalArgumentException("分组列不存在: $groupCols")
        }

        val n = df.shape().first
        val positionsByKey = LinkedHashMap<List<Any?>, MutableList<Int>>()
        for (i in 0 until n) {
            val key = groupCols.map { df.at(i, it) }
            positionsByKey.getOrPut(key) { ArrayList() }.add(i)
        }

        val firstOp = operations.values.firstOrNull()?.lowercase()
        val nonGroupColumns = df.columns().filter { it !in groupCols }
        val resultRows = ArrayList<Map<String, Any?>>(positionsByKey.size)

        for ((key, positions) in positionsByKey) {
            val row = LinkedHashMap<String, Any?>(groupCols.size + nonGroupColumns.size)
            groupCols.forEachIndexed { index, colName -> row[colName] = key[index] }
            if (firstOp != null) {
                for (colName in nonGroupColumns) {
                    row[colName] = aggregate(df[colName], positions, firstOp)
                }
            }
            resultRows.add(row)
        }

        return DataFrame(resultRows)
    }

    private fun aggregate(series: Series<Any>, positions: List<Int>, op: String): Any? {
        var count = 0
        var sum = 0.0
        var numeric: Boolean? = null
        var minNumber = Double.POSITIVE_INFINITY
        var maxNumber = Double.NEGATIVE_INFINITY
        var minText: String? = null
        var maxText: String? = null

        for (p in positions) {
            val value = series[p] ?: continue
            count++
            if (numeric == null) numeric = value is Number
            if (numeric == true) {
                val d = (value as Number).toDouble()
                sum += d
                if (d < minNumber) minNumber = d
                if (d > maxNumber) maxNumber = d
            } else {
                val text = value.toString()
                if (minText == null || text < minText) minText = text
                if (maxText == null || text > maxText) maxText = text
            }
        }

        return when (op) {
            "count" -> count
            "sum" -> if (numeric == true && count > 0) sum else null
            "mean" -> if (numeric == true && count > 0) sum / count else null
            "min" -> when {
                count == 0 -> null
                numeric == true -> minNumber
                else -> minText
            }
            "max" -> when {
                count == 0 -> null
                numeric == true -> maxNumber
                else -> maxText
            }
            else -> throw IllegalArgumentException("不支持的聚合操作: $op")
        }
    }

    /**
     * 求和
     */
    fun sum(): DataFrame = agg(groupCols.associateWith { "sum" })

    /**
     * 平均值
     */
    fun mean(): DataFrame = agg(groupCols.associateWith { "mean" })

    /**
     * 计数
     */
    fun count(): DataFrame = agg(groupCols.associateWith { "count" })
}


// 扩展函数 - 便捷IO操作
fun DataFrame.saveToPrivateStorage(context: android.content.Context, fileName: String) {
    DataFrameIO.saveToPrivateStorage(context, fileName, this)
}

fun DataFrame.saveToExternalStorage(filePath: String) {
    DataFrameIO.saveToExternalStorage(filePath, this)
}
