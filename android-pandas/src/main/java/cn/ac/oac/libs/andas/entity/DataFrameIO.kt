package cn.ac.oac.libs.andas.entity

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

object DataFrameIO {

    /**
     * 从Assets读取CSV文件
     */
    fun readFromAssets(
        assetManager: android.content.res.AssetManager,
        filePath: String,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        return try {
            val inputStream = assetManager.open(filePath)
            val reader = BufferedReader(InputStreamReader(inputStream, encoding))
            val lines = reader.readLines()
            reader.close()

            // 手动解析，因为File对象不可用
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
                headers = DataFrame.parseCSVLine(firstLine, delimiter, trimValues)
                dataLines = effectiveLines.drop(1)
            } else {
                val firstLineValues = DataFrame.parseCSVLine(firstLine, delimiter, trimValues)
                headers = firstLineValues.indices.map { "col$it" }
                dataLines = effectiveLines
            }

            if (headers.isEmpty()) {
                throw IllegalArgumentException("解析失败: 未找到有效的列名")
            }

            // 预先推断每列的数据类型（只使用第一行数据）
            val columnTypes = mutableMapOf<String, (String) -> Any?>()
            if (autoType && dataLines.isNotEmpty()) {
                val firstLineValues = DataFrame.parseCSVLine(dataLines.first(), delimiter, trimValues)
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
                    val inferredType = DataFrame.inferTypeFromSample(firstValue, nullValues)
                    columnTypes[header] = { value ->
                        if (value in nullValues) null
                        else DataFrame.convertValueWithType(value, inferredType)
                    }
                }
            }

