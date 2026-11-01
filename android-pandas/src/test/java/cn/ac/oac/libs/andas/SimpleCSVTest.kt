package cn.ac.oac.libs.andas

import cn.ac.oac.libs.andas.entity.DataFrame
import cn.ac.oac.libs.andas.entity.Series
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.io.FileWriter

/**
 * 简单的CSV读取功能验证测试
 */
class SimpleCSVTest {

    @Test
    fun testBasicCSVReading() {
        // 创建测试CSV文件
        val csvFile = File.createTempFile("simple_test_", ".csv")
        try {
            FileWriter(csvFile).use { writer ->
                writer.write("name,age,score\n")
                writer.write("Alice,25,95.5\n")
                writer.write("Bob,30,87.2\n")
            }

            // 读取CSV
            val df = DataFrame.readCSV(csvFile, header = true, autoType = true)

            // 基本验证
            assertEquals(2, df.shape().first)  // 2行数据
            assertEquals(3, df.shape().second) // 3列
            assertEquals(listOf("name", "age", "score"), df.columns())

            // 验证数据类型和值
            val row0 = df.iloc(0)
            assertEquals("Alice", row0.cell("name"))
            assertEquals(25, row0.cell("age"))
            assertEquals(95.5, row0.cell("score"))

            val row1 = df.iloc(1)
            assertEquals("Bob", row1.cell("name"))
            assertEquals(30, row1.cell("age"))
            assertEquals(87.2, row1.cell("score"))

            println("✓ Basic CSV reading test passed!")
        } finally {
            csvFile.delete()
        }
    }

    @Test
    fun testTypeInference() {
        // 测试类型推断
        val testCases = listOf(
            Pair("123", 123),
            Pair("3.14", 3.14),
            Pair("true", true),
            Pair("false", false),
            Pair("hello", "hello"),
            Pair("", null),
            Pair("null", null),
            Pair("NA", null)
        )

        testCases.filter { it.first.isNotEmpty() && !it.first.equals("null", true) && it.first != "NA" }
            .forEach { (input, expected) ->
                val type = DataFrame.inferTypeFromSample(input, listOf("", "null", "NA"))
                val result = DataFrame.convertValueWithType(input, type)
                assertEquals("Failed for input: $input", expected, result)
            }

        println("✓ Type inference test passed!")
    }

    @Test
    fun testNoHeader() {
        // 测试无表头CSV
        val csvFile = File.createTempFile("no_header_", ".csv")
        try {
            FileWriter(csvFile).use { writer ->
                writer.write("Alice,25,95.5\n")
                writer.write("Bob,30,87.2\n")
            }

            val df = DataFrame.readCSV(csvFile, header = false, autoType = true)

            assertEquals(2, df.shape().first)
            assertEquals(3, df.shape().second)
            assertEquals(listOf("col0", "col1", "col2"), df.columns())

            val row0 = df.iloc(0)
            assertEquals("Alice", row0.cell("col0"))
            assertEquals(25, row0.cell("col1"))
            assertEquals(95.5, row0.cell("col2"))

            println("✓ No header test passed!")
        } finally {
            csvFile.delete()
        }
    }

    @Test
    fun testNullHandling() {
        // 测试空值处理
        val csvFile = File.createTempFile("null_test_", ".csv")
        try {
            FileWriter(csvFile).use { writer ->
                writer.write("name,age,score\n")
                writer.write("Alice,,95.5\n")
                writer.write("Bob,30,null\n")
                writer.write("Charlie,28,NA\n")
            }

            val df = DataFrame.readCSV(csvFile, header = true, autoType = true)

            val row0 = df.iloc(0)
            assertNull(row0.cell("age"))
            assertEquals(95.5, row0.cell("score"))

            val row1 = df.iloc(1)
            assertEquals(30, row1.cell("age"))
            assertNull(row1.cell("score"))

            val row2 = df.iloc(2)
            assertEquals(28, row2.cell("age"))
            assertNull(row2.cell("score"))

            println("✓ Null handling test passed!")
        } finally {
            csvFile.delete()
        }
    }

    @Test
    fun testCustomDelimiter() {
        // 测试自定义分隔符
        val csvFile = File.createTempFile("delimiter_", ".csv")
        try {
            FileWriter(csvFile).use { writer ->
                writer.write("name|age|score\n")
                writer.write("Alice|25|95.5\n")
                writer.write("Bob|30|87.2\n")
            }

            val df = DataFrame.readCSV(csvFile, delimiter = "|", header = true, autoType = true)

            assertEquals(2, df.shape().first)
            assertEquals(3, df.shape().second)
            assertEquals(listOf("name", "age", "score"), df.columns())

            val row0 = df.iloc(0)
            assertEquals("Alice", row0.cell("name"))
            assertEquals(25, row0.cell("age"))
            assertEquals(95.5, row0.cell("score"))

            println("✓ Custom delimiter test passed!")
        } finally {
            csvFile.delete()
        }
    }

    @Test
    fun testQuoteHandling() {
        // 测试引号处理
        val csvFile = File.createTempFile("quote_", ".csv")
        try {
            FileWriter(csvFile).use { writer ->
                writer.write("name,description,price\n")
                writer.write("\"John Doe\",\"A good person\",100.50\n")
                writer.write("\"Jane Smith\",\"Another, person\",200.75\n")
            }

            val df = DataFrame.readCSV(csvFile, header = true, autoType = true)

            assertEquals(2, df.shape().first)
            assertEquals(3, df.shape().second)

            val row0 = df.iloc(0)
            assertEquals("John Doe", row0.cell("name"))
            assertEquals("A good person", row0.cell("description"))
            assertEquals(100.50, row0.cell("price"))

            val row1 = df.iloc(1)
            assertEquals("Jane Smith", row1.cell("name"))
            assertEquals("Another, person", row1.cell("description"))

            println("✓ Quote handling test passed!")
        } finally {
            csvFile.delete()
        }
    }

    @Test
    fun testEmptyFile() {
        // 测试空文件
        val csvFile = File.createTempFile("empty_", ".csv")
        try {
            // 创建空文件
            val df = DataFrame.readCSV(csvFile, header = true, autoType = true)
            assertEquals(0, df.shape().first)
            assertEquals(0, df.shape().second)
            println("✓ Empty file test passed!")
        } finally {
            csvFile.delete()
        }
    }

    @Test
    fun testOnlyHeader() {
        // 测试只有表头的文件
        val csvFile = File.createTempFile("header_only_", ".csv")
        try {
            FileWriter(csvFile).use { writer ->
                writer.write("name,age,score\n")
            }

            val df = DataFrame.readCSV(csvFile, header = true, autoType = true)
            assertEquals(0, df.shape().first)
            assertEquals(3, df.shape().second)
            assertEquals(listOf("name", "age", "score"), df.columns())
            println("✓ Only header test passed!")
        } finally {
            csvFile.delete()
        }
    }

    private fun Map<String, Series<Any>?>?.cell(name: String): Any? = this?.get(name)?.get(0)

}
