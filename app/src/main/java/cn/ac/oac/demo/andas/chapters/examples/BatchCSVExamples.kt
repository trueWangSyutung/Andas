package cn.ac.oac.demo.andas.chapters.examples

import android.content.Context
import cn.ac.oac.demo.andas.chapters.Example
import cn.ac.oac.libs.andas.utils.BatchCSVUtils
import cn.ac.oac.libs.andas.entity.DataFrame
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * BatchCSVUtils 示例类
 * 演示如何使用分批处理工具处理大型CSV文件
 */
object BatchCSVExamples {
    
    /**
     * 获取所有示例方法
     */
    fun getAllExamples(): Map<String, (Context) -> String> {
        return mapOf(
            "基础统计示例" to ::basicStatsExample,
            "向量化运算示例" to ::vectorizedOpsExample,
            "数据处理示例" to ::dataProcessingExample,
            "分组聚合示例" to ::groupByExample,
            "高级功能示例" to ::advancedFeaturesExample,
            "性能优化示例" to ::performanceExample,
            "销售数据分析" to ::salesAnalysisExample,
            "错误处理示例" to ::errorHandlingExample
        )
    }
    
    /**
     * 1. 基础统计示例
     */
    private fun basicStatsExample(context: Context): String {
        val result = StringBuilder()
        result.append("=== 基础统计示例 ===\n\n")
        
        result.append("1. 单列求和:\n")
        // 每次都重新获取流，避免流关闭问题
        val sum = BatchCSVUtils.batchSum(getTestInputStream(context), "double_col_10", 1000)
        result.append("   double_col_10 总和: $sum\n\n")
        
        result.append("2. 多列求和:\n")
        val sums = BatchCSVUtils.batchSumMultiple(
            getTestInputStream(context), 
            listOf("double_col_10", "double_col_11", "double_col_12"), 
            1000
        )
        sums.forEach { (col, value) ->
            result.append("   $col: $value\n")
        }
        result.append("\n")
        
        result.append("3. 描述性统计:\n")
        val stats = BatchCSVUtils.batchDescribe(getTestInputStream(context), "double_col_10", 1000)
        stats.forEach { (key, value) ->
            result.append("   $key: ${"%.2f".format(value)}\n")
        }
        
        return result.toString()
    }
    
    /**
     * 2. 向量化运算示例
     */
    private fun vectorizedOpsExample(context: Context): String {
        val result = StringBuilder()
        result.append("=== 向量化运算示例 ===\n\n")
        
        result.append("1. 向量加法:\n")
        val addDF = BatchCSVUtils.batchVectorizedAdd(
            getTestInputStream(context), "double_col_10", "double_col_11", "double_col_10_plus_11", 1000
        )
        result.append("   添加了 double_col_10_plus_11 列，前5行:\n")
        for (i in 0 until minOf(5, addDF.shape().first)) {
            val row = addDF.iloc(i)
            result.append("   行 $i: double_col_10=${row["double_col_10"]}, double_col_11=${row["double_col_11"]}, double_col_10_plus_11=${row["double_col_10_plus_11"]}\n")
        }
        result.append("\n")
        
        result.append("2. 向量乘法:\n")
        val mulDF = BatchCSVUtils.batchVectorizedMultiply(
            getTestInputStream(context), "double_col_10", "double_col_11", "double_col_10_times_11", 1000
        )
        result.append("   添加了 double_col_10_times_11 列，前3行:\n")
        for (i in 0 until minOf(3, mulDF.shape().first)) {
            val row = mulDF.iloc(i)
            result.append("   行 $i: double_col_10=${row["double_col_10"]}, double_col_11=${row["double_col_11"]}, double_col_10_times_11=${row["double_col_10_times_11"]}\n")
        }
        result.append("\n")
        
        result.append("3. 点积计算:\n")
        val dotProduct = BatchCSVUtils.batchDotProduct(
            getTestInputStream(context), "double_col_10", "double_col_11", 1000
        )
        result.append("   double_col_10 和 double_col_11 的点积: ${"%.2f".format(dotProduct)}\n")
        
        return result.toString()
    }
    
