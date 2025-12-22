package cn.ac.oac.libs.andas.core

import android.os.Handler
import android.os.Looper
import java.util.concurrent.*

/**
 * Andas SDK专用线程池管理器
 * 负责管理所有耗时操作的线程调度，确保不阻塞UI线程
 * 
 * 设计原则：
 * - 使用独立的线程池，避免与宿主App竞争资源
 * - 提供主线程回调机制
 * - 支持任务取消和超时控制
 * - 全面的异常处理
 */
object AndaThreadPool {
    
    // CPU核心数
    private val CPU_COUNT = Runtime.getRuntime().availableProcessors()
    
    // 核心线程数：根据CPU核心数动态调整
    private val CORE_POOL_SIZE = kotlin.math.max(2, kotlin.math.min(CPU_COUNT - 1, 4))
    
    // 最大线程数
    private val MAX_POOL_SIZE = CPU_COUNT * 2 + 1
    
    // 线程空闲存活时间（秒）
    private const val KEEP_ALIVE_TIME = 30L
    
    // 线程池名称前缀
    private const val THREAD_NAME_PREFIX = "andas-pool-"
    
    // 主线程Handler
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // IO线程池（用于文件读写、网络请求）
    private val ioThreadPool: ExecutorService by lazy {
        ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(128),
            ThreadFactory { runnable ->
                Thread(runnable, "${THREAD_NAME_PREFIX}io-${System.currentTimeMillis()}").apply {
                    isDaemon = false
                    priority = Thread.NORM_PRIORITY
                }
            },
            ThreadPoolExecutor.CallerRunsPolicy() // 队列满时在调用线程执行
        )
    }
    
    // 计算线程池（用于复杂计算）
    private val computeThreadPool: ExecutorService by lazy {
        ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(128),
            ThreadFactory { runnable ->
                Thread(runnable, "${THREAD_NAME_PREFIX}compute-${System.currentTimeMillis()}").apply {
                    isDaemon = false
                    priority = Thread.NORM_PRIORITY + 1
                }
            },
            ThreadPoolExecutor.CallerRunsPolicy()
        )
    }
    
    // 单线程池（用于需要顺序执行的任务）
    private val singleThreadPool: ExecutorService by lazy {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "${THREAD_NAME_PREFIX}single-${System.currentTimeMillis()}").apply {
                isDaemon = false
                priority = Thread.NORM_PRIORITY
            }
        }
    }
    
    /**
     * 任务类型
     */
    enum class TaskType {
        IO,           // 文件读写、网络请求
        COMPUTE,      // 数据计算、分析
        SINGLE        // 顺序执行的任务
    }
    
    /**
     * 异步任务结果封装
     */
    data class TaskResult<T>(
        val success: Boolean,
        val data: T? = null,
        val error: Throwable? = null,
        val taskType: TaskType
    )
    
    /**
     * 提交IO任务（文件读写、网络请求）
     */
    fun <T> submitIO(task: () -> T): CompletableFuture<TaskResult<T>> {
        return submitTask(task, TaskType.IO, ioThreadPool)
    }
    
    /**
     * 提交计算任务（数据分析、复杂计算）
     */
    fun <T> submitCompute(task: () -> T): CompletableFuture<TaskResult<T>> {
        return submitTask(task, TaskType.COMPUTE, computeThreadPool)
    }
    
    /**
     * 提交单线程任务（需要顺序执行的操作）
     */
    fun <T> submitSingle(task: () -> T): CompletableFuture<TaskResult<T>> {
        return submitTask(task, TaskType.SINGLE, singleThreadPool)
    }
    
    /**
     * 通用任务提交方法
     */
    private fun <T> submitTask(
        task: () -> T,
        taskType: TaskType,
        executor: ExecutorService
    ): CompletableFuture<TaskResult<T>> {
        val future = CompletableFuture<TaskResult<T>>()
        
        executor.submit {
            try {
                val result = task()
                val taskResult = TaskResult(success = true, data = result, taskType = taskType)
                
                // 回调到主线程
                mainHandler.post {
                    try {
                        future.complete(taskResult)
                    } catch (e: Exception) {
                        future.completeExceptionally(e)
                    }
                }
            } catch (e: Exception) {
                val taskResult = TaskResult<T>(
                    success = false,
                    error = e,
                    taskType = taskType
                )
                
                // 回调到主线程
                mainHandler.post {
                    try {
                        future.complete(taskResult)
                    } catch (ex: Exception) {
                        future.completeExceptionally(ex)
                    }
                }
            }
        }
        
        return future
    }
    
    /**
     * 提交带超时的任务
     */
    fun <T> submitWithTimeout(
        timeout: Long,
        timeUnit: TimeUnit,
        taskType: TaskType,
        task: () -> T
    ): CompletableFuture<TaskResult<T>> {
        val future = CompletableFuture<TaskResult<T>>()
        
        val executor = when (taskType) {
            TaskType.IO -> ioThreadPool
            TaskType.COMPUTE -> computeThreadPool
            TaskType.SINGLE -> singleThreadPool
        }
        
        executor.submit {
            val startTime = System.currentTimeMillis()
            try {
                val result = task()
                val elapsed = System.currentTimeMillis() - startTime
                val timeoutMillis = timeUnit.toMillis(timeout)
                
                if (elapsed > timeoutMillis) {
                    val taskResult = TaskResult<T>(
                        success = false,
                        error = TimeoutException("任务执行超时: ${elapsed}ms > ${timeoutMillis}ms"),
                        taskType = taskType
                    )
                    mainHandler.post { future.complete(taskResult) }
                } else {
                    val taskResult = TaskResult(success = true, data = result, taskType = taskType)
                    mainHandler.post { future.complete(taskResult) }
                }
            } catch (e: Exception) {
                val taskResult = TaskResult<T>(
                    success = false,
                    error = e,
                    taskType = taskType
                )
                mainHandler.post { future.complete(taskResult) }
            }
        }
        
        return future
    }
    
    /**
     * 在主线程执行任务
     */
    fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                action()
            } catch (e: Exception) {
                // 记录异常但不抛出，避免影响主线程
                e.printStackTrace()
            }
        } else {
            mainHandler.post {
                try {
                    action()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    /**
     * 延迟执行任务
     */
    fun runDelayed(delayMillis: Long, action: () -> Unit) {
        mainHandler.postDelayed({
            try {
                action()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, delayMillis)
    }
    
    /**
     * 取消所有待处理任务
     */
    fun shutdown() {
        try {
            ioThreadPool.shutdown()
            computeThreadPool.shutdown()
            singleThreadPool.shutdown()
            
            // 等待任务完成（最多等待10秒）
            if (!ioThreadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                ioThreadPool.shutdownNow()
            }
            if (!computeThreadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                computeThreadPool.shutdownNow()
            }
            if (!singleThreadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                singleThreadPool.shutdownNow()
            }
            
            mainHandler.removeCallbacksAndMessages(null)
        } catch (e: InterruptedException) {
            // 立即关闭所有线程池
            ioThreadPool.shutdownNow()
            computeThreadPool.shutdownNow()
            singleThreadPool.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
    
    /**
     * 获取线程池状态信息
     */
    fun getThreadPoolStats(): Map<String, Any> {
        return mapOf(
            "io_pool" to getPoolStats(ioThreadPool),
            "compute_pool" to getPoolStats(computeThreadPool),
            "single_pool" to getPoolStats(singleThreadPool)
        )
    }
    
    private fun getPoolStats(executor: ExecutorService): Map<String, Any> {
        return when (executor) {
            is ThreadPoolExecutor -> mapOf(
                "active_count" to executor.activeCount,
                "pool_size" to executor.poolSize,
                "core_pool_size" to executor.corePoolSize,
                "maximum_pool_size" to executor.maximumPoolSize,
                "queue_size" to executor.queue.size,
                "completed_task_count" to executor.completedTaskCount
            )
            else -> mapOf("type" to executor.javaClass.simpleName)
        }
    }
}

/**
 * 异步操作扩展函数，提供更友好的API
 */
class AsyncResult<T>(
    private val future: CompletableFuture<AndaThreadPool.TaskResult<T>>
) {
    /**
     * 成功回调
     */
    fun onSuccess(callback: (T) -> Unit): AsyncResult<T> {
        future.thenAccept { result ->
            if (result.success) {
                result.data?.let { callback(it) }
            }
        }
        return this
    }
    
    /**
     * 失败回调
     */
    fun onError(callback: (Throwable) -> Unit): AsyncResult<T> {
        future.thenAccept { result ->
            if (!result.success && result.error != null) {
                callback(result.error)
            }
        }
        return this
    }
    
    /**
     * 完成回调（无论成功或失败）
     */
    fun onComplete(callback: (AndaThreadPool.TaskResult<T>) -> Unit): AsyncResult<T> {
        future.thenAccept(callback)
        return this
    }
    
    /**
     * 阻塞等待结果（不推荐在主线程使用）
     */
    fun get(): T? {
        return try {
            val result = future.get()
            if (result.success) result.data else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 等待指定时间
     */
    fun get(timeout: Long, timeUnit: TimeUnit): T? {
        return try {
            val result = future.get(timeout, timeUnit)
            if (result.success) result.data else null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 便捷的异步执行函数
 */
fun <T> asyncIO(task: () -> T): AsyncResult<T> {
    return AsyncResult(AndaThreadPool.submitIO(task))
}

fun <T> asyncCompute(task: () -> T): AsyncResult<T> {
    return AsyncResult(AndaThreadPool.submitCompute(task))
}

fun <T> asyncSingle(task: () -> T): AsyncResult<T> {
    return AsyncResult(AndaThreadPool.submitSingle(task))
}

/**
 * 数据处理专用异步操作
 */
object AsyncOps {
    
    /**
     * 异步读取CSV文件
     */
    fun readCSVAsync(
        file: java.io.File,
        delimiter: String = ",",
        onSuccess: (cn.ac.oac.libs.andas.entity.DataFrame) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        asyncIO {
            cn.ac.oac.libs.andas.entity.DataFrame.readCSV(file, delimiter)
        }.onSuccess(onSuccess).onError(onError)
    }
    
    /**
     * 异步导出CSV文件
     */
    fun exportCSVAsync(
        df: cn.ac.oac.libs.andas.entity.DataFrame,
        file: java.io.File,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        asyncIO {
            df.toCSV(file)
        }.onSuccess { onSuccess() }.onError(onError)
    }
    
    /**
     * 异步执行复杂数据操作
     */
    fun <T> performComplexOperation(
        operation: () -> T,
        onSuccess: (T) -> Unit,
        onError: (Throwable) -> Unit,
        timeout: Long = 30
    ) {
        AndaThreadPool.submitWithTimeout(timeout, TimeUnit.SECONDS, AndaThreadPool.TaskType.COMPUTE, operation)
            .thenAccept { result ->
                if (result.success && result.data != null) {
                    onSuccess(result.data)
                } else if (result.error != null) {
                    onError(result.error)
                }
            }
    }
}
