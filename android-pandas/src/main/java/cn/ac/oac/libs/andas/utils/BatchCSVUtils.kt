package cn.ac.oac.libs.andas.utils

import cn.ac.oac.libs.andas.entity.DataFrame
import cn.ac.oac.libs.andas.entity.Series
import cn.ac.oac.libs.andas.core.NativeBatch
import cn.ac.oac.libs.andas.core.NativeData
import cn.ac.oac.libs.andas.core.NativeMath
import cn.ac.oac.libs.andas.types.AndaTypes
import cn.ac.oac.libs.andas.entity.DataFrameIO
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.math.sqrt

/**
 * 分批处理CSV工具类
 * 提供大数据 CSV 文件的分批处理功能
 */
object BatchCSVUtils {

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
     * 分批读取CSV文件（用于大数据处理）- 回调版本
     * 使用流式处理，避免一次性加载所有数据到内存
     * 一行一行读取，达到batchSize行后返回一个DataFrame
     *
     * @param inputStream CSV数据流
     * @param batchSize 批处理大小
     * @param callback 每批数据的处理回调函数
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     */
    fun readCSVBatch(
        inputStream: InputStream,
        batchSize: Int = 1000,
        callback: (DataFrame) -> Unit,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ) {
        if (batchSize == 0){
            throw IllegalArgumentException("Batch is Non-Zero!")
        }
        val reader = BufferedReader(InputStreamReader(inputStream, encoding))
        
        try {
            // 跳过指定行数
            var linesSkipped = 0
            while (linesSkipped < skipLines) {
                val line = reader.readLine()
                if (line == null) {
                    return // 文件为空或跳过行数超过文件行数
                }
                linesSkipped++
            }
            
            // 读取第一行（表头或第一行数据）
            val firstLine = reader.readLine()
            if (firstLine == null) {
                return // 文件为空
            }
            
            // 解析表头
            val headers: List<String>
            if (header) {
                headers = DataFrame.parseCSVLine(firstLine, delimiter, trimValues)
            } else {
                val firstLineValues = DataFrame.parseCSVLine(firstLine, delimiter, trimValues)
                headers = firstLineValues.indices.map { "col$it" }
            }
            
            if (headers.isEmpty()) {
                throw IllegalArgumentException("解析失败: 未找到有效的列名")
            }
            
            // 推断列类型（使用第一行数据）
            val columnTypes = mutableMapOf<String, (String) -> Any?>()
            if (autoType) {
                val firstDataRow = if (header) {
                    reader.readLine()
                } else {
                    firstLine
                }
                
                if (firstDataRow != null) {
                    val firstLineValues = DataFrame.parseCSVLine(firstDataRow, delimiter, trimValues)
                    val adjustedFirstValues = if (firstLineValues.size < headers.size) {
                        firstLineValues + List(headers.size - firstLineValues.size) { "" }
                    } else if (firstLineValues.size > headers.size) {
                        firstLineValues.take(headers.size)
                    } else {
                        firstLineValues
                    }
                    
                    headers.forEachIndexed { index, headerName ->
                        val firstValue = adjustedFirstValues.getOrNull(index) ?: ""
                        val inferredType = DataFrame.inferTypeFromSample(firstValue, nullValues)
                        columnTypes[headerName] = { value ->
                            if (value in nullValues) null
                            else DataFrame.convertValueWithType(value, inferredType)
                        }
                    }
                    
                   
                    processBatchLineByLine(reader, headers, batchSize, callback, delimiter, autoType, columnTypes, nullValues, trimValues)
                    return
                }
            }
            
            // 如果没有启用类型推断，直接开始逐行处理
            if (!autoType) {
                processBatchLineByLine(reader, headers, batchSize, callback, delimiter, autoType, columnTypes, nullValues, trimValues)
            }
            
        } catch (e: Exception) {
            throw RuntimeException("读取数据流失败: ${e.message}", e)
        } finally {
            reader.close()
        }
    }

    /**
     * 重新创建流进行分批处理（用于类型推断场景）
     */
    private fun readCSVBatchWithRecreate(
        inputStream: InputStream,
        batchSize: Int = 1000,
        callback: (DataFrame) -> Unit,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ) {
        // 重新创建reader（因为之前的reader已经关闭）
        val reader = BufferedReader(InputStreamReader(inputStream, encoding))
        
        try {
            // 跳过指定行数
            var linesSkipped = 0
            while (linesSkipped < skipLines) {
                val line = reader.readLine()
                if (line == null) return
                linesSkipped++
            }
            
            // 读取第一行
            val firstLine = reader.readLine()
            if (firstLine == null) return
            
            // 解析表头
            val headers: List<String>
            if (header) {
                headers = DataFrame.parseCSVLine(firstLine, delimiter, trimValues)
            } else {
                val firstLineValues = DataFrame.parseCSVLine(firstLine, delimiter, trimValues)
                headers = firstLineValues.indices.map { "col$it" }
            }
            
            // 推断类型（使用第一行数据）
            val columnTypes = mutableMapOf<String, (String) -> Any?>()
            if (autoType) {
                val firstDataRow = if (header) reader.readLine() else firstLine
                if (firstDataRow != null) {
                    val firstLineValues = DataFrame.parseCSVLine(firstDataRow, delimiter, trimValues)
                    val adjustedFirstValues = if (firstLineValues.size < headers.size) {
                        firstLineValues + List(headers.size - firstLineValues.size) { "" }
                    } else if (firstLineValues.size > headers.size) {
                        firstLineValues.take(headers.size)
                    } else {
                        firstLineValues
                    }
                    
                    headers.forEachIndexed { index, headerName ->
                        val firstValue = adjustedFirstValues.getOrNull(index) ?: ""
                        val inferredType = DataFrame.inferTypeFromSample(firstValue, nullValues)
                        columnTypes[headerName] = { value ->
                            if (value in nullValues) null
                            else DataFrame.convertValueWithType(value, inferredType)
                        }
                    }
                }
            }
            
            // 开始逐行处理
            processBatchLineByLine(reader, headers, batchSize, callback, delimiter, autoType, columnTypes, nullValues, trimValues)
            
        } catch (e: Exception) {
            throw RuntimeException("读取数据流失败: ${e.message}", e)
        } finally {
            reader.close()
        }
    }

