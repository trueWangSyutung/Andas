package cn.ac.oac.libs.andas.utils

import cn.ac.oac.libs.andas.entity.DataFrame
import cn.ac.oac.libs.andas.entity.Series
import cn.ac.oac.libs.andas.core.NativeBatch
import cn.ac.oac.libs.andas.core.NativeData
import cn.ac.oac.libs.andas.core.NativeMath
import cn.ac.oac.libs.andas.types.AndaTypes

/**
 * 分批处理工具类
 * 提供大数据集的分批处理功能，支持内存友好的大规模数据处理
 */
object BatchUtils {
    
    /**
     * 默认批处理大小
     */
    const val DEFAULT_BATCH_SIZE = 1000
    
    /**
     * 检查原生批处理库是否可用
     */
    fun isNativeBatchAvailable(): Boolean {
        return NativeBatch.isAvailable()
    }
    
    /**
     * 对DataFrame的指定数值列进行分批求和
     * 
     * @param dataFrame 要处理的DataFrame
     * @param colName 要求和的列名
     * @param batchSize 批处理大小，默认1000
     * @return 求和结果
     */
    fun batchSum(dataFrame: DataFrame, colName: String, batchSize: Int = DEFAULT_BATCH_SIZE): Double {
        if (!dataFrame.isNativeAvailable()) {
            // 如果原生库不可用，使用普通求和
            return dataFrame.sum(colName)
        }
        
        val series = dataFrame[colName]
        val values = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (values.isEmpty()) return 0.0
        
        // 如果数据量小于批处理大小，直接计算
        if (values.size <= batchSize) {
            return values.sum()
        }
        
        // 分批处理
        var totalSum = 0.0
        for (i in values.indices step batchSize) {
            val end = minOf(i + batchSize, values.size)
            val batch = values.subList(i, end).toDoubleArray()
            totalSum += NativeBatch.processBatch(batch, batchSize).sum()
        }
        
        return totalSum
    }
    
    /**
     * 对DataFrame的多个数值列进行分批求和
     * 
     * @param dataFrame 要处理的DataFrame
     * @param colNames 要求和的列名列表
     * @param batchSize 批处理大小
     * @return 各列的求和结果，键为列名，值为求和结果
     */
    fun batchSumMultiple(
        dataFrame: DataFrame, 
        colNames: List<String>, 
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): Map<String, Double> {
        return colNames.associateWith { colName ->
            batchSum(dataFrame, colName, batchSize)
        }
    }
    
    /**
     * 对DataFrame的所有数值列进行分批求和
     * 
     * @param dataFrame 要处理的DataFrame
     * @param batchSize 批处理大小
     * @return 所有数值列的求和结果
     */
    fun batchSumAll(dataFrame: DataFrame, batchSize: Int = DEFAULT_BATCH_SIZE): Map<String, Double> {
        val numericColumns = dataFrame.columns().filter { colName ->
            val dtype = dataFrame.dtypes()[colName]
            dtype == AndaTypes.INT8 || dtype == AndaTypes.INT16 || 
            dtype == AndaTypes.INT32 || dtype == AndaTypes.INT64 ||
            dtype == AndaTypes.FLOAT32 || dtype == AndaTypes.FLOAT64
        }
        
        return batchSumMultiple(dataFrame, numericColumns, batchSize)
    }
    
    /**
     * 对Series进行分批求和
     * 
     * @param series 要处理的Series
     * @param batchSize 批处理大小
     * @return 求和结果
     */
    fun batchSum(series: Series<Any>, batchSize: Int = DEFAULT_BATCH_SIZE): Double {
        val values = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (values.isEmpty()) return 0.0
        
        // 如果数据量小于批处理大小，直接计算
        if (values.size <= batchSize) {
            return values.sum()
        }
        
        // 分批处理
        var totalSum = 0.0
        for (i in values.indices step batchSize) {
            val end = minOf(i + batchSize, values.size)
            val batch = values.subList(i, end).toDoubleArray()
            totalSum += NativeBatch.processBatch(batch, batchSize).sum()
        }
        
        return totalSum
    }
    
    /**
     * 分批计算均值
     * 
     * @param dataFrame 要处理的DataFrame
     * @param colName 要计算均值的列名
     * @param batchSize 批处理大小
     * @return 均值结果
     */
    fun batchMean(dataFrame: DataFrame, colName: String, batchSize: Int = DEFAULT_BATCH_SIZE): Double {
        val series = dataFrame[colName]
        val values = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (values.isEmpty()) return Double.NaN
        
        val totalSum = batchSum(dataFrame, colName, batchSize)
        return totalSum / values.size
    }
    
