package cn.ac.oac.libs.andas

import android.content.Context
import cn.ac.oac.libs.andas.core.AndaThreadPool
import cn.ac.oac.libs.andas.core.asyncIO
import cn.ac.oac.libs.andas.core.asyncCompute
import cn.ac.oac.libs.andas.entity.DataFrame
import cn.ac.oac.libs.andas.entity.Series
import cn.ac.oac.libs.andas.entity.DataFrameIO
import java.io.File

/**
 * Andas SDK主入口类
 * 提供统一的API接口和初始化管理
 */
class Andas private constructor() {
    
    companion object {
        @Volatile
        private var instance: Andas? = null
        
        /**
         * 获取SDK实例（双重检查锁定）
         */
        fun getInstance(): Andas {
            if (instance == null) {
                synchronized(Andas::class) {
                    if (instance == null) {
                        instance = Andas()
                    }
                }
            }
            return instance!!
        }


        /**
         * 快速初始化（一行代码接入）
         */
        fun initialize(context: Context, config: AndasConfig.() -> Unit = {}): Andas {
            val instance = getInstance()
            instance.init(context, AndasConfig().apply(config))
            return instance
        }
    }
    
    // SDK配置
    private lateinit var config: AndasConfig
    
    // 应用上下文
    private lateinit var appContext: Context
    
    // 是否已初始化
    private var initialized = false
    
    /**
     * SDK配置类
     */
    class AndasConfig {
        var debugMode = false
        var cachePath = "andas_cache"
        var timeoutSeconds = 30L
        var memoryOptimization = true
        var maxConcurrentTasks = 4
        var logLevel = LogLevel.INFO
        var errorHandler: ((Exception) -> Unit)? = null
        
        fun validate() {
            if (maxConcurrentTasks < 1) {
                throw IllegalArgumentException("最大并发任务数必须大于0")
            }
        }
    }
    
    /**
     * 日志级别
     */
    enum class LogLevel {
        DEBUG, INFO, WARN, ERROR, NONE
    }
    
    /**
     * 初始化SDK
     */
    @Synchronized
    fun init(context: Context, config: AndasConfig = AndasConfig()) {
        if (initialized) {
            logWarn("Andas SDK已初始化，重复初始化将被忽略")
            return
        }
        
        try {
            config.validate()
            this.config = config
            this.appContext = context.applicationContext
            
            // 创建缓存目录
            val cacheDir = getCacheDirectory()
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            initialized = true
            
            logInfo("Andas SDK初始化成功")
            logInfo("调试模式: ${config.debugMode}")
            logInfo("缓存路径: ${cacheDir.absolutePath}")
            logInfo("超时时间: ${config.timeoutSeconds}s")
            
        } catch (e: Exception) {
            val errorMsg = "Andas SDK初始化失败: ${e.message}"
            logError(errorMsg)
            handleException(e)
            throw RuntimeException(errorMsg, e)
        }
    }
    
    /**
     * 检查是否已初始化
     */
    fun checkInitialized() {
        if (!initialized) {
            throw IllegalStateException("Andas SDK未初始化，请先调用 Andas.initialize()")
        }
    }
    
    /**
     * 获取缓存目录
     */
    fun getCacheDirectory(): File {
        return File(appContext.cacheDir, config.cachePath)
    }
    
    // ==================== Series工厂方法 ====================
    
    /**
     * 创建Series
     */
    fun <T> createSeries(
        data: List<T?>,
        index: List<Any>? = null,
        name: String? = null
    ): Series<T> {
        checkInitialized()
        return try {
            Series(data, index, name)
        } catch (e: Exception) {
            logError("创建Series失败: ${e.message}")
            handleException(e)
            throw e
        }
    }
    
    /**
     * 从Map创建Series
     */
    fun <T> createSeriesFromMap(
        map: Map<Any, T?>,
        name: String? = null
    ): Series<T> {
        checkInitialized()
        return try {
            Series(map, name)
        } catch (e: Exception) {
            logError("从Map创建Series失败: ${e.message}")
            handleException(e)
            throw e
        }
    }
    
    // ==================== DataFrame工厂方法 ====================
    
    /**
     * 从列数据创建DataFrame
     */
    fun createDataFrame(columnsData: Map<String, List<Any?>>): DataFrame {
        checkInitialized()
        return try {
            DataFrame(columnsData)
        } catch (e: Exception) {
            logError("创建DataFrame失败: ${e.message}")
            handleException(e)
            throw e
        }
    }
    
    /**
     * 从行数据创建DataFrame
     */
    fun createDataFrameFromRows(rowsData: List<Map<String, Any?>>): DataFrame {
        checkInitialized()
        return try {
            DataFrame(rowsData)
        } catch (e: Exception) {
            logError("从行数据创建DataFrame失败: ${e.message}")
            handleException(e)
            throw e
        }
    }
    
    // ==================== 数据I/O方法 ====================
    
