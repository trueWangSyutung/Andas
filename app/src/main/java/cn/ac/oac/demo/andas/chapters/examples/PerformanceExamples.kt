package cn.ac.oac.demo.andas.chapters.examples

import android.content.Context
import android.util.Log
import cn.ac.oac.libs.andas.Andas
import cn.ac.oac.libs.andas.core.NativeMath
import cn.ac.oac.demo.andas.chapters.Example
import cn.ac.oac.libs.andas.entity.Series
import cn.ac.oac.libs.andas.factory.andasDataFrame

/**
 * 第六章：性能测试示例
 */
object PerformanceExamples {
    
    fun getExamples(): List<Example> {
        return listOf(
            // 基础性能测试
            seriesCreation(),
            dataFrameCreation(),
            
            // 统计计算性能
            sumPerformance(),
            meanPerformance(),
            maxMinPerformance(),
            variancePerformance(),
            stdPerformance(),
            
            // 向量化运算性能
            vectorizedAddPerformance(),
            vectorizedMultiplyPerformance(),
            dotProductPerformance(),
            normPerformance(),
            
            // 空值处理性能
            nullDetectionPerformance(),
            nullHandlingPerformance(),
            
            // 分组聚合性能
            groupBySumPerformance(),
            
            // 排序性能
            sortPerformance(),
            
            // 筛选性能
            filterPerformance(),
            
            // 合并性能
            mergePerformance(),
            
            // 采样和批量处理
            samplingPerformance(),
            batchProcessingPerformance(),
            
            // JNI vs Kotlin 对比
            jniVsKotlin(),
            
            // 综合性能测试
            comprehensiveTest()
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
    
    private fun dataFrameCreation(): Example {
        return Example(
            id = "perf_dataframe_creation",
            title = "6.2 DataFrame 创建性能",
            description = "测试 DataFrame 创建性能",
            code = """
                val start = System.currentTimeMillis()
                val df = andasDataFrame(
                    mapOf(
                        "id" to (1..100000).toList(),
                        "value" to (1..100000).map { it * 1.5 }
                    )
                )
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val start = System.currentTimeMillis()
                    val df = andasDataFrame(
                        mapOf(
                            "id" to (1..100000).toList(),
                            "value" to (1..100000).map { it * 1.5 }
                        )
                    )
                    val time = System.currentTimeMillis() - start
                    callback("✅ DataFrame 创建性能\n数据量: 100000行\n耗时: ${time}ms")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun sumPerformance(): Example {
        return Example(
            id = "perf_sum",
            title = "6.3 求和性能",
            description = "测试 sum() 方法性能",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "data" to (1..1000000).map { it * 1.0 }
                    )
                )
                
                val start = System.currentTimeMillis()
                val sum = df.sum("data")
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "data" to (1..1000000).map { it * 1.0 }
                        )
                    )
                    
                    val start = System.currentTimeMillis()
                    val sum = df.sum("data")
                    val time = System.currentTimeMillis() - start
                    callback("✅ 求和性能测试\n数据量: 100万\n结果: $sum\n耗时: ${time}ms")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun meanPerformance(): Example {
        return Example(
            id = "perf_mean",
            title = "6.4 均值性能",
            description = "测试 mean() 方法性能",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "data" to (1..1000000).map { it * 1.0 }
                    )
                )
                
                val start = System.currentTimeMillis()
                val mean = df.mean("data")
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "data" to (1..1000000).map { it * 1.0 }
                        )
                    )
                    
                    val start = System.currentTimeMillis()
                    val mean = df.mean("data")
                    val time = System.currentTimeMillis() - start
                    callback("✅ 均值性能测试\n数据量: 100万\n结果: $mean\n耗时: ${time}ms")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun maxMinPerformance(): Example {
        return Example(
            id = "perf_max_min",
            title = "6.5 最大最小值性能",
            description = "测试 max() 和 min() 方法性能",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "data" to (1..1000000).map { it * 1.0 }
                    )
                )
                
                val start = System.currentTimeMillis()
                val max = df.max("data")
                val min = df.min("data")
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "data" to (1..1000000).map { it * 1.0 }
                        )
                    )
                    
                    val start = System.currentTimeMillis()
                    val max = df.max("data")
                    val min = df.min("data")
                    val time = System.currentTimeMillis() - start
                    callback("✅ 最大最小值性能测试\n数据量: 100万\n最大值: $max\n最小值: $min\n耗时: ${time}ms")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun variancePerformance(): Example {
        return Example(
            id = "perf_variance",
            title = "6.6 方差性能",
            description = "测试 variance() 方法性能",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "data" to (1..1000000).map { it * 1.0 }
                    )
                )
                
                val start = System.currentTimeMillis()
                val variance = df.variance("data")
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "data" to (1..1000000).map { it * 1.0 }
                        )
                    )
                    
                    val start = System.currentTimeMillis()
                    val variance = df.variance("data")
                    val time = System.currentTimeMillis() - start
                    callback("✅ 方差性能测试\n数据量: 100万\n结果: $variance\n耗时: ${time}ms")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun stdPerformance(): Example {
        return Example(
            id = "perf_std",
            title = "6.7 标准差性能",
            description = "测试 std() 方法性能",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "data" to (1..1000000).map { it * 1.0 }
                    )
                )
                
                val start = System.currentTimeMillis()
                val std = df.std("data")
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "data" to (1..1000000).map { it * 1.0 }
                        )
                    )
                    
                    val start = System.currentTimeMillis()
                    val std = df.std("data")
                    val time = System.currentTimeMillis() - start
                    callback("✅ 标准差性能测试\n数据量: 100万\n结果: $std\n耗时: ${time}ms")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun vectorizedAddPerformance(): Example {
        return Example(
            id = "perf_vectorized_add",
            title = "6.8 向量化加法性能",
            description = "测试向量化加法性能",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "col1" to (1..1000000).map { it * 1.0 },
                        "col2" to (1..1000000).map { it * 2.0 }
                    )
                )
                
                val start = System.currentTimeMillis()
                val result = df.vectorizedAdd("col1", "col2", "sum")
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "col1" to (1..1000000).map { it * 1.0 },
                            "col2" to (1..1000000).map { it * 2.0 }
                        )
                    )
                    
                    val start = System.currentTimeMillis()
                    val result = df.vectorizedAdd("col1", "col2", "sum")
                    val time = System.currentTimeMillis() - start
                    callback("✅ 向量化加法性能\n数据量: 100万\n耗时: ${time}ms\n结果形状: ${result.shape()}")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun vectorizedMultiplyPerformance(): Example {
        return Example(
            id = "perf_vectorized_multiply",
            title = "6.9 向量化乘法性能",
            description = "测试向量化乘法性能",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "col1" to (1..1000000).map { it * 1.0 },
                        "col2" to (1..1000000).map { it * 2.0 }
                    )
                )
                
                val start = System.currentTimeMillis()
                val result = df.vectorizedMultiply("col1", "col2", "product")
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "col1" to (1..1000000).map { it * 1.0 },
                            "col2" to (1..1000000).map { it * 2.0 }
                        )
                    )
                    
                    val start = System.currentTimeMillis()
                    val result = df.vectorizedMultiply("col1", "col2", "product")
                    val time = System.currentTimeMillis() - start
                    callback("✅ 向量化乘法性能\n数据量: 100万\n耗时: ${time}ms\n结果形状: ${result.shape()}")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun dotProductPerformance(): Example {
        return Example(
            id = "perf_dot_product",
            title = "6.10 点积性能",
            description = "测试点积计算性能",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "col1" to (1..1000000).map { it * 1.0 },
                        "col2" to (1..1000000).map { it * 2.0 }
                    )
                )
                
                val start = System.currentTimeMillis()
                val dotProduct = df.dotProduct("col1", "col2")
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "col1" to (1..1000000).map { it * 1.0 },
                            "col2" to (1..1000000).map { it * 2.0 }
                        )
                    )
                    
                    val start = System.currentTimeMillis()
                    val dotProduct = df.dotProduct("col1", "col2")
                    val time = System.currentTimeMillis() - start
                    callback("✅ 点积性能测试\n数据量: 100万\n结果: $dotProduct\n耗时: ${time}ms")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun normPerformance(): Example {
        return Example(
            id = "perf_norm",
            title = "6.11 范数性能",
            description = "测试范数计算性能",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "col1" to (1..1000000).map { it * 1.0 }
                    )
                )
                
                val start = System.currentTimeMillis()
                val norm = df.norm("col1")
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "col1" to (1..1000000).map { it * 1.0 }
                        )
                    )
                    
                    val start = System.currentTimeMillis()
                    val norm = df.norm("col1")
                    val time = System.currentTimeMillis() - start
                    callback("✅ 范数性能测试\n数据量: 100万\n结果: $norm\n耗时: ${time}ms")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun nullDetectionPerformance(): Example {
        return Example(
            id = "perf_null_detection",
            title = "6.12 空值检测性能",
            description = "测试空值检测性能",
            code = """
                val data = (1..1000000).map { if (it % 10 == 0) null else it * 1.0 }
                val df = andasDataFrame(
                    mapOf(
                        "data" to data
                    )
                )
                
                val start = System.currentTimeMillis()
                val nullDF = df.isnull()
                val nullIndices = df.findNullIndices("data")
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val data = (1..1000000).map { if (it % 10 == 0) null else it * 1.0 }
                    val df = andasDataFrame(
                        mapOf(
                            "data" to data
                        )
                    )
                    
                    val start = System.currentTimeMillis()
                    val nullDF = df.isnull()
                    val nullIndices = df.findNullIndices("data")
                    val time = System.currentTimeMillis() - start
                    callback("✅ 空值检测性能\n数据量: 100万\n空值数: ${nullIndices.size}\n耗时: ${time}ms")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun nullHandlingPerformance(): Example {
        return Example(
            id = "perf_null_handling",
            title = "6.13 空值处理性能",
            description = "测试空值处理性能",
            code = """
                val data = (1..1000000).map { if (it % 10 == 0) null else it * 1.0 }
                val df = andasDataFrame(
                    mapOf(
                        "data" to data
                    )
                )
                
                val start = System.currentTimeMillis()
                val filledDF = df.fillNull("data", 0.0)
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val data = (1..1000000).map { if (it % 10 == 0) null else it * 1.0 }
                    val df = andasDataFrame(
                        mapOf(
                            "data" to data
                        )
                    )
                    
                    val start = System.currentTimeMillis()
                    val filledDF = df.fillNull("data", 0.0)
                    val time = System.currentTimeMillis() - start
                    callback("✅ 空值处理性能\n数据量: 100万\n耗时: ${time}ms")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun groupBySumPerformance(): Example {
        return Example(
            id = "perf_group_by_sum",
            title = "6.14 分组求和性能",
            description = "测试分组求和性能",
            code = """
                val groups = (1..1000000).map { it % 100 }
                val values = (1..1000000).map { it * 1.0 }
                val df = andasDataFrame(
                    mapOf(
                        "group" to groups,
                        "value" to values
                    )
                )
                
                val start = System.currentTimeMillis()
                val result = df.groupBySum("group", "value")
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val groups = (1..1000000).map { it % 100 }
                    val values = (1..1000000).map { it * 1.0 }
                    val df = andasDataFrame(
                        mapOf(
                            "group" to groups,
                            "value" to values
                        )
                    )
                    
                    val start = System.currentTimeMillis()
                    val result = df.groupBySum("group", "value")
                    val time = System.currentTimeMillis() - start
                    callback("✅ 分组求和性能\n数据量: 100万\n分组数: 100\n耗时: ${time}ms\n结果: ${result.shape()}")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun sortPerformance(): Example {
        return Example(
            id = "perf_sort",
            title = "6.15 排序性能",
            description = "测试排序性能",
            code = """
                val data = (1..1000000).map { it * 1.0 }.shuffled()
                val df = andasDataFrame(
                    mapOf(
                        "data" to data
                    )
                )
                
                val start = System.currentTimeMillis()
                val sorted = df.sortValues("data")
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val data = (1..1000000).map { it * 1.0 }.shuffled()
                    val df = andasDataFrame(
                        mapOf(
                            "data" to data
                        )
                    )
                    
                    val start = System.currentTimeMillis()
                    val sorted = df.sortValues("data")
                    val time = System.currentTimeMillis() - start
                    callback("✅ 排序性能测试\n数据量: 100万\n耗时: ${time}ms")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun filterPerformance(): Example {
        return Example(
            id = "perf_filter",
            title = "6.16 筛选性能",
            description = "测试筛选性能",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "data" to (1..1000000).map { it * 1.0 }
                    )
                )
                
                val start = System.currentTimeMillis()
                val filtered = df.filterGreaterThan("data", 500000.0)
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "data" to (1..1000000).map { it * 1.0 }
                        )
                    )
                    
                    val start = System.currentTimeMillis()
                    val filtered = df.filterGreaterThan("data", 500000.0)
                    val time = System.currentTimeMillis() - start
                    callback("✅ 筛选性能测试\n数据量: 100万\n耗时: ${time}ms\n结果: ${filtered.shape()}")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun mergePerformance(): Example {
        return Example(
            id = "perf_merge",
            title = "6.17 合并性能",
            description = "测试数据合并性能",
            code = """
                val df1 = andasDataFrame(
                    mapOf(
                        "id" to (1..100000).toList(),
                        "name" to (1..100000).map { "User\$\it" }
                    )
                )
                
                val df2 = andasDataFrame(
                    mapOf(
                        "id" to (1..100000).toList(),
                        "value" to (1..100000).map { it * 1.5 }
                    )
                )
                
                val start = System.currentTimeMillis()
                val merged = df1.merge(df2, "id")
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df1 = andasDataFrame(
                        mapOf(
                            "id" to (1..100000).toList(),
                            "name" to (1..100000).map { "User$it" }
                        )
                    )
                    
                    val df2 = andasDataFrame(
                        mapOf(
                            "id" to (1..100000).toList(),
                            "value" to (1..100000).map { it * 1.5 }
                        )
                    )
                    
                    val start = System.currentTimeMillis()
                    val merged = df1.merge(df2, "id")
                    val time = System.currentTimeMillis() - start
                    callback("✅ 合并性能测试\n数据量: 10万 x 2\n耗时: ${time}ms\n结果: ${merged.shape()}")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun samplingPerformance(): Example {
        return Example(
            id = "perf_sampling",
            title = "6.18 采样性能",
            description = "测试数据采样性能",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "id" to (1..1000000).toList(),
                        "value" to (1..1000000).map { it * 1.5 }
                    )
                )
                
                val start = System.currentTimeMillis()
                val sample = df.sample(10000)
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "id" to (1..1000000).toList(),
                            "value" to (1..1000000).map { it * 1.5 }
                        )
                    )
                    
                    val start = System.currentTimeMillis()
                    val sample = df.sample(10000)
                    val time = System.currentTimeMillis() - start
                    callback("✅ 采样性能测试\n数据量: 100万\n采样数: 1万\n耗时: ${time}ms\n结果: ${sample.shape()}")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun batchProcessingPerformance(): Example {
        return Example(
            id = "perf_batch_processing",
            title = "6.19 批量处理性能",
            description = "测试批量处理性能",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "id" to (1..1000000).toList(),
                        "value" to (1..1000000).map { it * 1.5 }
                    )
                )
                
                val start = System.currentTimeMillis()
                val processed = df.processBatch("value", batchSize = 10000)
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "id" to (1..1000000).toList(),
                            "value" to (1..1000000).map { it * 1.5 }
                        )
                    )
                    
                    val start = System.currentTimeMillis()
                    val processed = df.processBatch("value", batchSize = 10000)
                    val time = System.currentTimeMillis() - start
                    callback("✅ 批量处理性能测试\n数据量: 100万\n耗时: ${time}ms\n结果: ${processed.shape()}")
                } catch (e: Exception) {
                    callback("❌ 性能测试失败: ${e.message}")
                }
            }
        )
    }
    