    /**
     * 分批计算标准差
     * 
     * @param dataFrame 要处理的DataFrame
     * @param colName 要计算标准差的列名
     * @param batchSize 批处理大小
     * @return 标准差结果
     */
    fun batchStd(dataFrame: DataFrame, colName: String, batchSize: Int = DEFAULT_BATCH_SIZE): Double {
        if (!dataFrame.isNativeAvailable()) {
            return dataFrame.std(colName)
        }
        
        val series = dataFrame[colName]
        val values = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (values.size < 2) return Double.NaN
        
        // 分批计算均值
        val mean = batchMean(dataFrame, colName, batchSize)
        
        // 分批计算方差
        var sumSquaredDiff = 0.0
        for (i in values.indices step batchSize) {
            val end = minOf(i + batchSize, values.size)
            val batch = values.subList(i, end).toDoubleArray()
            
            // 计算批次的平方差和
            val batchSumSquaredDiff = batch.sumOf { (it - mean) * (it - mean) }
            sumSquaredDiff += batchSumSquaredDiff
        }
        
        return kotlin.math.sqrt(sumSquaredDiff / (values.size - 1))
    }
    
    /**
     * 分批计算最小值
     * 
     * @param dataFrame 要处理的DataFrame
     * @param colName 要计算最小值的列名
     * @param batchSize 批处理大小
     * @return 最小值结果
     */
    fun batchMin(dataFrame: DataFrame, colName: String, batchSize: Int = DEFAULT_BATCH_SIZE): Double {
        if (!dataFrame.isNativeAvailable()) {
            return dataFrame.min(colName)
        }
        
        val series = dataFrame[colName]
        val values = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (values.isEmpty()) return Double.NaN
        
        // 分批计算最小值
        var minValue = Double.POSITIVE_INFINITY
        for (i in values.indices step batchSize) {
            val end = minOf(i + batchSize, values.size)
            val batch = values.subList(i, end).toDoubleArray()
            val batchMin = NativeBatch.processBatch(batch, batchSize).minOrNull() ?: continue
            minValue = minOf(minValue, batchMin)
        }
        
        return minValue
    }
    
    /**
     * 分批计算最大值
     * 
     * @param dataFrame 要处理的DataFrame
     * @param colName 要计算最大值的列名
     * @param batchSize 批处理大小
     * @return 最大值结果
     */
    fun batchMax(dataFrame: DataFrame, colName: String, batchSize: Int = DEFAULT_BATCH_SIZE): Double {
        if (!dataFrame.isNativeAvailable()) {
            return dataFrame.max(colName)
        }
        
        val series = dataFrame[colName]
        val values = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (values.isEmpty()) return Double.NaN
        
        // 分批计算最大值
        var maxValue = Double.NEGATIVE_INFINITY
        for (i in values.indices step batchSize) {
            val end = minOf(i + batchSize, values.size)
            val batch = values.subList(i, end).toDoubleArray()
            val batchMax = NativeBatch.processBatch(batch, batchSize).maxOrNull() ?: continue
            maxValue = maxOf(maxValue, batchMax)
        }
        
        return maxValue
    }
    
    /**
     * 分批计算描述性统计
     * 
     * @param dataFrame 要处理的DataFrame
     * @param colName 要计算统计的列名
     * @param batchSize 批处理大小
     * @return 包含count, mean, std, min, max的Map
     */
    fun batchDescribe(
        dataFrame: DataFrame, 
        colName: String, 
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): Map<String, Double> {
        val series = dataFrame[colName]
        val values = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (values.isEmpty()) {
            return mapOf(
                "count" to 0.0,
                "mean" to Double.NaN,
                "std" to Double.NaN,
                "min" to Double.NaN,
                "max" to Double.NaN
            )
        }
        
        val count = values.size.toDouble()
        val mean = batchMean(dataFrame, colName, batchSize)
        val std = batchStd(dataFrame, colName, batchSize)
        val min = batchMin(dataFrame, colName, batchSize)
        val max = batchMax(dataFrame, colName, batchSize)
        
        return mapOf(
            "count" to count,
            "mean" to mean,
            "std" to std,
            "min" to min,
            "max" to max
        )
    }
    
