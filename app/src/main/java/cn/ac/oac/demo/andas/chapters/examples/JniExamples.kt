package cn.ac.oac.demo.andas.chapters.examples

import android.content.Context
import cn.ac.oac.libs.andas.core.NativeMath
import cn.ac.oac.demo.andas.chapters.Example

/**
 * 第五章：JNI 原生示例
 */
object JniExamples {
    
    fun getExamples(): List<Example> {
        return listOf(
            mathSum(),
            mathStats(),
            dataProcessing()
        )
    }
    
    private fun mathSum(): Example {
        return Example(
            id = "jni_math_sum",
            title = "5.1 原生数学运算 - 求和",
            description = "使用 JNI 进行高性能数组求和",
            code = """
                val data = (1..10000).map { it.toDouble() }.toDoubleArray()
                val sum = NativeMath.sumDoubleArray(data)
                val mean = NativeMath.meanDoubleArray(data)
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val data = (1..10000).map { it.toDouble() }.toDoubleArray()
                    val sum = NativeMath.sumDoubleArray(data)
                    val mean = NativeMath.meanDoubleArray(data)
                    callback("✅ 原生数学运算运算测试\n数据量: ${data.size}\nsum=$sum, mean=$mean")
                } catch (e: Exception) {
                    callback("❌ JNI 运算失败: ${e.message}")
                }
            }
        )
    }
    
    private fun mathStats(): Example {
        return Example(
            id = "jni_math_stats",
            title = "5.2 原生统计计算",
            description = "原生计算方差、标准差等",
            code = """
                val data = (1..1000).map { it * 1.0 }.toDoubleArray()
                val variance = NativeMath.varianceDoubleArray(data)
                val std = NativeMath.stdDoubleArray(data)
                val max = NativeMath.maxDoubleArray(data)
                val min = NativeMath.minDoubleArray(data)
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val data = (1..1000).map { it * 1.0 }.toDoubleArray()
                    val variance = NativeMath.variance(data)
                    val std = NativeMath.sumDoubleArray(data)
                    val max = NativeMath.maxDoubleArray(data)
                    val min = NativeMath.minDoubleArray(data)
                    callback("✅ 原生统计计算\n方差: $variance\n标准差: $std\n最大值: $max\n最小值: $min")
                } catch (e: Exception) {
                    callback("❌ 统计失败: ${e.message}")
                }
            }
        )
    }
    
    private fun dataProcessing(): Example {
        return Example(
            id = "jni_data_processing",
            title = "5.3 原生数据处理",
            description = "原生数据变换和处理",
            code = """
                val data = (1..100).map { it * 1.0 }.toDoubleArray()
                val normalized = NativeMath.normalizeArray(data)
                val doubled = NativeMath.multiplyArray(data, 2.0)
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val data = (1..100).map { it * 1.0 }.toDoubleArray()
                    val normalized = NativeMath.normalize(data)
                    val doubled = NativeMath.multiplyDoubleArray(data, 2.0)
                    callback("✅ 原生数据处理\n归一化前5: ${normalized.take(5)}\n加倍前5: ${doubled.take(5)}")
                } catch (e: Exception) {
                    callback("❌ 数据处理失败: ${e.message}")
                }
            }
        )
    }
}
