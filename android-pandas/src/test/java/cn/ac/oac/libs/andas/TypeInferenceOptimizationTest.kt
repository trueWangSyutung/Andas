package cn.ac.oac.libs.andas

import cn.ac.oac.libs.andas.entity.DataFrame
import org.junit.Test
import java.io.File
import java.io.FileWriter

/**
 * 类型推断优化测试
 * 验证单样本类型推断的性能改进
 */
class TypeInferenceOptimizationTest {

    @Test
    fun testSingleSampleTypeInference() {
        println("=== 单样本类型推断优化测试 ===")
        
        // 创建测试数据
        val testData = createLargeCSV()
        
        // 测试优化后的性能
        val startTime = System.currentTimeMillis()
        val df = DataFrame.readCSV(testData, autoType = true)
        val endTime = System.currentTimeMillis()
        
        println("读取 ${countLines(testData)} 行数据耗时: ${endTime - startTime}ms")
        println("DataFrame形状: ${df.shape()}")
        println("数据类型: ${df.dtypes()}")
        
        // 验证数据正确性
        assert(df.shape().first > 0)
        assert(df.columns().size == 4)
        
        // 清理测试文件
        testData.delete()
        
        println("测试完成！")
    }
    
    @Test
    fun testPerformanceComparison() {
        println("\n=== 性能对比测试 ===")
        
        // 创建不同大小的测试数据
        val sizes = listOf(100, 1000, 5000)
        
        for (size in sizes) {
            val testData = createTestCSV(size)
            
            // 测试优化后的性能
            val startTime = System.currentTimeMillis()
            val df = DataFrame.readCSV(testData, autoType = true)
            val endTime = System.currentTimeMillis()
            
            println("数据量: $size 行, 耗时: ${endTime - startTime}ms")
            
            testData.delete()
        }
        
        println("性能对比测试完成！")
    }
    
    @Test
    fun testTypeAccuracy() {
        println("\n=== 类型准确性测试 ===")
        
        val testData = createTypeTestCSV()
        val df = DataFrame.readCSV(testData, autoType = true)
        
        println("数据类型推断结果:")
        df.dtypes().forEach { (colName, dtype) ->
            println("  $colName: $dtype")
        }
        
        // 验证前几行数据
        println("\n前3行数据:")
        println(df.head(3))
        
        testData.delete()
        println("类型准确性测试完成！")
    }
    
    private fun createLargeCSV(): File {
        val file = File.createTempFile("test_large_", ".csv")
        val writer = FileWriter(file)
        
        // 写入表头
        writer.write("id,name,age,score\n")
        
        // 写入10000行数据
        for (i in 1..10000) {
            writer.write("$i,User$i,${20 + (i % 50)},${(i % 100) + (i % 10) * 0.1}\n")
        }
        
        writer.close()
        return file
    }
    
    private fun createTestCSV(rows: Int): File {
        val file = File.createTempFile("test_${rows}_", ".csv")
        val writer = FileWriter(file)
        
        writer.write("id,value,category,flag\n")
        
        for (i in 1..rows) {
            writer.write("$i,${i * 1.5},Cat${i % 5},${i % 2 == 0}\n")
        }
        
        writer.close()
        return file
    }
    
    private fun createTypeTestCSV(): File {
        val file = File.createTempFile("test_types_", ".csv")
        val writer = FileWriter(file)
        
        writer.write("int_col,float_col,bool_col,string_col\n")
        writer.write("123,45.67,true,text1\n")
        writer.write("456,78.90,false,text2\n")
        writer.write("789,12.34,true,text3\n")
        
        writer.close()
        return file
    }
    
    private fun countLines(file: File): Int {
        return file.readLines().size - 1 // 减去表头
    }
}