    /**
     * 逐行处理批次数据 - 真正的流式处理
     */
    private fun processBatchLineByLine(
        reader: BufferedReader,
        headers: List<String>,
        batchSize: Int,
        callback: (DataFrame) -> Unit,
        delimiter: String,
        autoType: Boolean,
        columnTypes: Map<String, (String) -> Any?>,
        nullValues: List<String>,
        trimValues: Boolean
    ) {
        var currentBatch = mutableListOf<Map<String, Any?>>()
        
        reader.useLines { lines ->
            for (line in lines) {
                try {
                    val values = DataFrame.parseCSVLine(line, delimiter, trimValues)
                    
                    // 处理列数不匹配的情况
                    val adjustedValues = if (values.size < headers.size) {
                        values + List(headers.size - values.size) { "" }
                    } else if (values.size > headers.size) {
                        values.take(headers.size)
                    } else {
                        values
                    }
                    
                    val rowMap = headers.zip(adjustedValues).toMap()
                    
                    val processedRow = if (autoType) {
                        rowMap.mapValues { (key, value) ->
                            val converter = columnTypes[key]
                            if (converter != null) {
                                converter(value)
                            } else {
                                if (value in nullValues) null else value
                            }
                        }
                    } else {
                        rowMap.mapValues { (_, value) ->
                            if (value in nullValues) null else value
                        }
                    }
                    
                    currentBatch.add(processedRow)
                    
                    // 当批次达到指定大小时，调用回调函数
                    if (currentBatch.size >= batchSize) {
                        callback(DataFrame(currentBatch))
                        currentBatch.clear()
                    }
                } catch (e: Exception) {
                    throw RuntimeException("解析行失败: $line", e)
                }
            }
        }
        
        // 处理剩余数据
        if (currentBatch.isNotEmpty()) {
            callback(DataFrame(currentBatch))
        }
    }