    /**
     * 3. 数据处理示例
     */
    private fun dataProcessingExample(context: Context): String {
        val result = StringBuilder()
        result.append("=== 数据处理示例 ===\n\n")
        
        result.append("1. 数据归一化:\n")
        val normalizedDF = BatchCSVUtils.batchNormalize(getTestInputStream(context), "double_col_10", 1000)
        val firstRow = normalizedDF.iloc(0)
        result.append("   归一化后第一行 double_col_10 值: ${"%.2f".format(firstRow["double_col_10"] as Double)}\n\n")
        
        result.append("2. 布尔筛选 (double_col_10 > 5000):\n")
        val filteredDF = BatchCSVUtils.batchFilterGreaterThan(
            getTestInputStream(context), "double_col_10", 5000.0, 1000
        )
        result.append("   筛选结果行数: ${filteredDF.shape().first}\n")
        result.append("   前3行数据:\n")
        for (i in 0 until minOf(3, filteredDF.shape().first)) {
            val row = filteredDF.iloc(i)
            result.append("   ${row["double_col_10"]}, ${row["double_col_11"]}, ${row["double_col_12"]}\n")
        }
        result.append("\n")
        
        result.append("3. 数据采样 (采样5条):\n")
        val sampleDF = BatchCSVUtils.batchSample(getTestInputStream(context), 5, 1000)
        result.append("   采样结果行数: ${sampleDF.shape().first}\n")
        
        return result.toString()
    }
    
    /**
     * 4. 分组聚合示例
     */
    private fun groupByExample(context: Context): String {
        val result = StringBuilder()
        result.append("=== 分组聚合示例 ===\n\n")
        
        result.append("1. 分组计数:\n")
        // 使用字符串列进行分组
        val counts = BatchCSVUtils.batchGroupByCount(getTestInputStream(context), "string_col_1", 1000)
        counts.forEach { (group, count) ->
            result.append("   $group: $count 条\n")
        }
        result.append("\n")
        
        result.append("2. 分组求和:\n")
        val sums = BatchCSVUtils.batchGroupBySum(
            getTestInputStream(context), "string_col_1", "double_col_10", 1000
        )
        sums.forEach { (group, sum) ->
            result.append("   $group: ${"%.2f".format(sum)}\n")
        }
        result.append("\n")
        
        result.append("3. 分组均值:\n")
        val means = BatchCSVUtils.batchGroupByMean(
            getTestInputStream(context), "string_col_1", "double_col_11", 1000
        )
        means.forEach { (group, mean) ->
            result.append("   $group: ${"%.2f".format(mean)}\n")
        }
        
        return result.toString()
    }
    
    /**
     * 5. 高级功能示例
     */
    private fun advancedFeaturesExample(context: Context): String {
        val result = StringBuilder()
        result.append("=== 高级功能示例 ===\n\n")
        
        result.append("1. 数据排序:\n")
        val sortedDF = BatchCSVUtils.batchSort(getTestInputStream(context), "double_col_10", descending = true, 1000)
        result.append("   按 double_col_10 降序，前3行:\n")
        for (i in 0 until minOf(3, sortedDF.shape().first)) {
            val row = sortedDF.iloc(i)
            result.append("   ${row["double_col_10"]}\n")
        }
        result.append("\n")
        
        result.append("2. 条件筛选:\n")
        val conditionDF = BatchCSVUtils.batchFilterWithCondition(
            getTestInputStream(context),
            { row ->
                val double10 = row["double_col_10"] as? Double ?: 0.0
                val double11 = row["double_col_11"] as? Double ?: 0.0
                double10 > 4000 && double11 > 500
            },
            1000
        )
        result.append("   满足条件的行数: ${conditionDF.shape().first}\n\n")
        
        result.append("3. 累积求和:\n")
        val cumDF = BatchCSVUtils.batchCumulativeOperation(
            getTestInputStream(context), "double_col_10", "sum", 1000
        )
        result.append("   累积求和，前3行:\n")
        for (i in 0 until minOf(3, cumDF.shape().first)) {
            val row = cumDF.iloc(i)
            result.append("   原值: ${row["double_col_10"]}, 累积: ${row["double_col_10_cumulative_sum"]}\n")
        }
        
        return result.toString()
    }
    
    /**
     * 6. 性能优化示例
     */
    private fun performanceExample(context: Context): String {
        val result = StringBuilder()
        result.append("=== 性能优化示例 ===\n\n")
        
        result.append("1. 检查原生库可用性:\n")
        val isNativeAvailable = BatchCSVUtils.isNativeBatchAvailable()
        result.append("   原生库可用: $isNativeAvailable\n\n")
        
        result.append("2. 批处理大小优化:\n")
        val fileSize = File("app/src/main/assets/largest.csv").length()
        val recommendedBatchSize = when {
            fileSize < 10_000_000 -> 1000
            fileSize < 1_000_000_000 -> 10000
            else -> 50000
        }
        result.append("   文件大小: ${fileSize / 1024 / 1024}MB\n")
        result.append("   推荐批处理大小: $recommendedBatchSize\n\n")
        
        result.append("3. 批量操作优化:\n")
        val startTime = System.currentTimeMillis()
        val stats = BatchCSVUtils.batchDescribeAll(getTestInputStream(context), 1000)
        val duration = System.currentTimeMillis() - startTime
        result.append("   批量统计耗时: ${duration}ms\n")
        result.append("   统计列数: ${stats.size}\n")
        
        return result.toString()
    }
    