    private fun jniVsKotlin(): Example {
        return Example(
            id = "perf_jni_vs_kotlin",
            title = "6.20 JNI vs Kotlin 性能对比",
            description = "对比原生 JNI 和 Kotlin 实现的性能差异",
            code = """
                val data = (1..1000000).map { it.toDouble() }
                val series = Andas.getInstance().createSeries(data)
                
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
                    val series = Andas.getInstance().createSeries(data)
                    
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
    
    private fun comprehensiveTest(): Example {
        return Example(
            id = "perf_comprehensive",
            title = "6.21 综合性能测试",
            description = "完整的数据处理流程性能测试",
            code = """
                // 创建大数据集
                val df = andasDataFrame(
                    mapOf(
                        "id" to (1..500000).toList(),
                        "value" to (1..500000).map { it * 1.0 },
                        "category" to (1..500000).map { it % 10 }
                    )
                )
                
                // 链式操作
                val start = System.currentTimeMillis()
                val result = df
                    .filterGreaterThan("value", 250000.0)
                    .addColumn("doubled") { row ->
                        val value = row["value"]?.get(0) as? Double
                        value?.times(2) ?: 0.0
                    }
                    .groupBySum("category", "doubled")
                val time = System.currentTimeMillis() - start
            """.trimIndent(),
            action = { context, callback ->
                try {
                    // 创建大数据集
                    val df = andasDataFrame(
                        mapOf(
                            "id" to (1..50000).toList(),
                            "value" to (1..50000).map { it * 1.0 },
                            "category" to (1..50000).map { it % 10 }
                        )
                    )
                    
                    // 链式操作
                    val start = System.currentTimeMillis()
                    var result = df
                        .filterGreaterThan("value", 25000.0)

                    callback("${result.columns()}")
                    Log.i("TAG", "comprehensiveTest: ${result.columns()}")
                    result = result
                        .addColumn("doubled") { row ->
                            val value = row["value"] as Double
                            value.times(2)
                        }
                        .groupBySum("category", "doubled")
                    callback("$result")
                    val time = System.currentTimeMillis() - start
                    
                    callback("✅ 综合性能测试\n数据量: 50000\n操作: 筛选 + 新增列 + 分组求和\n耗时: ${time}ms\n结果: ${result.shape()}")
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback("❌ 综合测试失败: ${e.printStackTrace()}")
                }
            }
        )
    }
}