            val rows = dataLines.mapIndexed { lineIndex, line ->
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

            DataFrame(rows)
        } catch (e: Exception) {
            throw RuntimeException("从Assets读取文件失败: ${e.message}", e)
        }
    }

    /**
     * 从CSV文件读取（静态方法）
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
        return DataFrame.readCSV(file, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)
    }

    /**
     * 从CSV数据流读取（静态方法）
     */
    fun readCSV(
        inputStream: InputStream,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        val lines = try {
            val reader = BufferedReader(InputStreamReader(inputStream, encoding))
            reader.readLines()
        } catch (e: Exception) {
            throw RuntimeException("读取数据流失败: ${e.message}", e)
        }

        if (lines.isEmpty()) {
            return DataFrame(emptyList<Map<String, Any?>>())
        }

        // 跳过指定行数
        val effectiveLines = lines.drop(skipLines)
        if (effectiveLines.isEmpty()) {
            return DataFrame(emptyList<Map<String, Any?>>())
        }

        // 解析表头
        val firstLine = effectiveLines.first()
        val headers: List<String>
        val dataLines: List<String>

        if (header) {
            headers = DataFrame.parseCSVLine(firstLine, delimiter, trimValues)
            dataLines = effectiveLines.drop(1)
        } else {
            val firstLineValues = DataFrame.parseCSVLine(firstLine, delimiter, trimValues)
            headers = firstLineValues.indices.map { "col$it" }
            dataLines = effectiveLines
        }

        if (headers.isEmpty()) {
            throw IllegalArgumentException("解析失败: 未找到有效的列名")
        }

        // 预先推断每列的数据类型（只使用第一行数据）
        val columnTypes = mutableMapOf<String, (String) -> Any?>()
        if (autoType && dataLines.isNotEmpty()) {
            val firstLineValues = DataFrame.parseCSVLine(dataLines.first(), delimiter, trimValues)
            val adjustedFirstValues = if (firstLineValues.size < headers.size) {
                firstLineValues + List(headers.size - firstLineValues.size) { "" }
            } else if (firstLineValues.size > headers.size) {
                firstLineValues.take(headers.size)
            } else {
                firstLineValues
            }

            headers.forEachIndexed { index, header ->
                val firstValue = adjustedFirstValues.getOrNull(index) ?: ""
                val inferredType = DataFrame.inferTypeFromSample(firstValue, nullValues)
                columnTypes[header] = { value ->
                    if (value in nullValues) null
                    else DataFrame.convertValueWithType(value, inferredType)
                }
            }
        }

        val rows = dataLines.mapIndexed { lineIndex, line ->
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

        return DataFrame(rows)
    }

    /**
     * 分批读取CSV文件（用于大数据处理）- 回调版本
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
        val lines = try {
            val reader = BufferedReader(InputStreamReader(inputStream, encoding))
            reader.readLines()
        } catch (e: Exception) {
            throw RuntimeException("读取数据流失败: ${e.message}", e)
        }

        if (lines.isEmpty()) {
            return
        }

        // 跳过指定行数
        val effectiveLines = lines.drop(skipLines)
        if (effectiveLines.isEmpty()) {
            return
        }

        // 解析表头
        val firstLine = effectiveLines.first()
        val headers: List<String>
        val dataLines: List<String>

        if (header) {
            headers = DataFrame.parseCSVLine(firstLine, delimiter, trimValues)
            dataLines = effectiveLines.drop(1)
        } else {
            val firstLineValues = DataFrame.parseCSVLine(firstLine, delimiter, trimValues)
            headers = firstLineValues.indices.map { "col$it" }
            dataLines = effectiveLines
        }

        if (headers.isEmpty()) {
            throw IllegalArgumentException("解析失败: 未找到有效的列名")
        }

        // 预先推断每列的数据类型（只使用第一行数据）
        val columnTypes = mutableMapOf<String, (String) -> Any?>()
        if (autoType && dataLines.isNotEmpty()) {
            val firstLineValues = DataFrame.parseCSVLine(dataLines.first(), delimiter, trimValues)
            val adjustedFirstValues = if (firstLineValues.size < headers.size) {
                firstLineValues + List(headers.size - firstLineValues.size) { "" }
            } else if (firstLineValues.size > headers.size) {
                firstLineValues.take(headers.size)
            } else {
                firstLineValues
            }

            headers.forEachIndexed { index, header ->
                val firstValue = adjustedFirstValues.getOrNull(index) ?: ""
                val inferredType = DataFrame.inferTypeFromSample(firstValue, nullValues)
                columnTypes[header] = { value ->
                    if (value in nullValues) null
                    else DataFrame.convertValueWithType(value, inferredType)
                }
            }
        }

        // 分批处理数据
        var currentBatch = mutableListOf<Map<String, Any?>>()

        dataLines.forEachIndexed { lineIndex, line ->
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
                throw RuntimeException("解析第 ${lineIndex + skipLines + (if (header) 2 else 1)} 行失败: $line", e)
            }
        }

        // 处理剩余数据
        if (currentBatch.isNotEmpty()) {
            callback(DataFrame(currentBatch))
        }
    }

 


    /**
     * 导出到CSV文件（静态方法）
     */
    fun toCSV(df: DataFrame, file: File) {
        df.toCSV(file)
    }

    /**
     * 从应用私有目录读取CSV文件
     */
    fun readFromPrivateStorage(
        context: android.content.Context,
        fileName: String,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        val file = File(context.filesDir, fileName)
        return DataFrame.readCSV(file, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)
    }

    /**
     * 从外部存储读取CSV文件（需要权限）
     */
    fun readFromExternalStorage(
        filePath: String,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        encoding: String = "UTF-8",
        skipLines: Int = 0,
        nullValues: List<String> = listOf("", "null", "NULL", "NA", "N/A"),
        trimValues: Boolean = true
    ): DataFrame {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("文件不存在: $filePath")
        }
        if (!file.canRead()) {
            throw SecurityException("无法读取文件: $filePath，请确保有读取权限")
        }
        return DataFrame.readCSV(file, delimiter, header, autoType, encoding, skipLines, nullValues, trimValues)
    }

    /**
     * 保存到应用私有目录
     */
    fun saveToPrivateStorage(
        context: android.content.Context,
        fileName: String,
        df: DataFrame
    ) {
        val file = File(context.filesDir, fileName)
        df.toCSV(file)
    }

    /**
     * 保存到外部存储（需要权限）
     */
    fun saveToExternalStorage(
        filePath: String,
        df: DataFrame
    ) {
        val file = File(filePath)
        // 确保父目录存在
        file.parentFile?.mkdirs()
        df.toCSV(file)
    }

    /**
     * 异步读取Assets文件
     */
    fun readFromAssetsAsync(
        assetManager: android.content.res.AssetManager,
        filePath: String,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        onSuccess: (DataFrame) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        cn.ac.oac.libs.andas.core.asyncIO {
            readFromAssets(assetManager, filePath, delimiter, header, autoType)
        }.onSuccess { df ->
            onSuccess(df)
        }.onError { error ->
            onError(error)
        }
    }

    /**
     * 异步读取私有存储
     */
    fun readFromPrivateStorageAsync(
        context: android.content.Context,
        fileName: String,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        onSuccess: (DataFrame) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        cn.ac.oac.libs.andas.core.asyncIO {
            readFromPrivateStorage(context, fileName, delimiter, header, autoType)
        }.onSuccess { df ->
            onSuccess(df)
        }.onError { error ->
            onError(error)
        }
    }

    /**
     * 异步保存到私有存储
     */
    fun saveToPrivateStorageAsync(
        context: android.content.Context,
        fileName: String,
        df: DataFrame,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        cn.ac.oac.libs.andas.core.asyncIO {
            saveToPrivateStorage(context, fileName, df)
        }.onSuccess {
            onSuccess()
        }.onError { error ->
            onError(error)
        }
    }
}