    /**
     * 7. 数据分析示例
     */
    private fun salesAnalysisExample(context: Context): String {
        val result = StringBuilder()
        result.append("=== 数据分析示例 ===\n\n")
        
        // 基础统计
        val totalDouble10 = BatchCSVUtils.batchSum(getTestInputStream(context), "double_col_10", 1000)
        val avgDouble10 = BatchCSVUtils.batchMean(getTestInputStream(context), "double_col_10", 1000)
        
        result.append("1. 基础统计:\n")
        result.append("   double_col_10 总和: ${"%.2f".format(totalDouble10)}\n")
        result.append("   double_col_10 平均值: ${"%.2f".format(avgDouble10)}\n\n")
        
        // 分组统计
        val sumByString = BatchCSVUtils.batchGroupBySum(
            getTestInputStream(context), "string_col_1", "double_col_10", 1000
        )
        
        result.append("2. 字符串分组统计:\n")
        sumByString.forEach { (group, sum) ->
            result.append("   $group: ${"%.2f".format(sum)}\n")
        }
        result.append("\n")
        
        // 高价值数据识别
        val threshold = totalDouble10 * 0.3  // 前30%的数据
        val highValueData = BatchCSVUtils.batchFilterGreaterThan(
            getTestInputStream(context), "double_col_10", threshold, 1000
        )
        
        result.append("3. 高价值数据识别:\n")
        result.append("   阈值: ${"%.2f".format(threshold)}\n")
        result.append("   高价值数据行数: ${highValueData.shape().first}\n\n")
        
        // 多维度聚合
        val operations = mapOf(
            "double_col_10" to listOf("sum", "mean", "max", "count"),
            "double_col_11" to listOf("sum", "mean")
        )
        val summary = BatchCSVUtils.batchAggregateMultiple(
            getTestInputStream(context), operations, 1000
        )
        
        result.append("4. 多维度聚合:\n")
        for (i in 0 until summary.shape().first) {
            val row = summary.iloc(i)
            val operation = row["operation"]
            val value = row["value"]
            result.append("   $operation: ${"%.2f".format(value as Double)}\n")
        }
        
        return result.toString()
    }
    
    /**
     * 8. 错误处理示例
     */
    private fun errorHandlingExample(context: Context): String {
        val result = StringBuilder()
        result.append("=== 错误处理示例 ===\n\n")
        
        // 1. 流验证
        result.append("1. 流支持检查:\n")
        val inputStream = getTestInputStream(context)
        if (!inputStream.markSupported()) {
            result.append("   ⚠️  警告: 流不支持 mark/reset，可能影响性能\n")
        } else {
            result.append("   ✓ 流支持 mark/reset\n")
        }
        inputStream.close() // 关闭流
        result.append("\n")
        
        // 2. 列验证
        result.append("2. 列存在性验证:\n")
        try {
            val testStream = getTestInputStream(context)
            val df = cn.ac.oac.libs.andas.entity.DataFrameIO.readCSV(testStream)
            val hasDouble10 = df.columns().contains("double_col_10")
            val hasUnknown = df.columns().contains("unknown_column")
            result.append("   double_col_10 列存在: $hasDouble10\n")
            result.append("   unknown_column 列存在: $hasUnknown\n")
        } catch (e: Exception) {
            result.append("   验证失败: ${e.message}\n")
        }
        result.append("\n")
        
        // 3. 异常处理
        result.append("3. 异常处理示例:\n")
        try {
            val sum = BatchCSVUtils.batchSum(getTestInputStream(context), "double_col_10", 1000)
            result.append("   计算成功: $sum\n")
        } catch (e: Exception) {
            result.append("   计算失败: ${e.message}\n")
        }
        
        return result.toString()
    }
    
    /**
     * 获取测试数据输入流
     * 每次调用都返回新的输入流，避免流关闭问题
     */
    private fun getTestInputStream(context: Context): InputStream {
        // 使用内置的测试数据文件
        return context.assets.open("largest.csv")
    }
    
    /**
     * 运行指定示例
     */
    fun runExample(exampleName: String, context: Context): String {
        val examples = getAllExamples()
        return examples[exampleName]?.invoke(context) ?: "未找到示例: $exampleName"
    }
    
