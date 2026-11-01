package cn.ac.oac.libs.andas.entity

import java.io.File
import java.io.InputStream

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
            assetManager.open(filePath).use { inputStream ->
                CsvReader.read(
                    inputStream,
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
        return try {
            CsvReader.read(
                inputStream,
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
        } catch (e: Exception) {
            throw RuntimeException("读取数据流失败: ${e.message}", e)
        }
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
        try {
            CsvReader.readBatches(
                inputStream,
                batchSize,
                CsvReader.Options(
                    delimiter = delimiter,
                    header = header,
                    autoType = autoType,
                    encoding = encoding,
                    skipLines = skipLines,
                    nullValues = nullValues,
                    trimValues = trimValues
                ),
                callback
            )
        } catch (e: Exception) {
            throw RuntimeException("读取数据流失败: ${e.message}", e)
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