    /**
     * 读取CSV文件（同步）
     * 
     * @param file CSV文件对象
     * @param delimiter 分隔符，默认为逗号
     * @param header 是否包含表头，默认为true
     * @param autoType 是否自动推断数据类型，默认为true
     * @return DataFrame对象
     */
    fun readCSV(
        file: File, 
        delimiter: String = ",", 
        header: Boolean = true,
        autoType: Boolean = true
    ): DataFrame {
        checkInitialized()
        return try {
            DataFrame.readCSV(file, delimiter, header, autoType)
        } catch (e: Exception) {
            logError("读取CSV文件失败: ${e.message}")
            handleException(e)
            throw e
        }
    }
    
    /**
     * 读取CSV文件（异步）
     * 
     * @param file CSV文件对象
     * @param delimiter 分隔符，默认为逗号
     * @param header 是否包含表头，默认为true
     * @param autoType 是否自动推断数据类型，默认为true
     * @param onSuccess 成功回调
     * @param onError 失败回调
     */
    fun readCSVAsync(
        file: File,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        onSuccess: (DataFrame) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        checkInitialized()
        asyncIO {
            DataFrame.readCSV(file, delimiter, header, autoType)
        }.onSuccess { df ->
            onSuccess(df)
        }.onError { error ->
            onError(error)
        }
    }
    
    /**
     * 导出CSV文件（同步）
     */
    fun exportCSV(df: DataFrame, file: File) {
        checkInitialized()
        try {
            df.toCSV(file)
        } catch (e: Exception) {
            logError("导出CSV文件失败: ${e.message}")
            handleException(e)
            throw e
        }
    }
    
    /**
     * 导出CSV文件（异步）
     */
    fun exportCSVAsync(
        df: DataFrame,
        file: File,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        checkInitialized()
        asyncIO {
            df.toCSV(file)
        }.onSuccess {
            onSuccess()
        }.onError { error ->
            onError(error)
        }
    }
    
    /**
     * 从Assets读取CSV文件
     * 
     * @param filePath Assets中的文件路径
     * @param delimiter 分隔符，默认为逗号
     * @param header 是否包含表头，默认为true
     * @param autoType 是否自动推断数据类型，默认为true
     * @return DataFrame对象
     */
    fun readCSVFromAssets(
        filePath: String,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true
    ): DataFrame {
        checkInitialized()
        return DataFrameIO.readFromAssets(
            context.assets, filePath, delimiter, header, autoType
        )
    }
    
    /**
     * 从Assets读取CSV文件（异步）
     * 
     * @param filePath Assets中的文件路径
     * @param delimiter 分隔符，默认为逗号
     * @param header 是否包含表头，默认为true
     * @param autoType 是否自动推断数据类型，默认为true
     * @param onSuccess 成功回调
     * @param onError 失败回调
     */
    fun readCSVFromAssetsAsync(
        filePath: String,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        onSuccess: (DataFrame) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        checkInitialized()
        DataFrameIO.readFromAssetsAsync(
            context.assets, filePath, delimiter, header, autoType, onSuccess, onError
        )
    }
    
    /**
     * 从私有存储读取CSV文件
     * 
     * @param fileName 文件名
     * @param delimiter 分隔符，默认为逗号
     * @param header 是否包含表头，默认为true
     * @param autoType 是否自动推断数据类型，默认为true
     * @return DataFrame对象
     */
    fun readCSVFromPrivateStorage(
        fileName: String,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true
    ): DataFrame {
        checkInitialized()
        return DataFrameIO.readFromPrivateStorage(
            context, fileName, delimiter, header, autoType
        )
    }
    
    /**
     * 从私有存储读取CSV文件（异步）
     * 
     * @param fileName 文件名
     * @param delimiter 分隔符，默认为逗号
     * @param header 是否包含表头，默认为true
     * @param autoType 是否自动推断数据类型，默认为true
     * @param onSuccess 成功回调
     * @param onError 失败回调
     */
    fun readCSVFromPrivateStorageAsync(
        fileName: String,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true,
        onSuccess: (DataFrame) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        checkInitialized()
        DataFrameIO.readFromPrivateStorageAsync(
            context, fileName, delimiter, header, autoType, onSuccess, onError
        )
    }
    
    /**
     * 从外部存储读取CSV文件（需要权限）
     * 
     * @param filePath 文件路径
     * @param delimiter 分隔符，默认为逗号
     * @param header 是否包含表头，默认为true
     * @param autoType 是否自动推断数据类型，默认为true
     * @return DataFrame对象
     */
    fun readCSVFromExternalStorage(
        filePath: String,
        delimiter: String = ",",
        header: Boolean = true,
        autoType: Boolean = true
    ): DataFrame {
        checkInitialized()
        return DataFrameIO.readFromExternalStorage(
            filePath, delimiter, header, autoType
        )
    }
    
    /**
     * 保存到私有存储
     */
    fun saveToPrivateStorage(
        fileName: String,
        df: DataFrame
    ) {
        checkInitialized()
        DataFrameIO.saveToPrivateStorage(
            context, fileName, df
        )
    }
    
