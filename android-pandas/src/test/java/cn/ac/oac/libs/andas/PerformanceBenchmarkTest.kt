package cn.ac.oac.libs.andas

import cn.ac.oac.libs.andas.core.AndaThreadPool
import cn.ac.oac.libs.andas.entity.DataFrame
import org.junit.Test
import org.junit.Before
import org.junit.After
import java.io.File
import java.util.concurrent.Future
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Android DataFrame性能基准测试套件
 * 基于large_dataset.csv进行完整的性能测试
 */
class PerformanceBenchmarkTest {
    
    private val testCsvPath = "large_dataset.csv"
    private val results = mutableMapOf<String, TestResult>()
    
    data class TestResult(
        val testName: String,
        val timeMs: Double,
        val memoryMb: Double,
        val success: Boolean,
        val extraInfo: String = ""
    )
    
    private fun measurePerformance(testName: String, block: () -> Unit): TestResult {
        // 预热
        try {
            block()
        } catch (e: Exception) {
            // 忽略预热错误
        }
        
        // 正式测量
        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val startTime = System.nanoTime()
        
        block()
        
        val endTime = System.nanoTime()
        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        val timeMs = (endTime - startTime) / 1_000_000.0
        val memoryMb = (endMemory - startMemory) / 1024.0 / 1024.0
        
        return TestResult(testName, timeMs, memoryMb, true)
    }
    
    private fun saveResults() {
        val resultsFile = File("android_benchmark_results.json")
        val json = StringBuilder()
        json.append("{\n")
        json.append("  \"benchmark\": \"Android DataFrame Performance Test\",\n")
        json.append("  \"timestamp\": \"${java.time.LocalDateTime.now()}\",\n")
        json.append("  \"tests\": [\n")
        
        results.values.forEachIndexed { index, result ->
            json.append("    {\n")
            json.append("      \"test_name\": \"${result.testName}\",\n")
            json.append("      \"time_ms\": ${result.timeMs},\n")
            json.append("      \"memory_mb\": ${result.memoryMb},\n")
            json.append("      \"success\": ${result.success}\n")
            if (result.extraInfo.isNotEmpty()) {
                json.append("      ,\"extra_info\": \"${result.extraInfo}\"\n")
            }
            json.append("    }")
            if (index < results.size - 1) json.append(",")
            json.append("\n")
        }
        
        json.append("  ]\n")
        json.append("}")
        
        resultsFile.writeText(json.toString())
        println("✅ 测试结果已保存到: ${resultsFile.absolutePath}")
    }
    
    private fun printSummary() {
        println("\n" + "=".repeat(60))
        println("ANDROID基准测试摘要")
        println("=".repeat(60))
        
        val totalTests = results.size
        val passedTests = results.values.count { it.success }
        
        println("总测试数: $totalTests")
        println("通过数: $passedTests")
        println("通过率: ${passedTests * 100.0 / totalTests}%")
        println("\n详细结果:")
        
        results.values.forEach { result ->
            val status = if (result.success) "✅" else "❌"
            val extra = if (result.extraInfo.isNotEmpty()) " (${result.extraInfo})" else ""
            println("$status ${result.testName}: ${"%.2f".format(result.timeMs)}ms, ${"%.2f".format(result.memoryMb)}MB$extra")
        }
    }
    
    // ========== 基础性能测试 ==========
    
    @Test
    fun test01_CSVReadPerformance() {
        println("1. 测试CSV读取性能...")
        
        val result = measurePerformance("CSV读取") {
            val df = Andas.readCsv(testCsvPath)
            assertTrue(df.rows > 0, "应该成功读取数据")
        }
        
        results["CSV读取"] = result
    }
    
    @Test
    fun test02_TypeInferencePerformance() {
        println("2. 测试类型推断性能...")
        
        val df = Andas.readCsv(testCsvPath)
        
        val result = measurePerformance("类型推断") {
            df.inferColumnTypes()
        }
        
        results["类型推断"] = result
    }
    
