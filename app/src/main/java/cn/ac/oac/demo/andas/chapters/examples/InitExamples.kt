package cn.ac.oac.demo.andas.chapters.examples

import android.content.Context
import android.util.Log
import cn.ac.oac.libs.andas.Andas
import cn.ac.oac.demo.andas.chapters.Example

/**
 * 第一章：SDK 初始化示例
 */
object InitExamples {
    
    fun getExamples(): List<Example> {
        return listOf(
            fastInit(),
            standardInit(),
            productionInit()
        )
    }
    
    private fun fastInit(): Example {
        return Example(
            id = "init_fast",
            title = "1.1 快速初始化",
            description = "使用默认配置快速初始化 SDK（推荐）",
            code = """
                // 在 Application.onCreate() 或 Activity.onCreate() 中
                Andas.initialize(this) {
                    debugMode = true
                    logLevel = Andas.LogLevel.DEBUG
                    timeoutSeconds = 60L
                }
            """.trimIndent(),
            action = { context, callback ->
                try {
                    Andas.initialize(context) {
                        debugMode = true
                        logLevel = Andas.LogLevel.DEBUG
                        timeoutSeconds = 60L
                    }
                    callback("✅ 快速初始化成功\n配置: debug=true, log=DEBUG, timeout=60s")
                } catch (e: Exception) {
                    callback("❌ 初始化失败: ${e.message}")
                }
            }
        )
    }
    
    private fun standardInit(): Example {
        return Example(
            id = "init_standard",
            title = "1.2 标准初始化",
            description = "使用完整配置参数初始化",
            code = """
                val config = Andas.AndasConfig().apply {
                    debugMode = true
                    logLevel = Andas.LogLevel.INFO
                    timeoutSeconds = 30L
                    cachePath = "andas_cache"
                    memoryOptimization = true
                    maxConcurrentTasks = 4
                    errorHandler = { exception ->
                        Log.e("Andas", "发生错误", exception)
                    }
                }
                Andas.getInstance().init(this, config)
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val config = Andas.AndasConfig().apply {
                        debugMode = true
                        logLevel = Andas.LogLevel.INFO
                        timeoutSeconds = 30L
                        cachePath = "andas_cache"
                        memoryOptimization = true
                        maxConcurrentTasks = 4
                        errorHandler = { exception ->
                            Log.e("Andas", "发生错误", exception)
                        }
                    }
                    Andas.getInstance().init(context, config)
                    callback("✅ 标准初始化成功\n完整配置已应用")
                } catch (e: Exception) {
                    callback("❌ 初始化失败: ${e.message}")
                }
            }
        )
    }
    
    private fun productionInit(): Example {
        return Example(
            id = "init_production",
            title = "1.3 生产环境配置",
            description = "生产环境优化配置",
            code = """
                Andas.initialize(this) {
                    debugMode = false
                    logLevel = Andas.LogLevel.WARN
                    timeoutSeconds = 120L
                    memoryOptimization = true
                    maxConcurrentTasks = Runtime.getRuntime().availableProcessors() * 2
                }
            """.trimIndent(),
            action = { context, callback ->
                try {
                    Andas.initialize(context) {
                        debugMode = false
                        logLevel = Andas.LogLevel.WARN
                        timeoutSeconds = 120L
                        memoryOptimization = true
                        maxConcurrentTasks = Runtime.getRuntime().availableProcessors() * 2
                    }
                    callback("✅ 生产环境配置成功\n优化: 长超时, 高并发, 内存优化")
                } catch (e: Exception) {
                    callback("❌ 配置失败: ${e.message}")
                }
            }
        )
    }
}