    /**
     * 保存到私有存储（异步）
     */
    fun saveToPrivateStorageAsync(
        fileName: String,
        df: DataFrame,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        checkInitialized()
        DataFrameIO.saveToPrivateStorageAsync(
            context, fileName, df, onSuccess, onError
        )
    }
    
    /**
     * 保存到外部存储（需要权限）
     */
    fun saveToExternalStorage(
        filePath: String,
        df: DataFrame
    ) {
        checkInitialized()
        DataFrameIO.saveToExternalStorage(
            filePath, df
        )
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 执行异步任务
     */
    fun <T> executeAsync(
        task: () -> T,
        onSuccess: (T) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        checkInitialized()
        asyncCompute(task)
            .onSuccess(onSuccess)
            .onError(onError)
    }
    
    /**
     * 执行IO任务
     */
    fun <T> executeIO(
        task: () -> T,
        onSuccess: (T) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        checkInitialized()
        asyncIO(task)
            .onSuccess(onSuccess)
            .onError(onError)
    }
    
    /**
     * 获取SDK统计信息
     */
    fun getStats(): Map<String, Any> {
        checkInitialized()
        return mapOf(
            "initialized" to initialized,
            "debug_mode" to config.debugMode,
            "cache_directory" to getCacheDirectory().absolutePath,
            "thread_pool_stats" to AndaThreadPool.getThreadPoolStats()
        )
    }
    
    /**
     * 销毁SDK，释放资源
     */
    @Synchronized
    fun destroy() {
        if (!initialized) return
        
        try {
            AndaThreadPool.shutdown()
            initialized = false
            logInfo("Andas SDK已销毁")
        } catch (e: Exception) {
            logError("销毁SDK时出错: ${e.message}")
            handleException(e)
        }
    }
    
    // ==================== 日志和异常处理 ====================
    
    private fun logDebug(message: String) {
        if (config.debugMode && config.logLevel <= LogLevel.DEBUG) {
            println("[Andas-DEBUG] $message")
        }
    }
    
    private fun logInfo(message: String) {
        if (config.logLevel <= LogLevel.INFO) {
            println("[Andas-INFO] $message")
        }
    }
    
    private fun logWarn(message: String) {
        if (config.logLevel <= LogLevel.WARN) {
            println("[Andas-WARN] $message")
        }
    }
    
    private fun logError(message: String) {
        if (config.logLevel <= LogLevel.ERROR) {
            System.err.println("[Andas-ERROR] $message")
        }
    }
    
    private fun handleException(e: Exception) {
        config.errorHandler?.invoke(e) ?: run {
            if (config.debugMode) {
                e.printStackTrace()
            }
        }
    }
    
    // 为了内部方法访问context
    private val context: Context
        get() = appContext
}

/**
 * 便捷的链式API扩展
 */

/**
 * 创建Series的便捷函数
 */
fun <T> andasSeries(
    data: List<T?>,
    index: List<Any>? = null,
    name: String? = null
): Series<T> {
    return Andas.getInstance().createSeries(data, index, name)
}

/**
 * 从Map创建Series的便捷函数
 */
fun <T> andasSeriesFromMap(
    map: Map<Any, T?>,
    name: String? = null
): Series<T> {
    return Andas.getInstance().createSeriesFromMap(map, name)
}

/**
 * 创建DataFrame的便捷函数
 */
fun andasDataFrame(columnsData: Map<String, List<Any?>>): DataFrame {
    return Andas.getInstance().createDataFrame(columnsData)
}

/**
 * 从行数据创建DataFrame的便捷函数
 */
fun andasDataFrameFromRows(rowsData: List<Map<String, Any?>>): DataFrame {
    return Andas.getInstance().createDataFrameFromRows(rowsData)
}

/**
 * 读取CSV的便捷函数
 * 
 * @param file CSV文件对象
 * @param delimiter 分隔符，默认为逗号
 * @param header 是否包含表头，默认为true
 * @param autoType 是否自动推断数据类型，默认为true
 * @return DataFrame对象
 */
fun andasReadCSV(
    file: File, 
    delimiter: String = ",", 
    header: Boolean = true,
    autoType: Boolean = true
): DataFrame {
    return Andas.getInstance().readCSV(file, delimiter, header, autoType)
}

/**
 * 异步读取CSV的便捷函数
 * 
 * @param file CSV文件对象
 * @param delimiter 分隔符，默认为逗号
 * @param header 是否包含表头，默认为true
 * @param autoType 是否自动推断数据类型，默认为true
 * @param onSuccess 成功回调
 * @param onError 失败回调
 */
fun andasReadCSVAsync(
    file: File,
    delimiter: String = ",",
    header: Boolean = true,
    autoType: Boolean = true,
    onSuccess: (DataFrame) -> Unit,
    onError: (Throwable) -> Unit
) {
    Andas.getInstance().readCSVAsync(file, delimiter, header, autoType, onSuccess, onError)
}