    @Test
    fun test03_MemoryBaseline() {
        println("3. 测试内存占用基准...")
        
        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val startTime = System.currentTimeMillis()
        
        val df = Andas.readCsv(testCsvPath)
        
        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val endTime = System.currentTimeMillis()
        
        val memoryUsed = (endMemory - startMemory) / 1024 / 1024
        val timeUsed = endTime - startTime
        
        results["内存基准"] = TestResult("内存基准", timeUsed.toDouble(), memoryUsed.toDouble(), true)
        
        println("   内存使用: ${memoryUsed}MB, 时间: ${timeUsed}ms")
    }
    
    // ========== 数据操作测试 ==========
    
    @Test
    fun test04_DataFilteringPerformance() {
        println("4. 测试数据过滤性能...")
        
        val df = Andas.readCsv(testCsvPath)
        
        val result = measurePerformance("数据过滤") {
            val filtered = df.filter { it.getLong("long_col_4") > 1000000000000000000L }
            assertTrue(filtered.rows > 0, "过滤应该返回结果")
        }
        
        results["数据过滤"] = result
    }
    
    @Test
    fun test05_SortingPerformance() {
        println("5. 测试排序性能...")
        
        val df = Andas.readCsv(testCsvPath)
        
        val result = measurePerformance("快速排序") {
            val sorted = df.sortBy("double_col_10")
            assertTrue(sorted.rows > 0, "排序应该返回结果")
        }
        
        results["快速排序"] = result
    }
    
    @Test
    fun test06_StatisticalOperations() {
        println("6. 测试统计计算性能...")
        
        val df = Andas.readCsv(testCsvPath)
        
        val result = measurePerformance("统计计算") {
            val mean = df.getDoubleColumn("double_col_10").mean()
            val std = df.getDoubleColumn("double_col_10").std()
            val max = df.getDoubleColumn("double_col_10").max()
            val min = df.getDoubleColumn("double_col_10").min()
            
            assertTrue(!mean.isNaN(), "均值不应该为NaN")
            assertTrue(!std.isNaN(), "标准差不应该为NaN")
        }
        
        results["统计计算"] = result
    }
    
    // ========== 并发性能测试 ==========
    
    @Test
    fun test07_ConcurrentProcessing() {
        println("7. 测试并发处理性能...")
        
        val df = Andas.readCsv(testCsvPath)
        
        val result = measurePerformance("4线程并发计算") {
            val threadPool = AndaThreadPool(4)
            val futures = mutableListOf<Future<Double>>()
            
            // 并发计算4列的均值
            futures.add(threadPool.submit { df.getDoubleColumn("double_col_10").mean() })
            futures.add(threadPool.submit { df.getDoubleColumn("double_col_11").mean() })
            futures.add(threadPool.submit { df.getDoubleColumn("double_col_12").mean() })
            futures.add(threadPool.submit { df.getDoubleColumn("double_col_13").mean() })
            
            // 等待所有任务完成
            val results = futures.map { it.get() }
            
            // 验证结果
            results.forEach { 
                assertTrue(!it.isNaN(), "并发计算结果不应该为NaN") 
            }
        }
        
        results["4线程并发计算"] = result
    }
    
    // ========== 内存压力测试 ==========
    
    @Test
    fun test08_MemoryPressure() {
        println("8. 测试内存压力...")
        
        val dataSizes = listOf(100, 1000, 10000)
        
        for (size in dataSizes) {
            // 生成测试数据
            val testData = generateTestData(size)
            val fileName = "test_${size}.csv"
            saveTestData(testData, fileName)
            
            val result = measurePerformance("数据规模${size}行") {
                val df = Andas.readCsv(fileName)
                val filtered = df.filter { it.getLong("long_col_4") > 0 }
                val sorted = filtered.sortBy("double_col_10")
                val mean = sorted.getDoubleColumn("double_col_10").mean()
                
                assertTrue(!mean.isNaN(), "计算不应该返回NaN")
            }
            
            results["数据规模${size}行"] = result
            
            // 清理临时文件
            File(fileName).delete()
        }
    }
    
    // ========== 精度与正确性测试 ==========
    