    /**
     * 获取所有示例（符合 ChapterManager 接口）
     * 使用协程在子线程中执行耗时操作
     */
    fun getExamples(): List<Example> {
        return listOf(
            Example(
                id = "basic_stats",
                title = "基础统计示例",
                description = "演示单列求和、多列求和和描述性统计",
                code = "BatchCSVUtils.batchSum(inputStream, \"double_col_10\", 1000)",
                action = { context, callback ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val result = basicStatsExample(context)
                            withContext(Dispatchers.Main) {
                                callback(result)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                callback("❌ 执行失败: ${e.message}\n${e.stackTraceToString()}")
                            }
                        }
                    }
                }
            ),
            Example(
                id = "vectorized_ops",
                title = "向量化运算示例",
                description = "演示向量加法、向量乘法和点积计算",
                code = "BatchCSVUtils.batchVectorizedAdd(inputStream, \"double_col_10\", \"double_col_11\", \"double_col_10_plus_11\", 1000)",
                action = { context, callback ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val result = vectorizedOpsExample(context)
                            withContext(Dispatchers.Main) {
                                callback(result)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                callback("❌ 执行失败: ${e.message}\n${e.stackTraceToString()}")
                            }
                        }
                    }
                }
            ),
            Example(
                id = "data_processing",
                title = "数据处理示例",
                description = "演示数据归一化、布尔筛选和数据采样",
                code = "BatchCSVUtils.batchNormalize(inputStream, \"double_col_10\", 1000)",
                action = { context, callback ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val result = dataProcessingExample(context)
                            withContext(Dispatchers.Main) {
                                callback(result)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                callback("❌ 执行失败: ${e.message}\n${e.stackTraceToString()}")
                            }
                        }
                    }
                }
            ),
            Example(
                id = "group_by",
                title = "分组聚合示例",
                description = "演示分组计数、分组求和和分组均值",
                code = "BatchCSVUtils.batchGroupBySum(inputStream, \"string_col_1\", \"double_col_10\", 1000)",
                action = { context, callback ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val result = groupByExample(context)
                            withContext(Dispatchers.Main) {
                                callback(result)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                callback("❌ 执行失败: ${e.message}\n${e.stackTraceToString()}")
                            }
                        }
                    }
                }
            ),
            Example(
                id = "advanced_features",
                title = "高级功能示例",
                description = "演示数据排序、条件筛选和累积计算",
                code = "BatchCSVUtils.batchSort(inputStream, \"double_col_10\", descending = true, 1000)",
                action = { context, callback ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val result = advancedFeaturesExample(context)
                            withContext(Dispatchers.Main) {
                                callback(result)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                callback("❌ 执行失败: ${e.message}\n${e.stackTraceToString()}")
                            }
                        }
                    }
                }
            ),
            Example(
                id = "performance",
                title = "性能优化示例",
                description = "演示原生库检查、批处理优化和批量操作",
                code = "BatchCSVUtils.batchDescribeAll(inputStream, 1000)",
                action = { context, callback ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val result = performanceExample(context)
                            withContext(Dispatchers.Main) {
                                callback(result)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                callback("❌ 执行失败: ${e.message}\n${e.stackTraceToString()}")
                            }
                        }
                    }
                }
            ),
            Example(
                id = "sales_analysis",
                title = "数据分析示例",
                description = "综合数据统计和分析",
                code = "BatchCSVUtils.batchGroupBySum(inputStream, \"string_col_1\", \"double_col_10\", 1000)",
                action = { context, callback ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val result = salesAnalysisExample(context)
                            withContext(Dispatchers.Main) {
                                callback(result)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                callback("❌ 执行失败: ${e.message}\n${e.stackTraceToString()}")
                            }
                        }
                    }
                }
            ),
            Example(
                id = "error_handling",
                title = "错误处理示例",
                description = "演示流验证、列验证和异常处理",
                code = "try { BatchCSVUtils.batchSum(inputStream, \"double_col_10\", 1000) } catch (e: Exception) { ... }",
                action = { context, callback ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val result = errorHandlingExample(context)
                            withContext(Dispatchers.Main) {
                                callback(result)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                callback("❌ 执行失败: ${e.message}\n${e.stackTraceToString()}")
                            }
                        }
                    }
                }
            )
        )
    }

    /**
     * 运行所有示例
     */
    fun runAllExamples(context: Context): String {
        val result = StringBuilder()
        val examples = getAllExamples()
        
        examples.forEach { (name, func) ->
            result.append(func.invoke(context))
            result.append("\n${"=".repeat(50)}\n\n")
        }
        
        return result.toString()
    }
}