    /**
     * 分批向量化加法
     * 
     * @param dataFrame 要处理的DataFrame
     * @param col1 第一列名
     * @param col2 第二列名
     * @param resultCol 结果列名
     * @param batchSize 批处理大小
     * @return 包含结果列的新DataFrame
     */
    fun batchVectorizedAdd(
        dataFrame: DataFrame,
        col1: String,
        col2: String,
        resultCol: String,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): DataFrame {
        if (!dataFrame.isNativeAvailable()) {
            return dataFrame.vectorizedAdd(col1, col2, resultCol)
        }
        
        val series1 = dataFrame[col1]
        val series2 = dataFrame[col2]
        
        val values1 = series1.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        val values2 = series2.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (values1.size != values2.size) {
            throw IllegalArgumentException("列长度不匹配: $col1 (${values1.size}) vs $col2 (${values2.size})")
        }
        
        if (values1.isEmpty()) {
            return dataFrame
        }
        
        val resultValues = mutableListOf<Double>()
        
        // 分批处理
        for (i in values1.indices step batchSize) {
            val end = minOf(i + batchSize, values1.size)
            val batch1 = values1.subList(i, end).toDoubleArray()
            val batch2 = values2.subList(i, end).toDoubleArray()
            
            val batchResult = NativeMath.vectorizedAdd(batch1, batch2)
            resultValues.addAll(batchResult.toList())
        }
        
        // 通过添加列的方式创建新DataFrame
        val resultSeries = Series(resultValues.map { it as Any? }, dataFrame.index(), resultCol)
        return dataFrame.addColumn(resultCol, resultSeries.values())
    }
    
    /**
     * 分批向量化乘法
     * 
     * @param dataFrame 要处理的DataFrame
     * @param col1 第一列名
     * @param col2 第二列名
     * @param resultCol 结果列名
     * @param batchSize 批处理大小
     * @return 包含结果列的新DataFrame
     */
    fun batchVectorizedMultiply(
        dataFrame: DataFrame,
        col1: String,
        col2: String,
        resultCol: String,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): DataFrame {
        if (!dataFrame.isNativeAvailable()) {
            return dataFrame.vectorizedMultiply(col1, col2, resultCol)
        }
        
        val series1 = dataFrame[col1]
        val series2 = dataFrame[col2]
        
        val values1 = series1.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        val values2 = series2.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (values1.size != values2.size) {
            throw IllegalArgumentException("列长度不匹配: $col1 (${values1.size}) vs $col2 (${values2.size})")
        }
        
        if (values1.isEmpty()) {
            return dataFrame
        }
        
        val resultValues = mutableListOf<Double>()
        
        // 分批处理
        for (i in values1.indices step batchSize) {
            val end = minOf(i + batchSize, values1.size)
            val batch1 = values1.subList(i, end).toDoubleArray()
            val batch2 = values2.subList(i, end).toDoubleArray()
            
            val batchResult = NativeMath.vectorizedMultiply(batch1, batch2)
            resultValues.addAll(batchResult.toList())
        }
        
        // 通过添加列的方式创建新DataFrame
        val resultSeries = Series(resultValues.map { it as Any? }, dataFrame.index(), resultCol)
        return dataFrame.addColumn(resultCol, resultSeries.values())
    }
    
    /**
     * 分批计算点积
     * 
     * @param dataFrame 要处理的DataFrame
     * @param col1 第一列名
     * @param col2 第二列名
     * @param batchSize 批处理大小
     * @return 点积结果
     */
    fun batchDotProduct(
        dataFrame: DataFrame,
        col1: String,
        col2: String,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): Double {
        if (!dataFrame.isNativeAvailable()) {
            return dataFrame.dotProduct(col1, col2)
        }
        
        val series1 = dataFrame[col1]
        val series2 = dataFrame[col2]
        
        val values1 = series1.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        val values2 = series2.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (values1.size != values2.size) {
            throw IllegalArgumentException("列长度不匹配: $col1 (${values1.size}) vs $col2 (${values2.size})")
        }
        
        if (values1.isEmpty()) {
            return 0.0
        }
        
        var totalDotProduct = 0.0
        
        // 分批处理
        for (i in values1.indices step batchSize) {
            val end = minOf(i + batchSize, values1.size)
            val batch1 = values1.subList(i, end).toDoubleArray()
            val batch2 = values2.subList(i, end).toDoubleArray()
            
            val batchResult = NativeMath.dotProduct(batch1, batch2)
            totalDotProduct += batchResult
        }
        
        return totalDotProduct
    }
    
