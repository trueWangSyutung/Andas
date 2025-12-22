package cn.ac.oac.demo.andas.chapters.examples

import android.content.Context
import cn.ac.oac.libs.andas.Andas
import cn.ac.oac.libs.andas.core.NativeMath
import cn.ac.oac.demo.andas.chapters.Example
import cn.ac.oac.libs.andas.entity.Series

/**
 * 第六章：性能测试示例
 */
object PerformanceExamples {
    
    fun getExamples(): List<Example> {
        return listOf(
            seriesCreation(),
            jniVsKotlin(),
            batchProcessing()
        )
    }
    
    private fun seriesCreation(): Example {
        return Example(
            id = "perf_series_creation",
            title = "6.1 Series 创建性能",
            description = "测试 Series 创建和基本操作性能",
            code = """
                val data = (1..100000).map { it }
                val start = System.currentTimeMillis()
                val series = Andas.getInstance().createSeries(data)
                val doubled = series * 2
                val time = System.currentTimeMillis() - start
                println("耗时: ${'$'}{time}ms")
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val data = (1..100000).map { it }
                    val start = System.currentTimeMillis()
                    val series = Andas.getInstance().createSeries(data)
                    val doubled = series * 2
                    val time = System.currentTimeMillis() - start
                    callback("✅ Series 创建性能测试\n数据量: ${data.size}\n耗时: ${time}ms")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun jniVsKotlin(): Example {
        return Example(
            id = "perf_jni_vs_kotlin",
            title = "6.2 JNI vs Kotlin 性能对比",
            description = "对比原生 JNI 和 Kotlin 实现的性能差异",
            code = """
                // Kotlin 实现
                 val data = (1..100000).map { it.toDouble() }
                    val series = Andas.getInstance().createSeries(
                        data
                    )
                    // Kotlin 实现
                    val start1 = System.currentTimeMillis()
                    val kotlinSum = series.sumKt()
                    val time1 = System.currentTimeMillis() - start1
                    
                    // JNI 实现
                    val start2 = System.currentTimeMillis()
                    val jniSum = series.sum()
                    val time2 = System.currentTimeMillis() - start2
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val data = (1..1000000).map { it.toDouble() }
                    val series = Andas.getInstance().createSeries(
                        data
                    )
                    // Kotlin 实现
                    val start1 = System.currentTimeMillis()
                    val kotlinSum = series.sumKt()
                    val time1 = System.currentTimeMillis() - start1
                    
                    // JNI 实现
                    val start2 = System.currentTimeMillis()
                    val jniSum = series.sum()
                    val time2 = System.currentTimeMillis() - start2
                    
                    val speedup = if (time2 > 0) "%.2f".format(time1.toDouble() / time2) else "N/A"
                    
                    callback("✅ JNI vs Kotlin 性能对比\nKotlin: ${time1}ms (sum=$kotlinSum)\nJNI: ${time2}ms (sum=$jniSum)\n加速比: ${speedup}x")
                } catch (e: Exception) {
                    callback("❌ 性能对比失败: ${e.message}")
                }
            }
        )
    }
    
    private fun batchProcessing(): Example {
        return Example(
            id = "perf_batch_processing",
            title = "6.3 批量处理性能",
            description = "测试大数据量批量处理性能",
            code = """
                val data = (1..500000).map { it * 1.0 }
                val start = System.currentTimeMillis()
                val series = Andas.getInstance().createSeries(data)
                val processed = series.normalize()
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val data = (1..500000).map { it * 1.0 }
                    val start = System.currentTimeMillis()
                    val series = Andas.getInstance().createSeries(data)
                    val processed = series.normalize()
                    val time = System.currentTimeMillis() - start
                    callback("✅ 批量处理性能测试\n数据量: ${data.size}\n耗时: ${time}ms\n处理结果前5: ${processed.head().toString()}")
                } catch (e: Exception) {
                    callback("❌ 批量测试失败: ${e.message}")
                }
            }
        )
    }
}