    /**
     * 对CSV数据流的指定数值列进行分批求和
     *
     * @param inputStream CSV数据流
     * @param colName 要求和的列名
     * @param batchSize 批处理大小，默认1000
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 求和结果
     */
    fun batchSum(
        inputStream: InputStream,
        colName: String,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): Double {
        var totalSum = 0.0

        readCSVBatch(inputStream, batchSize, { batchDF ->
            // 使用与batchMean相同的逻辑：手动计算，避免原生库的差异
            val series = batchDF[colName]
            val values = series.values()
                .filterNotNull()
                .map { (it as Number).toDouble() }
            
            if (values.isNotEmpty()) {
                totalSum += values.sum()
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return totalSum
    }

    /**
     * 对CSV数据流的所有数值列进行分批求和
     *
     * @param inputStream CSV数据流
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 所有数值列的求和结果
     */
    fun batchSumAll(
        inputStream: InputStream,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): Map<String, Double> {
        val columnSums = mutableMapOf<String, Double>()
        val columnCounts = mutableMapOf<String, Int>()

        readCSVBatch(inputStream, batchSize, { batchDF ->
            if (batchDF.isNativeAvailable()) {
                batchDF.columns().forEach { colName ->
                    val dtype = batchDF.dtypes()[colName]
                    // 只处理数值列
                    if (dtype == AndaTypes.INT8 || dtype == AndaTypes.INT16 ||
                        dtype == AndaTypes.INT32 || dtype == AndaTypes.INT64 ||
                        dtype == AndaTypes.FLOAT32 || dtype == AndaTypes.FLOAT64) {
                        val sum = batchDF.sum(colName)
                        columnSums[colName] = (columnSums[colName] ?: 0.0) + sum
                        columnCounts[colName] = (columnCounts[colName] ?: 0) + 1
                    }
                }
            } else {
                batchDF.columns().forEach { colName ->
                    val series = batchDF[colName]
                    val values = series.values()
                        .filterNotNull()
                        .map { (it as Number).toDouble() }

                    if (values.isNotEmpty()) {
                        // 检查是否为数值列（通过第一个非空值判断）
                        val firstValue = series.values().firstOrNull { it != null }
                        if (firstValue is Number) {
                            val sum = values.sum()
                            columnSums[colName] = (columnSums[colName] ?: 0.0) + sum
                            columnCounts[colName] = (columnCounts[colName] ?: 0) + 1
                        }
                    }
                }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        // 返回平均值（处理多个批次的情况）
        return columnSums.mapValues { (colName, sum) ->
            val count = columnCounts[colName] ?: 1
            sum / count
        }
    }

    /**
     * 对CSV数据流的多个数值列进行分批求和
     *
     * @param inputStream CSV数据流
     * @param colNames 要求和的列名列表
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 各列的求和结果
     */
    fun batchSumMultiple(
        inputStream: InputStream,
        colNames: List<String>,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): Map<String, Double> {
        val columnSums = mutableMapOf<String, Double>()

        readCSVBatch(inputStream, batchSize, { batchDF ->
            colNames.forEach { colName ->
                // 使用与batchSum相同的逻辑：手动计算，避免原生库的差异
                val series = batchDF[colName]
                val values = series.values()
                    .filterNotNull()
                    .map { (it as Number).toDouble() }
                
                if (values.isNotEmpty()) {
                    columnSums[colName] = (columnSums[colName] ?: 0.0) + values.sum()
                }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return columnSums
    }

    /**
     * 对CSV数据流的指定列进行分批计算均值
     *
     * @param inputStream CSV数据流
     * @param colName 要计算均值的列名
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 均值结果
     */
    fun batchMean(
        inputStream: InputStream,
        colName: String,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): Double {
        var totalSum = 0.0
        var totalCount = 0L

        readCSVBatch(inputStream, batchSize, { batchDF ->
            // 使用与batchSum相同的逻辑：手动计算，避免原生库的差异
            val series = batchDF[colName]
            val values = series.values()
                .filterNotNull()
                .map { (it as Number).toDouble() }
            
            if (values.isNotEmpty()) {
                totalSum += values.sum()
                totalCount += values.size
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return if (totalCount > 0) totalSum / totalCount else Double.NaN
    }

    /**
     * 对CSV数据流的指定列进行分批计算标准差
     *
     * @param inputStream CSV数据流
     * @param colName 要计算标准差的列名
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 标准差结果
     */
    fun batchStd(
        inputStream: InputStream,
        colName: String,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): Double {
        // 使用流式处理计算标准差
        var count = 0L
        var sum = 0.0
        var sumSquared = 0.0

        readCSVBatch(inputStream, batchSize, { batchDF ->
            val series = batchDF[colName]
            val values = series.values()
                .filterNotNull()
                .map { (it as Number).toDouble() }

            if (values.isNotEmpty()) {
                count += values.size
                sum += values.sum()
                sumSquared += values.sumOf { it * it }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        if (count == 0L) {
            return Double.NaN
        }

        val mean = sum / count
        val variance = if (count > 1) (sumSquared - sum * sum / count) / (count - 1) else 0.0
        return if (variance > 0) kotlin.math.sqrt(variance) else 0.0
    }

    /**
     * 对CSV数据流的指定列进行分批计算最小值
     *
     * @param inputStream CSV数据流
     * @param colName 要计算最小值的列名
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 最小值结果
     */
    fun batchMin(
        inputStream: InputStream,
        colName: String,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): Double {
        var minValue = Double.POSITIVE_INFINITY
        var hasValue = false

        readCSVBatch(inputStream, batchSize, { batchDF ->
            val series = batchDF[colName]
            val values = series.values()
                .filterNotNull()
                .map { (it as Number).toDouble() }

            if (values.isNotEmpty()) {
                val batchMin = values.minOrNull()
                if (batchMin != null) {
                    minValue = minOf(minValue, batchMin)
                    hasValue = true
                }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return if (hasValue) minValue else Double.NaN
    }

    /**
     * 对CSV数据流的指定列进行分批计算最大值
     *
     * @param inputStream CSV数据流
     * @param colName 要计算最大值的列名
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 最大值结果
     */
    fun batchMax(
        inputStream: InputStream,
        colName: String,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): Double {
        var maxValue = Double.NEGATIVE_INFINITY
        var hasValue = false

        readCSVBatch(inputStream, batchSize, { batchDF ->
            val series = batchDF[colName]
            val values = series.values()
                .filterNotNull()
                .map { (it as Number).toDouble() }

            if (values.isNotEmpty()) {
                // 直接使用Kotlin标准库计算，避免NativeBatch的复杂性
                val batchMax = values.maxOrNull()
                if (batchMax != null) {
                    maxValue = maxOf(maxValue, batchMax)
                    hasValue = true
                }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return if (hasValue) maxValue else Double.NaN
    }

    /**
     * 对CSV数据流的指定列进行分批描述性统计
     *
     * @param inputStream CSV数据流
     * @param colName 要计算统计的列名
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 包含count, mean, std, min, max的Map
     */
    fun batchDescribe(
        inputStream: InputStream,
        colName: String,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): Map<String, Double> {
        // 使用流式处理收集统计信息
        var count = 0L
        var sum = 0.0
        var sumSquared = 0.0
        var minValue = Double.POSITIVE_INFINITY
        var maxValue = Double.NEGATIVE_INFINITY
        var hasValues = false

        readCSVBatch(inputStream, batchSize, { batchDF ->
            val series = batchDF[colName]
            val values = series.values()
                .filterNotNull()
                .map { (it as Number).toDouble() }

            if (values.isNotEmpty()) {
                hasValues = true
                count += values.size
                sum += values.sum()
                sumSquared += values.sumOf { it * it }
                minValue = minOf(minValue, values.minOrNull() ?: Double.POSITIVE_INFINITY)
                maxValue = maxOf(maxValue, values.maxOrNull() ?: Double.NEGATIVE_INFINITY)
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        if (!hasValues || count == 0L) {
            return mapOf(
                "count" to 0.0,
                "mean" to Double.NaN,
                "std" to Double.NaN,
                "min" to Double.NaN,
                "max" to Double.NaN
            )
        }

        val mean = sum / count
        val variance = if (count > 1) (sumSquared - sum * sum / count) / (count - 1) else 0.0
        val std = if (variance > 0) kotlin.math.sqrt(variance) else 0.0

        return mapOf(
            "count" to count.toDouble(),
            "mean" to mean,
            "std" to std,
            "min" to minValue,
            "max" to maxValue
        )
    }

    /**
     * 对CSV数据流的所有数值列进行分批描述性统计
     *
     * @param inputStream CSV数据流
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 所有数值列的统计描述
     */
    fun batchDescribeAll(
        inputStream: InputStream,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): Map<String, Map<String, Double>> {
        // 先收集所有列名和类型信息
        val columnStats = mutableMapOf<String, MutableMap<String, Double>>()
        val columnData = mutableMapOf<String, MutableList<Double>>()
        
        // 第一次遍历：收集数据和确定数值列
        var numericColumns: Set<String>? = null
        
        readCSVBatch(inputStream, batchSize, { batchDF ->
            if (numericColumns == null) {
                // 第一次处理，确定数值列
                numericColumns = batchDF.columns().filter { colName ->
                    val dtype = batchDF.dtypes()[colName]
                    dtype == AndaTypes.INT8 || dtype == AndaTypes.INT16 ||
                            dtype == AndaTypes.INT32 || dtype == AndaTypes.INT64 ||
                            dtype == AndaTypes.FLOAT32 || dtype == AndaTypes.FLOAT64
                }.toSet()
                
                // 初始化统计容器
                numericColumns!!.forEach { colName ->
                    columnStats[colName] = mutableMapOf(
                        "count" to 0.0,
                        "mean" to 0.0,
                        "std" to 0.0,
                        "min" to Double.POSITIVE_INFINITY,
                        "max" to Double.NEGATIVE_INFINITY
                    )
                    columnData[colName] = mutableListOf()
                }
            }
            
            // 收集数据
            numericColumns!!.forEach { colName ->
                val series = batchDF[colName]
                val values = series.values()
                    .filterNotNull()
                    .map { (it as Number).toDouble() }
                
                if (values.isNotEmpty()) {
                    columnData[colName]!!.addAll(values)
                }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)
        
        // 计算统计量
        val result = mutableMapOf<String, Map<String, Double>>()
        numericColumns?.forEach { colName ->
            val values = columnData[colName] ?: emptyList()
            if (values.isEmpty()) {
                result[colName] = mapOf(
                    "count" to 0.0,
                    "mean" to Double.NaN,
                    "std" to Double.NaN,
                    "min" to Double.NaN,
                    "max" to Double.NaN
                )
            } else {
                val count = values.size.toDouble()
                val sum = values.sum()
                val sumSquared = values.sumOf { it * it }
                val mean = sum / count
                val variance = if (count > 1) (sumSquared - sum * sum / count) / (count - 1) else 0.0
                val std = if (variance > 0) kotlin.math.sqrt(variance) else 0.0
                val min = values.minOrNull() ?: Double.NaN
                val max = values.maxOrNull() ?: Double.NaN
                
                result[colName] = mapOf(
                    "count" to count,
                    "mean" to mean,
                    "std" to std,
                    "min" to min,
                    "max" to max
                )
            }
        }
        
        return result
    }

    /**
     * 对CSV数据流的指定列进行分批归一化
     *
     * @param inputStream CSV数据流
     * @param colName 要归一化的列名
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 归一化后的DataFrame
     */
    fun batchNormalize(
        inputStream: InputStream,
        colName: String,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        // 使用流式处理计算均值和标准差
        var count = 0L
        var sum = 0.0
        var sumSquared = 0.0

        readCSVBatch(inputStream, batchSize, { batchDF ->
            val series = batchDF[colName]
            val values = series.values()
                .filterNotNull()
                .map { (it as Number).toDouble() }

            if (values.isNotEmpty()) {
                count += values.size
                sum += values.sum()
                sumSquared += values.sumOf { it * it }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        val mean = sum / count
        val variance = if (count > 1) (sumSquared - sum * sum / count) / (count - 1) else 0.0
        val std = if (variance > 0) kotlin.math.sqrt(variance) else 0.0

        // 重新读取数据并进行归一化
        val allRows = mutableListOf<Map<String, Any?>>()
        readCSVBatch(inputStream, batchSize, { batchDF ->
            val series = batchDF[colName]
            val values = series.values()
                .filterNotNull()
                .map { (it as Number).toDouble() }

            val indexList = batchDF.index()
            for (i in indexList.indices) {
                val originalRow = batchDF.iloc(i)
                val rowValues = originalRow.mapValues { (_, series) -> series?.get(0) }.toMutableMap()
                
                if (i < values.size) {
                    val normalizedValue = (values[i] - mean) / std
                    rowValues[colName] = normalizedValue
                }
                
                allRows.add(rowValues)
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return DataFrame(allRows)
    }

    /**
     * 对CSV数据流的指定列进行分批布尔筛选（大于阈值）
     *
     * @param inputStream CSV数据流
     * @param colName 要筛选的列名
     * @param threshold 阈值
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 筛选后的DataFrame
     */
    fun batchFilterGreaterThan(
        inputStream: InputStream,
        colName: String,
        threshold: Double,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        // 检查原生库是否可用
        val nativeAvailable = NativeBatch.isAvailable()
        
        val filteredRows = mutableListOf<Map<String, Any?>>()
        val filteredIndices = mutableListOf<Int>()
        var rowIndex = 0

        readCSVBatch(inputStream, batchSize, { batchDF ->
            if (nativeAvailable && batchDF.isNativeAvailable()) {
                // 使用原生库处理
                val series = batchDF[colName]
                val values = series.values()
                    .filterNotNull()
                    .map { (it as Number).toDouble() }
                    .toDoubleArray()

                if (values.isNotEmpty()) {
                    val mask = NativeMath.greaterThan(values, threshold)
                    for (index in mask.indices) {
                        if (mask[index]) {
                            filteredIndices.add(rowIndex + index)
                        }
                    }
                }
            } else {
                // 使用Kotlin标准库处理
                val indexList = batchDF.index()
                for (i in indexList.indices) {
                    val value = batchDF[colName][i]
                    if (value != null && (value as Number).toDouble() > threshold) {
                        filteredIndices.add(rowIndex + i)
                    }
                }
            }
            rowIndex += batchDF.shape().first
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        // 重新读取数据构建结果DataFrame
        if (filteredIndices.isEmpty()) {
            return DataFrame(emptyList<Map<String, Any?>>())
        }

        val targetIndices = filteredIndices.toSet()
        rowIndex = 0
        
        readCSVBatch(inputStream, batchSize, { batchDF ->
            val indexList = batchDF.index()
            for (i in indexList.indices) {
                val globalIndex = rowIndex + i
                if (globalIndex in targetIndices) {
                    val row = batchDF.iloc(i)
                    val rowValues = row.mapValues { (_, series) -> series?.get(0) }
                    filteredRows.add(rowValues)
                }
            }
            rowIndex += batchDF.shape().first
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return DataFrame(filteredRows)
    }

    /**
     * 对CSV数据流进行分批数据采样
     *
     * @param inputStream CSV数据流
     * @param sampleSize 采样大小
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 采样后的DataFrame
     */
    fun batchSample(
        inputStream: InputStream,
        sampleSize: Int,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        // 先统计总行数
        var totalRows = 0L
        readCSVBatch(inputStream, batchSize, { batchDF ->
            totalRows += batchDF.shape().first
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        if (totalRows == 0L) {
            return DataFrame(emptyList<Map<String, Any?>>())
        }

        // 生成随机索引
        val randomIndices = (0 until totalRows.toLong()).shuffled().take(sampleSize).sorted().toSet()

        // 重新读取数据并采样
        val sampledRows = mutableListOf<Map<String, Any?>>()
        var rowIndex = 0L

        readCSVBatch(inputStream, batchSize, { batchDF ->
            val indexList = batchDF.index()
            for (i in indexList.indices) {
                val globalIndex = rowIndex + i.toLong()  // 将 i 转换为 Long
                if (globalIndex in randomIndices) {
                    val row = batchDF.iloc(i)
                    val rowValues = row.mapValues { (_, series) -> series?.get(0) }
                    sampledRows.add(rowValues)
                }
            }

            rowIndex += batchDF.shape().first
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return DataFrame(sampledRows)
    }

    /**
     * 对CSV数据流的指定列进行分批自定义聚合
     *
     * @param inputStream CSV数据流
     * @param colName 要聚合的列名
     * @param aggregator 聚合函数 (batch: DoubleArray) -> Double
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 聚合结果
     */
    fun batchAggregate(
        inputStream: InputStream,
        colName: String,
        aggregator: (DoubleArray) -> Double,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): Double {
        var result = 0.0
        var hasValues = false
        
        readCSVBatch(inputStream, batchSize, { batchDF ->
            val series = batchDF[colName]
            val values = series.values()
                .filterNotNull()
                .map { (it as Number).toDouble() }

            if (values.isNotEmpty()) {
                hasValues = true
                val batch = values.toDoubleArray()
                result += aggregator(batch)
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return if (hasValues) result else Double.NaN
    }

    /**
     * 对CSV数据流的指定列进行分批自定义转换
     *
     * @param inputStream CSV数据流
     * @param colName 要转换的列名
     * @param transformer 转换函数 (batch: DoubleArray) -> DoubleArray
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 转换后的DataFrame
     */
    fun batchTransform(
        inputStream: InputStream,
        colName: String,
        transformer: (DoubleArray) -> DoubleArray,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        // 先收集所有数据进行转换
        val allRows = mutableListOf<Map<String, Any?>>()
        
        readCSVBatch(inputStream, batchSize, { batchDF ->
            val series = batchDF[colName]
            val values = series.values()
                .filterNotNull()
                .map { (it as Number).toDouble() }

            if (values.isNotEmpty()) {
                val transformedBatch = transformer(values.toDoubleArray())
                
                val indexList = batchDF.index()
                for (i in indexList.indices) {
                    val originalRow = batchDF.iloc(i)
                    val rowValues = originalRow.mapValues { (_, series) -> series?.get(0) }.toMutableMap()
                    
                    if (i < transformedBatch.size) {
                        rowValues[colName] = transformedBatch[i]
                    }
                    
                    allRows.add(rowValues)
                }
            } else {
                // 如果没有值，直接添加原始行
                val indexList = batchDF.index()
                for (i in indexList.indices) {
                    val originalRow = batchDF.iloc(i)
                    val rowValues = originalRow.mapValues { (_, series) -> series?.get(0) }
                    allRows.add(rowValues)
                }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return DataFrame(allRows)
    }

    /**
     * 对CSV数据流的指定列进行分批自定义过滤
     *
     * @param inputStream CSV数据流
     * @param colName 要过滤的列名
     * @param filterPredicate 过滤函数 (batch: DoubleArray) -> BooleanArray
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 过滤后的DataFrame
     */
    fun batchFilter(
        inputStream: InputStream,
        colName: String,
        filterPredicate: (DoubleArray) -> BooleanArray,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        val filteredRows = mutableListOf<Map<String, Any?>>()
        val filteredIndices = mutableListOf<Int>()
        var rowIndex = 0

        readCSVBatch(inputStream, batchSize, { batchDF ->
            val series = batchDF[colName]
            val values = series.values()
                .filterNotNull()
                .map { (it as Number).toDouble() }

            if (values.isNotEmpty()) {
                val mask = filterPredicate(values.toDoubleArray())
                for (index in mask.indices) {
                    if (mask[index]) {
                        filteredIndices.add(rowIndex + index)
                    }
                }
            }
            rowIndex += batchDF.shape().first
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        // 重新读取数据构建结果DataFrame
        if (filteredIndices.isEmpty()) {
            return DataFrame(emptyList<Map<String, Any?>>())
        }

        val targetIndices = filteredIndices.toSet()
        rowIndex = 0
        
        readCSVBatch(inputStream, batchSize, { batchDF ->
            val indexList = batchDF.index()
            for (i in indexList.indices) {
                val globalIndex = rowIndex + i
                if (globalIndex in targetIndices) {
                    val row = batchDF.iloc(i)
                    val rowValues = row.mapValues { (_, series) -> series?.get(0) }
                    filteredRows.add(rowValues)
                }
            }
            rowIndex += batchDF.shape().first
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return DataFrame(filteredRows)
    }

    /**
     * 对CSV数据流进行分批向量化加法
     *
     * @param inputStream CSV数据流
     * @param col1 第一列名
     * @param col2 第二列名
     * @param resultCol 结果列名
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 包含结果列的新DataFrame
     */
    fun batchVectorizedAdd(
        inputStream: InputStream,
        col1: String,
        col2: String,
        resultCol: String,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        // 检查原生库是否可用
        val nativeAvailable = NativeBatch.isAvailable()
        
        val resultRows = mutableListOf<Map<String, Any?>>()

        readCSVBatch(inputStream, batchSize, { batchDF ->
            if (nativeAvailable && batchDF.isNativeAvailable()) {
                val series1 = batchDF[col1]
                val series2 = batchDF[col2]

                val values1 = series1.values()
                    .filterNotNull()
                    .map { (it as Number).toDouble() }
                    .toDoubleArray()

                val values2 = series2.values()
                    .filterNotNull()
                    .map { (it as Number).toDouble() }
                    .toDoubleArray()

                if (values1.isNotEmpty() && values2.isNotEmpty()) {
                    val batchResult = NativeMath.vectorizedAdd(values1, values2)
                    
                    val indexList = batchDF.index()
                    for (i in indexList.indices) {
                        val originalRow = batchDF.iloc(i)
                        val rowValues = originalRow.mapValues { (_, series) -> series?.get(0) }.toMutableMap()
                        
                        if (i < batchResult.size) {
                            rowValues[resultCol] = batchResult[i]
                        }
                        
                        resultRows.add(rowValues)
                    }
                } else {
                    // 如果没有值，直接添加原始行
                    val indexList = batchDF.index()
                    for (i in indexList.indices) {
                        val originalRow = batchDF.iloc(i)
                        val rowValues = originalRow.mapValues { (_, series) -> series?.get(0) }
                        resultRows.add(rowValues)
                    }
                }
            } else {
                // 使用Kotlin标准库处理
                val indexList = batchDF.index()
                for (i in indexList.indices) {
                    val originalRow = batchDF.iloc(i)
                    val rowValues = originalRow.mapValues { (_, series) -> series?.get(0) }.toMutableMap()
                    
                    val value1 = batchDF[col1][i]
                    val value2 = batchDF[col2][i]
                    
                    if (value1 != null && value2 != null) {
                        rowValues[resultCol] = (value1 as Number).toDouble() + (value2 as Number).toDouble()
                    }
                    
                    resultRows.add(rowValues)
                }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return DataFrame(resultRows)
    }

    /**
     * 对CSV数据流进行分批向量化乘法
     *
     * @param inputStream CSV数据流
     * @param col1 第一列名
     * @param col2 第二列名
     * @param resultCol 结果列名
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 包含结果列的新DataFrame
     */
    fun batchVectorizedMultiply(
        inputStream: InputStream,
        col1: String,
        col2: String,
        resultCol: String,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        // 检查原生库是否可用
        val nativeAvailable = NativeBatch.isAvailable()
        
        val resultRows = mutableListOf<Map<String, Any?>>()

        readCSVBatch(inputStream, batchSize, { batchDF ->
            if (nativeAvailable && batchDF.isNativeAvailable()) {
                val series1 = batchDF[col1]
                val series2 = batchDF[col2]

                val values1 = series1.values()
                    .filterNotNull()
                    .map { (it as Number).toDouble() }
                    .toDoubleArray()

                val values2 = series2.values()
                    .filterNotNull()
                    .map { (it as Number).toDouble() }
                    .toDoubleArray()

                if (values1.isNotEmpty() && values2.isNotEmpty()) {
                    val batchResult = NativeMath.vectorizedMultiply(values1, values2)
                    
                    val indexList = batchDF.index()
                    for (i in indexList.indices) {
                        val originalRow = batchDF.iloc(i)
                        val rowValues = originalRow.mapValues { (_, series) -> series?.get(0) }.toMutableMap()
                        
                        if (i < batchResult.size) {
                            rowValues[resultCol] = batchResult[i]
                        }
                        
                        resultRows.add(rowValues)
                    }
                } else {
                    // 如果没有值，直接添加原始行
                    val indexList = batchDF.index()
                    for (i in indexList.indices) {
                        val originalRow = batchDF.iloc(i)
                        val rowValues = originalRow.mapValues { (_, series) -> series?.get(0) }
                        resultRows.add(rowValues)
                    }
                }
            } else {
                // 使用Kotlin标准库处理
                val indexList = batchDF.index()
                for (i in indexList.indices) {
                    val originalRow = batchDF.iloc(i)
                    val rowValues = originalRow.mapValues { (_, series) -> series?.get(0) }.toMutableMap()
                    
                    val value1 = batchDF[col1][i]
                    val value2 = batchDF[col2][i]
                    
                    if (value1 != null && value2 != null) {
                        rowValues[resultCol] = (value1 as Number).toDouble() * (value2 as Number).toDouble()
                    }
                    
                    resultRows.add(rowValues)
                }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return DataFrame(resultRows)
    }

    /**
     * 对CSV数据流进行分批计算点积
     *
     * @param inputStream CSV数据流
     * @param col1 第一列名
     * @param col2 第二列名
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 点积结果
     */
    fun batchDotProduct(
        inputStream: InputStream,
        col1: String,
        col2: String,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): Double {
        // 检查原生库是否可用
        val nativeAvailable = NativeBatch.isAvailable()
        
        var totalDotProduct = 0.0

        readCSVBatch(inputStream, batchSize, { batchDF ->
            if (nativeAvailable && batchDF.isNativeAvailable()) {
                val series1 = batchDF[col1]
                val series2 = batchDF[col2]

                val values1 = series1.values()
                    .filterNotNull()
                    .map { (it as Number).toDouble() }
                    .toDoubleArray()

                val values2 = series2.values()
                    .filterNotNull()
                    .map { (it as Number).toDouble() }
                    .toDoubleArray()

                if (values1.isNotEmpty() && values2.isNotEmpty()) {
                    val batchResult = NativeMath.dotProduct(values1, values2)
                    totalDotProduct += batchResult
                }
            } else {
                // 使用Kotlin标准库处理
                val indexList = batchDF.index()
                for (i in indexList.indices) {
                    val value1 = batchDF[col1][i]
                    val value2 = batchDF[col2][i]
                    
                    if (value1 != null && value2 != null) {
                        totalDotProduct += (value1 as Number).toDouble() * (value2 as Number).toDouble()
                    }
                }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return totalDotProduct
    }

    /**
     * 对CSV数据流的指定列进行分批计算范数
     *
     * @param inputStream CSV数据流
     * @param colName 要计算范数的列名
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 范数结果
     */
    fun batchNorm(
        inputStream: InputStream,
        colName: String,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): Double {
        // 检查原生库是否可用
        val nativeAvailable = NativeBatch.isAvailable()
        
        var sumSquared = 0.0
        var hasValues = false

        readCSVBatch(inputStream, batchSize, { batchDF ->
            if (nativeAvailable && batchDF.isNativeAvailable()) {
                val series = batchDF[colName]
                val values = series.values()
                    .filterNotNull()
                    .map { (it as Number).toDouble() }
                    .toDoubleArray()

                if (values.isNotEmpty()) {
                    hasValues = true
                    val batchSumSquared = values.sumOf { it * it }
                    sumSquared += batchSumSquared
                }
            } else {
                // 使用Kotlin标准库处理
                val indexList = batchDF.index()
                for (i in indexList.indices) {
                    val value = batchDF[colName][i]
                    if (value != null) {
                        hasValues = true
                        val doubleValue = (value as Number).toDouble()
                        sumSquared += doubleValue * doubleValue
                    }
                }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return if (hasValues) sqrt(sumSquared) else 0.0
    }

    /**
     * 对CSV数据流进行分批计数（统计非空值数量）
     *
     * @param inputStream CSV数据流
     * @param colName 要计数的列名
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 非空值数量
     */
    fun batchCount(
        inputStream: InputStream,
        colName: String,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): Long {
        var count = 0L

        readCSVBatch(inputStream, batchSize, { batchDF ->
            val series = batchDF[colName]
            count += series.values().count { it != null }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return count
    }

    /**
     * 对CSV数据流进行分批唯一值计数
     *
     * @param inputStream CSV数据流
     * @param colName 要计数的列名
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 唯一值数量
     */
    fun batchNunique(
        inputStream: InputStream,
        colName: String,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): Int {
        val uniqueValues = mutableSetOf<Any?>()

        readCSVBatch(inputStream, batchSize, { batchDF ->
            val series = batchDF[colName]
            series.values().forEach { value ->
                if (value != null) {
                    uniqueValues.add(value)
                }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return uniqueValues.size
    }

    /**
     * 对CSV数据流进行分批分组计数
     *
     * @param inputStream CSV数据流
     * @param groupCol 分组列名
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 各分组的计数
     */
    fun batchGroupByCount(
        inputStream: InputStream,
        groupCol: String,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): Map<Any?, Long> {
        val groupCounts = mutableMapOf<Any?, Long>()

        readCSVBatch(inputStream, batchSize, { batchDF ->
            val groupSeries = batchDF[groupCol]
            groupSeries.values().forEach { value ->
                if (value != null) {
                    groupCounts[value] = (groupCounts[value] ?: 0L) + 1L
                }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return groupCounts
    }

    /**
     * 对CSV数据流进行分批分组求和
     *
     * @param inputStream CSV数据流
     * @param groupCol 分组列名
     * @param valueCol 数值列名
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 各分组的求和结果
     */
    fun batchGroupBySum(
        inputStream: InputStream,
        groupCol: String,
        valueCol: String,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): Map<Any?, Double> {
        val groupSums = mutableMapOf<Any?, Double>()

        readCSVBatch(inputStream, batchSize, { batchDF ->
            val groupSeries = batchDF[groupCol]
            val valueSeries = batchDF[valueCol]

            val indexList = batchDF.index()
            for (i in indexList.indices) {
                val groupKey = groupSeries[i]
                val value = valueSeries[i]

                if (groupKey != null && value != null) {
                    val currentValue = groupSums[groupKey] ?: 0.0
                    groupSums[groupKey] = currentValue + (value as Number).toDouble()
                }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return groupSums
    }

    /**
     * 对CSV数据流进行分批分组均值计算
     *
     * @param inputStream CSV数据流
     * @param groupCol 分组列名
     * @param valueCol 数值列名
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 各分组的均值结果
     */
    fun batchGroupByMean(
        inputStream: InputStream,
        groupCol: String,
        valueCol: String,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): Map<Any?, Double> {
        val groupSums = mutableMapOf<Any?, Double>()
        val groupCounts = mutableMapOf<Any?, Long>()

        readCSVBatch(inputStream, batchSize, { batchDF ->
            val groupSeries = batchDF[groupCol]
            val valueSeries = batchDF[valueCol]

            val indexList = batchDF.index()
            for (i in indexList.indices) {
                val groupKey = groupSeries[i]
                val value = valueSeries[i]

                if (groupKey != null && value != null) {
                    val currentValue = groupSums[groupKey] ?: 0.0
                    groupSums[groupKey] = currentValue + (value as Number).toDouble()
                    groupCounts[groupKey] = (groupCounts[groupKey] ?: 0L) + 1L
                }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        // 计算均值
        return groupSums.mapValues { (key, sum) ->
            val count = groupCounts[key] ?: 1L
            sum / count
        }
    }

    /**
     * 对CSV数据流进行分批排序
     *
     * @param inputStream CSV数据流
     * @param colName 排序列名
     * @param descending 是否降序
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 排序后的DataFrame
     */
    fun batchSort(
        inputStream: InputStream,
        colName: String,
        descending: Boolean = false,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        // 先收集所有数据和索引
        val allData = mutableListOf<Pair<Int, Double>>()
        var rowIndex = 0
        
        readCSVBatch(inputStream, batchSize, { batchDF ->
            if (batchDF.isNativeAvailable()) {
                val series = batchDF[colName]
                val values = series.values()
                    .filterNotNull()
                    .map { (it as Number).toDouble() }
                
                values.forEachIndexed { index, value ->
                    allData.add(Pair(rowIndex + index, value))
                }
            } else {
                val indexList = batchDF.index()
                for (i in indexList.indices) {
                    val value = batchDF[colName][i]
                    if (value != null) {
                        allData.add(Pair(rowIndex + i, (value as Number).toDouble()))
                    }
                }
            }
            rowIndex += batchDF.shape().first
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        // 排序
        val sortedData = if (descending) {
            allData.sortedByDescending { it.second }
        } else {
            allData.sortedBy { it.second }
        }

        val sortedIndices = sortedData.map { it.first }.toSet()

        // 重新读取数据构建结果DataFrame
        val sortedRows = mutableListOf<Map<String, Any?>>()
        rowIndex = 0
        
        readCSVBatch(inputStream, batchSize, { batchDF ->
            val indexList = batchDF.index()
            for (i in indexList.indices) {
                val globalIndex = rowIndex + i
                if (globalIndex in sortedIndices) {
                    val row = batchDF.iloc(i)
                    val rowValues = row.mapValues { (_, series) -> series?.get(0) }
                    sortedRows.add(rowValues)
                }
            }
            rowIndex += batchDF.shape().first
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        // 按排序顺序重新排列
        val indexMap = sortedRows.mapIndexed { index, row -> 
            val globalIndex = sortedIndices.elementAt(index)
            globalIndex to row
        }.toMap()

        val finalRows = sortedData.map { (globalIndex, _) -> indexMap[globalIndex]!! }

        return DataFrame(finalRows)
    }

    /**
     * 对CSV数据流进行分批去重
     *
     * @param inputStream CSV数据流
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 去重后的DataFrame
     */
    fun batchDropDuplicates(
        inputStream: InputStream,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        val seenRows = mutableSetOf<String>()
        val uniqueRows = mutableListOf<Map<String, Any?>>()

        readCSVBatch(inputStream, batchSize, { batchDF ->
            val indexList = batchDF.index()
            for (i in indexList.indices) {
                val row = batchDF.iloc(i)
                val rowString = row.values.joinToString(",") { it?.toString() ?: "" }

                if (!seenRows.contains(rowString)) {
                    seenRows.add(rowString)
                    val rowValues = row.mapValues { (_, series) -> series?.get(0) }
                    uniqueRows.add(rowValues)
                }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return DataFrame(uniqueRows)
    }

    /**
     * 对CSV数据流进行分批条件筛选（支持复杂条件）
     *
     * @param inputStream CSV数据流
     * @param condition 条件函数，接受行数据返回是否保留
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 筛选后的DataFrame
     */
    fun batchFilterWithCondition(
        inputStream: InputStream,
        condition: (Map<String, Any?>) -> Boolean,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        val filteredRows = mutableListOf<Map<String, Any?>>()

        readCSVBatch(inputStream, batchSize, { batchDF ->
            val indexList = batchDF.index()
            for (i in indexList.indices) {
                val row = batchDF.iloc(i)
                val rowValues = row.mapValues { (_, series) -> series?.get(0) }

                if (condition(rowValues)) {
                    filteredRows.add(rowValues)
                }
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return DataFrame(filteredRows)
    }

    /**
     * 对CSV数据流进行分批窗口函数（滑动窗口）
     *
     * @param inputStream CSV数据流
     * @param colName 要处理的列名
     * @param windowSize 窗口大小
     * @param operation 窗口操作函数
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 包含窗口计算结果的新DataFrame
     */
    fun batchWindowOperation(
        inputStream: InputStream,
        colName: String,
        windowSize: Int,
        operation: (DoubleArray) -> Double,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        // 先收集所有数据
        val allValues = mutableListOf<Double>()
        val allRows = mutableListOf<Map<String, Any?>>()
        
        readCSVBatch(inputStream, batchSize, { batchDF ->
            val series = batchDF[colName]
            val values = series.values()
                .filterNotNull()
                .map { (it as Number).toDouble() }
            
            allValues.addAll(values)
            
            // 同时收集所有行数据
            val indexList = batchDF.index()
            for (i in indexList.indices) {
                val originalRow = batchDF.iloc(i)
                val rowValues = originalRow.mapValues { (_, series) -> series?.get(0) }
                allRows.add(rowValues)
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        if (allValues.isEmpty()) {
            return DataFrame(allRows)
        }

        val windowResults = mutableListOf<Double?>()

        // 计算窗口结果
        for (i in allValues.indices) {
            val start = maxOf(0, i - windowSize + 1)
            val end = i + 1
            val window = allValues.subList(start, end).toDoubleArray()
            val result = operation(window)
            windowResults.add(result)
        }

        // 添加结果列到所有行
        val resultRows = allRows.mapIndexed { index, row ->
            row.toMutableMap().apply {
                put("${colName}_window_${windowSize}", windowResults[index])
            }
        }

        return DataFrame(resultRows)
    }

    /**
     * 对CSV数据流进行分批累积计算（如累积和、累积均值）
     *
     * @param inputStream CSV数据流
     * @param colName 要处理的列名
     * @param operation 累积操作类型： "sum", "mean", "min", "max"
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 包含累积结果的新DataFrame
     */
    fun batchCumulativeOperation(
        inputStream: InputStream,
        colName: String,
        operation: String = "sum",
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        // 先收集所有数据和行信息
        val allValues = mutableListOf<Double>()
        val allRows = mutableListOf<Map<String, Any?>>()
        
        readCSVBatch(inputStream, batchSize, { batchDF ->
            val series = batchDF[colName]
            val values = series.values()
                .filterNotNull()
                .map { (it as Number).toDouble() }
            
            allValues.addAll(values)
            
            // 收集所有行数据
            val indexList = batchDF.index()
            for (i in indexList.indices) {
                val originalRow = batchDF.iloc(i)
                val rowValues = originalRow.mapValues { (_, series) -> series?.get(0) }
                allRows.add(rowValues)
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        if (allValues.isEmpty()) {
            return DataFrame(allRows)
        }

        val cumulativeResults = mutableListOf<Double>()
        var runningValue = 0.0
        var runningMin = Double.POSITIVE_INFINITY
        var runningMax = Double.NEGATIVE_INFINITY
        var count = 0

        // 计算累积结果
        for (value in allValues) {
            count++
            when (operation.lowercase()) {
                "sum" -> {
                    runningValue += value
                    cumulativeResults.add(runningValue)
                }
                "mean" -> {
                    runningValue += value
                    cumulativeResults.add(runningValue / count)
                }
                "min" -> {
                    runningMin = minOf(runningMin, value)
                    cumulativeResults.add(runningMin)
                }
                "max" -> {
                    runningMax = maxOf(runningMax, value)
                    cumulativeResults.add(runningMax)
                }
                else -> throw IllegalArgumentException("不支持的累积操作: $operation")
            }
        }

        // 添加结果列到所有行
        val resultRows = allRows.mapIndexed { index, row ->
            row.toMutableMap().apply {
                put("${colName}_cumulative_${operation}", cumulativeResults[index])
            }
        }

        return DataFrame(resultRows)
    }

    /**
     * 对CSV数据流进行分批数据聚合（多列多操作）
     *
     * @param inputStream CSV数据流
     * @param operations 操作映射：列名 -> 操作类型列表
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 聚合结果DataFrame
     */
    fun batchAggregateMultiple(
        inputStream: InputStream,
        operations: Map<String, List<String>>,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        val results = mutableMapOf<String, List<Double>>()
        val rowLabels = mutableListOf<String>()

        // 为每个列和操作组合初始化结果
        operations.forEach { (colName, ops) ->
            ops.forEach { op ->
                rowLabels.add("${colName}_${op}")
                results["${colName}_${op}"] = emptyList()
            }
        }

        // 分批处理
        readCSVBatch(inputStream, batchSize, { batchDF ->
            operations.forEach { (colName, ops) ->
                ops.forEach { op ->
                    val value = when (op.lowercase()) {
                        "sum" -> batchDF.sum(colName)
                        "mean" -> batchDF.mean(colName)
                        "min" -> batchDF.min(colName)
                        "max" -> batchDF.max(colName)
                        "std" -> batchDF.std(colName)
                        "count" -> batchDF[colName].values().count { it != null }.toDouble()
                        else -> Double.NaN
                    }
                    val key = "${colName}_${op}"
                    results[key] = results[key]!! + value
                }
            }
        })

        // 计算最终结果（对批次结果进行聚合）
        val finalResults = results.mapValues { (_, values) ->
            when {
                values.isEmpty() -> Double.NaN
                else -> values.average()
            }
        }

        // 转换为DataFrame格式
        val resultRows = rowLabels.map { label ->
            mapOf("operation" to label, "value" to (finalResults[label] ?: Double.NaN))
        }

        return DataFrame(resultRows)
    }

    /**
     * 对CSV数据流进行分批数据转换（多列转换）
     *
     * @param inputStream CSV数据流
     * @param transformations 转换映射：新列名 -> (原列名, 转换函数)
     * @param batchSize 批处理大小
     * @param delimiter 分隔符
     * @param header 是否包含表头
     * @param autoType 是否自动推断类型
     * @param encoding 文件编码
     * @param skipLines 跳过行数
     * @param nullValues 空值标识列表
     * @param trimValues 是否修剪值
     * @return 转换后的DataFrame
     */
    fun batchTransformMultiple(
        inputStream: InputStream,
        transformations: Map<String, (Map<String, Any?>) -> Any?>,
        batchSize: Int = DEFAULT_BATCH_SIZE,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        val allRows = mutableListOf<Map<String, Any?>>()

        readCSVBatch(inputStream, batchSize, { batchDF ->
            val indexList = batchDF.index()
            for (i in indexList.indices) {
                val originalRow = batchDF.iloc(i)
                val rowValues = originalRow.mapValues { (_, series) -> series?.get(0) }.toMutableMap()

                // 应用所有转换
                transformations.forEach { (newColName, transformFunc) ->
                    rowValues[newColName] = transformFunc(rowValues)
                }

                allRows.add(rowValues)
            }
        }, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)

        return DataFrame(allRows)
    }
}