    /**
     * 分批计算范数
     * 
     * @param dataFrame 要处理的DataFrame
     * @param colName 要计算范数的列名
     * @param batchSize 批处理大小
     * @return 范数结果
     */
    fun batchNorm(
        dataFrame: DataFrame,
        colName: String,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): Double {
        if (!dataFrame.isNativeAvailable()) {
            return dataFrame.norm(colName)
        }
        
        val series = dataFrame[colName]
        val values = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (values.isEmpty()) {
            return 0.0
        }
        
        // 分批计算平方和
        var sumSquared = 0.0
        
        for (i in values.indices step batchSize) {
            val end = minOf(i + batchSize, values.size)
            val batch = values.subList(i, end).toDoubleArray()
            
            // 计算批次的平方和
            val batchSumSquared = batch.sumOf { it * it }
            sumSquared += batchSumSquared
        }
        
        return kotlin.math.sqrt(sumSquared)
    }
    
    /**
     * 分批归一化
     * 
     * @param dataFrame 要处理的DataFrame
     * @param colName 要归一化的列名
     * @param batchSize 批处理大小
     * @return 归一化后的DataFrame
     */
    fun batchNormalize(
        dataFrame: DataFrame,
        colName: String,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): DataFrame {
        if (!dataFrame.isNativeAvailable()) {
            return dataFrame.normalize(colName)
        }
        
        // 先计算均值和标准差
        val mean = batchMean(dataFrame, colName, batchSize)
        val std = batchStd(dataFrame, colName, batchSize)
        
        if (std == 0.0 || std.isNaN()) {
            return dataFrame
        }
        
        val series = dataFrame[colName]
        val values = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (values.isEmpty()) {
            return dataFrame
        }
        
        val normalizedValues = mutableListOf<Double>()
        
        // 分批归一化
        for (i in values.indices step batchSize) {
            val end = minOf(i + batchSize, values.size)
            val batch = values.subList(i, end).toDoubleArray()
            
            // 批次归一化
            val batchNormalized = batch.map { (it - mean) / std }
            normalizedValues.addAll(batchNormalized)
        }
        
        // 通过添加列的方式创建新DataFrame
        val normalizedSeries = Series(normalizedValues.map { it as Any? }, dataFrame.index(), colName)
        return dataFrame.addColumn(colName, normalizedSeries.values())
    }
    
    /**
     * 分批布尔筛选（大于阈值）
     * 
     * @param dataFrame 要处理的DataFrame
     * @param colName 要筛选的列名
     * @param threshold 阈值
     * @param batchSize 批处理大小
     * @return 筛选后的DataFrame
     */
    fun batchFilterGreaterThan(
        dataFrame: DataFrame,
        colName: String,
        threshold: Double,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): DataFrame {
        if (!dataFrame.isNativeAvailable()) {
            return dataFrame.filterGreaterThan(colName, threshold)
        }
        
        val series = dataFrame[colName]
        val values = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (values.isEmpty()) {
            return DataFrame(emptyList<Map<String, Any?>>())
        }
        
        val filteredIndices = mutableListOf<Int>()
        
        // 分批筛选
        for (i in values.indices step batchSize) {
            val end = minOf(i + batchSize, values.size)
            val batch = values.subList(i, end).toDoubleArray()
            
            val mask = NativeMath.greaterThan(batch, threshold)
            // 手动实现mapIndexedNotNull逻辑
            for (index in mask.indices) {
                if (mask[index]) {
                    filteredIndices.add(i + index)
                }
            }
        }
        
        // 构建结果DataFrame
        val filteredRows = filteredIndices.map { i ->
            dataFrame.columns().associate { colName -> 
                colName to dataFrame[colName][i] 
            }
        }
        
        return DataFrame(filteredRows)
    }
    
    /**
     * 分批处理统计描述（所有数值列）
     * 
     * @param dataFrame 要处理的DataFrame
     * @param batchSize 批处理大小
     * @return 所有数值列的统计描述
     */
    fun batchDescribeAll(
        dataFrame: DataFrame,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): Map<String, Map<String, Double>> {
        val numericColumns = dataFrame.columns().filter { colName ->
            val dtype = dataFrame.dtypes()[colName]
            dtype == AndaTypes.INT8 || dtype == AndaTypes.INT16 || 
            dtype == AndaTypes.INT32 || dtype == AndaTypes.INT64 ||
            dtype == AndaTypes.FLOAT32 || dtype == AndaTypes.FLOAT64
        }
        
        return numericColumns.associateWith { colName ->
            batchDescribe(dataFrame, colName, batchSize)
        }
    }
    