    @Test
    fun test09_NumericalAccuracy() {
        println("9. 测试数值计算精度...")
        
        val df = Andas.readCsv(testCsvPath)
        
        val doubleCol = df.getDoubleColumn("double_col_10")
        val mean = doubleCol.mean()
        val std = doubleCol.std()
        
        // 手动验证
        val values = doubleCol.toTypedArray()
        val manualMean = values.average()
        val manualStd = sqrt(values.map { (it - manualMean).pow(2) }.sum() / values.size)
        
        val meanMatch = Math.abs(manualMean - mean) < 0.0001
        val stdMatch = Math.abs(manualStd - std) < 0.0001
        
        results["数值精度验证"] = TestResult(
            "数值精度验证", 
            0.0, 
            0.0, 
            meanMatch && stdMatch,
            "mean_match=$meanMatch, std_match=$stdMatch"
        )
        
        assertTrue(meanMatch, "均值计算应该精确")
        assertTrue(stdMatch, "标准差计算应该精确")
    }
    
    @Test
    fun test10_ComprehensivePerformance() {
        println("10. 测试综合性能...")
        
        val df = Andas.readCsv(testCsvPath)
        
        val result = measurePerformance("综合操作链") {
            // 链式操作: 过滤 -> 排序 -> 统计
            val result = df.filter { it.getLong("long_col_4") > 1000000000000000000L }
                .sortBy("double_col_10")
                .getDoubleColumn("double_col_10")
                .mean()
            
            assertTrue(!result.isNaN(), "综合操作不应该返回NaN")
        }
        
        results["综合操作链"] = result
    }
    
    // ========== 辅助方法 ==========
    
    private fun generateTestData(rows: Int): DataFrame {
        val stringCol1 = Array(rows) { "ID_${it.toString().padStart(6, '0')}" }
        val stringCol2 = Array(rows) { "CODE_${it.toString().padStart(3, '0')}" }
        val stringCol3 = Array(rows) { "TYPE_${(it % 100).toString().padStart(2, '0')}" }
        
        val longCol4 = Array(rows) { (Math.random() * 1e18).toLong() + 1e15.toLong() }
        val longCol5 = Array(rows) { (Math.random() * 1e18).toLong() + 1e15.toLong() }
        val longCol6 = Array(rows) { (Math.random() * 1e18).toLong() + 1e15.toLong() }
        
        val intCol7 = Array(rows) { (Math.random() * 4e9).toInt() - 2e9.toInt() }
        val intCol8 = Array(rows) { (Math.random() * 4e9).toInt() - 2e9.toInt() }
        val intCol9 = Array(rows) { (Math.random() * 4e9).toInt() - 2e9.toInt() }
        
        val doubleCol10 = Array(rows) { Math.random() * 2e6 - 1e6 }
        val doubleCol11 = Array(rows) { Math.random() * 2e6 - 1e6 }
        val doubleCol12 = Array(rows) { Math.random() * 2e6 - 1e6 }
        
        val floatCol13 = Array(rows) { (Math.random() * 2e5 - 1e5).toFloat() }
        val floatCol14 = Array(rows) { (Math.random() * 2e5 - 1e5).toFloat() }
        val floatCol15 = Array(rows) { (Math.random() * 2e5 - 1e5).toFloat() }
        
        return DataFrame(
            mapOf(
                "string_col_1" to stringCol1,
                "string_col_2" to stringCol2,
                "string_col_3" to stringCol3,
                "long_col_4" to longCol4,
                "long_col_5" to longCol5,
                "long_col_6" to longCol6,
                "int_col_7" to intCol7,
                "int_col_8" to intCol8,
                "int_col_9" to intCol9,
                "double_col_10" to doubleCol10,
                "double_col_11" to doubleCol11,
                "double_col_12" to doubleCol12,
                "float_col_13" to floatCol13,
                "float_col_14" to floatCol14,
                "float_col_15" to floatCol15
            )
        )
    }
    
    private fun saveTestData(df: DataFrame, fileName: String) {
        val csvContent = StringBuilder()
        
        // Header
        val columns = df.columns.keys.toList()
        csvContent.append(columns.joinToString(","))
        csvContent.append("\n")
        
        // Data
        for (i in 0 until df.rows) {
            val row = columns.map { col ->
                when (val value = df[col]) {
                    is Array<*> -> value[i].toString()
                    else -> value.toString()
                }
            }
            csvContent.append(row.joinToString(","))
            csvContent.append("\n")
        }
        
        File(fileName).writeText(csvContent.toString())
    }
    
    @After
    fun tearDown() {
        // 保存所有测试结果
        if (results.size >= 10) {
            saveResults()
            printSummary()
        }
    }
}