    /**
     * 分批处理数据采样
     * 
     * @param dataFrame 要处理的DataFrame
     * @param sampleSize 采样大小
     * @param batchSize 批处理大小
     * @return 采样后的DataFrame
     */
    fun batchSample(
        dataFrame: DataFrame,
        sampleSize: Int,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): DataFrame {
        if (!dataFrame.isNativeAvailable()) {
            return dataFrame.sample(sampleSize)
        }
        
        val indexList = dataFrame.index()
        if (indexList.isEmpty()) {
            return dataFrame
        }
        
        // 使用随机采样（简单实现）
        val totalSize = indexList.size
        if (sampleSize >= totalSize) {
            return dataFrame
        }
        
        // 生成随机索引
        val randomIndices = (0 until totalSize).shuffled().take(sampleSize).sorted()
        
        // 构建采样结果
        val sampledRows = randomIndices.map { i ->
            dataFrame.columns().associate { colName -> 
                colName to dataFrame[colName][i] 
            }
        }
        
        return DataFrame(sampledRows)
    }
    
    /**
     * 分批处理数据聚合（自定义聚合函数）
     * 
     * @param dataFrame 要处理的DataFrame
     * @param colName 要聚合的列名
     * @param aggregator 聚合函数 (batch: DoubleArray) -> Double
     * @param batchSize 批处理大小
     * @return 聚合结果
     */
    fun batchAggregate(
        dataFrame: DataFrame,
        colName: String,
        aggregator: (DoubleArray) -> Double,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): Double {
        val series = dataFrame[colName]
        val values = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (values.isEmpty()) {
            return Double.NaN
        }
        
        // 分批聚合
        var result = 0.0
        for (i in values.indices step batchSize) {
            val end = minOf(i + batchSize, values.size)
            val batch = values.subList(i, end).toDoubleArray()
            result += aggregator(batch)
        }
        
        return result
    }
    
    /**
     * 分批处理数据转换（自定义转换函数）
     * 
     * @param dataFrame 要处理的DataFrame
     * @param colName 要转换的列名
     * @param transformer 转换函数 (batch: DoubleArray) -> DoubleArray
     * @param batchSize 批处理大小
     * @return 转换后的DataFrame
     */
    fun batchTransform(
        dataFrame: DataFrame,
        colName: String,
        transformer: (DoubleArray) -> DoubleArray,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): DataFrame {
        val series = dataFrame[colName]
        val values = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (values.isEmpty()) {
            return dataFrame
        }
        
        val transformedValues = mutableListOf<Double>()
        
        // 分批转换
        for (i in values.indices step batchSize) {
            val end = minOf(i + batchSize, values.size)
            val batch = values.subList(i, end).toDoubleArray()
            val transformedBatch = transformer(batch)
            transformedValues.addAll(transformedBatch.toList())
        }
        
        // 通过添加列的方式创建新DataFrame
        val transformedSeries = Series(transformedValues.map { it as Any? }, dataFrame.index(), colName)
        return dataFrame.addColumn(colName, transformedSeries.values())
    }
    
    /**
     * 分批处理数据过滤（自定义过滤函数）
     * 
     * @param dataFrame 要处理的DataFrame
     * @param colName 要过滤的列名
     * @param filterPredicate 过滤函数 (batch: DoubleArray) -> BooleanArray
     * @param batchSize 批处理大小
     * @return 过滤后的DataFrame
     */
    fun batchFilter(
        dataFrame: DataFrame,
        colName: String,
        filterPredicate: (DoubleArray) -> BooleanArray,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): DataFrame {
        val series = dataFrame[colName]
        val values = series.values()
            .filterNotNull()
            .map { (it as Number).toDouble() }
        
        if (values.isEmpty()) {
            return DataFrame(emptyList<Map<String, Any?>>())
        }
        
        val filteredIndices = mutableListOf<Int>()
        
        // 分批过滤
        for (i in values.indices step batchSize) {
            val end = minOf(i + batchSize, values.size)
            val batch = values.subList(i, end).toDoubleArray()
            val mask = filterPredicate(batch)
            
            // 手动实现mapIndexedNotNull逻辑
            for (index in mask.indices) {
                if (mask[index]) {
                    filteredIndices.add(i + index)
                }
            }
        }
        
        // 构建结果DataFrame
        val filteredRows = filteredIndices.map { i ->
            dataFrame.columns().associate { colName -> 
                colName to dataFrame[colName][i] 
            }
        }
        
        return DataFrame(filteredRows)
    }
}
