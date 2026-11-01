package cn.ac.oac.libs.andas

import cn.ac.oac.libs.andas.entity.DataFrame
import cn.ac.oac.libs.andas.entity.Series
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.io.FileWriter

/**
 * CSV读取功能测试
 */
class CSVReadTest {

    @Test
    fun testReadCSVWithHeader() {
        // 创建测试CSV文件
        val csvFile = createTempFile()
        FileWriter(csvFile).use { writer ->
            writer.write("name,age,score,active\n")
            writer.write("Alice,25,95.5,true\n")
            writer.write("Bob,30,87.2,false\n")
            writer.write("Charlie,28,92.0,true\n")
        }

        // 读取CSV
        val df = DataFrame.readCSV(csvFile, header = true, autoType = true)

        // 验证结果
        assertEquals(3, df.shape().first) // 3行
        assertEquals(4, df.shape().second) // 4列
        assertEquals(listOf("name", "age", "score", "active"), df.columns())

        // 验证数据类型
        val row0 = df.iloc(0)
        assertEquals("Alice", row0.cell("name"))
        assertEquals(25, row0.cell("age"))
        assertEquals(95.5, row0.cell("score"))
        assertEquals(true, row0.cell("active"))

        csvFile.delete()
    }

    @Test
    fun testReadCSVWithoutHeader() {
        // 创建测试CSV文件
        val csvFile = createTempFile()
        FileWriter(csvFile).use { writer ->
            writer.write("Alice,25,95.5,true\n")
            writer.write("Bob,30,87.2,false\n")
            writer.write("Charlie,28,92.0,true\n")
        }

        // 读取CSV（无表头）
        val df = DataFrame.readCSV(csvFile, header = false, autoType = true)

        // 验证结果
        assertEquals(3, df.shape().first)
        assertEquals(4, df.shape().second)
        assertEquals(listOf("col0", "col1", "col2", "col3"), df.columns())

        // 验证数据类型
        val row0 = df.iloc(0)
        assertEquals("Alice", row0.cell("col0"))
        assertEquals(25, row0.cell("col1"))
        assertEquals(95.5, row0.cell("col2"))
        assertEquals(true, row0.cell("col3"))

        csvFile.delete()
    }

    @Test
    fun testAutoTypeDetection() {
        val csvFile = createTempFile()
        FileWriter(csvFile).use { writer ->
            writer.write("int_col,long_col,double_col,bool_col,text_col,date_col\n")
            writer.write("123,9223372036854775807,3.14,true,hello,2024-01-02\n")
            writer.write("-7,1234567890123,2.718,false,world,2024-01-03\n")
        }

        val df = DataFrame.readCSV(csvFile, header = true, autoType = true)

        assertEquals(123, df.at(0, "int_col"))
        assertEquals(9223372036854775807L, df.at(0, "long_col"))
        assertEquals(3.14, df.at(0, "double_col"))
        assertEquals(true, df.at(0, "bool_col"))
        assertEquals("hello", df.at(0, "text_col"))
        assertEquals("2024-01-02", df.at(0, "date_col"))

        csvFile.delete()
    }

    @Test
    fun testComplexCSVWithQuotes() {
        // 测试包含引号和分隔符的CSV
        val csvFile = createTempFile()
        FileWriter(csvFile).use { writer ->
            writer.write("name,description,price\n")
            writer.write("\"John Doe\",\"A good person\",100.50\n")
            writer.write("\"Jane Smith\",\"Another, person\",200.75\n")
            writer.write("\"Bob \"\"The Builder\"\"\",\"Expert, builder\",150.00\n")
        }

        val df = DataFrame.readCSV(csvFile, header = true, autoType = true)

        assertEquals(3, df.shape().first)
        assertEquals(3, df.shape().second)

        val row0 = df.iloc(0)
        assertEquals("John Doe", row0.cell("name"))
        assertEquals("A good person", row0.cell("description"))
        assertEquals(100.50, row0.cell("price"))

        val row1 = df.iloc(1)
        assertEquals("Jane Smith", row1.cell("name"))
        assertEquals("Another, person", row1.cell("description"))

        val row2 = df.iloc(2)
        assertEquals("Bob \"The Builder\"", row2.cell("name"))
        assertEquals("Expert, builder", row2.cell("description"))

        csvFile.delete()
    }

    @Test
    fun testNullValuesHandling() {
        // 测试空值处理
        val csvFile = createTempFile()
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

        csvFile.delete()
    }

    @Test
    fun testSkipLines() {
        // 测试跳过行
        val csvFile = createTempFile()
        FileWriter(csvFile).use { writer ->
            writer.write("# This is a comment\n")
            writer.write("# Another comment\n")
            writer.write("name,age,score\n")
            writer.write("Alice,25,95.5\n")
            writer.write("Bob,30,87.2\n")
        }

        val df = DataFrame.readCSV(csvFile, header = true, autoType = true, skipLines = 2)

        assertEquals(2, df.shape().first)
        assertEquals(3, df.shape().second)
        assertEquals(listOf("name", "age", "score"), df.columns())

        val row0 = df.iloc(0)
        assertEquals("Alice", row0.cell("name"))

        csvFile.delete()
    }

    @Test
    fun testCustomDelimiters() {
        // 测试自定义分隔符
        val csvFile = createTempFile()
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

        csvFile.delete()
    }

    @Test
    fun testTrimValues() {
        // 测试空格修剪
        val csvFile = createTempFile()
        FileWriter(csvFile).use { writer ->
            writer.write("name,age,score\n")
            writer.write("  Alice  ,  25  ,  95.5  \n")
            writer.write("Bob,30,87.2\n")
        }

        // 启用修剪
        val df1 = DataFrame.readCSV(csvFile, header = true, autoType = true, trimValues = true)
        val row0 = df1.iloc(0)
        assertEquals("Alice", row0.cell("name"))
        assertEquals(25, row0.cell("age"))
        assertEquals(95.5, row0.cell("score"))

        // 禁用修剪
        val df2 = DataFrame.readCSV(csvFile, header = true, autoType = true, trimValues = false)
        val row0_2 = df2.iloc(0)
        assertEquals("  Alice  ", row0_2.cell("name"))
        assertEquals("  25  ", row0_2.cell("age"))
        assertEquals("  95.5  ", row0_2.cell("score"))

        csvFile.delete()
    }

    @Test
    fun testColumnMismatchHandling() {
        // 测试列数不匹配的情况
        val csvFile = createTempFile()
        FileWriter(csvFile).use { writer ->
            writer.write("name,age,score\n")
            writer.write("Alice,25,95.5,extra\n") // 多一列
            writer.write("Bob,30\n") // 少一列
            writer.write("Charlie,28,92.0\n")
        }

        val df = DataFrame.readCSV(csvFile, header = true, autoType = true)

        assertEquals(3, df.shape().first)
        assertEquals(3, df.shape().second)

        val row0 = df.iloc(0)
        assertEquals("Alice", row0.cell("name"))
        assertEquals(25, row0.cell("age"))
        assertEquals(95.5, row0.cell("score"))

        val row1 = df.iloc(1)
        assertEquals("Bob", row1.cell("name"))
        assertEquals(30, row1.cell("age"))
        assertNull(row1.cell("score")) // 缺失的列应该是null

        csvFile.delete()
    }

    @Test
    fun testLargeNumbers() {
        // 测试大数字处理
        val csvFile = createTempFile()
        FileWriter(csvFile).use { writer ->
            writer.write("big_int,big_float\n")
            writer.write("9223372036854775807,1.7976931348623157E308\n")
            writer.write("-9223372036854775808,-1.7976931348623157E308\n")
        }

        val df = DataFrame.readCSV(csvFile, header = true, autoType = true)

        val row0 = df.iloc(0)
        // 大整数应该转为Long
        assertTrue(row0.cell("big_int") is Long)
        // 大浮点数应该转为Double
        assertTrue(row0.cell("big_float") is Double)

        csvFile.delete()
    }

    @Test
    fun testErrorHandling() {
        // 测试错误处理
        val csvFile = createTempFile()
        FileWriter(csvFile).use { writer ->
            writer.write("name,age\n")
            writer.write("Alice,25\n")
            writer.write("Bob,not_a_number\n") // 无效数字
        }

        try {
            // 应该能够处理，非数字保持为字符串
            val df = DataFrame.readCSV(csvFile, header = true, autoType = true)
            val row1 = df.iloc(1)
            assertEquals("Bob", row1.cell("name"))
            // 无法转换为数字，保持为字符串
            assertEquals("not_a_number", row1.cell("age"))
        } catch (e: Exception) {
            fail("Should not throw exception for invalid numbers")
        }

        csvFile.delete()
    }

    @Test
    fun testEmptyFile() {
        // 测试空文件
        val csvFile = createTempFile()
        // 创建空文件

        val df = DataFrame.readCSV(csvFile, header = true, autoType = true)
        assertEquals(0, df.shape().first)
        assertEquals(0, df.shape().second)

        csvFile.delete()
    }

    @Test
    fun testOnlyHeader() {
        // 测试只有表头的文件
        val csvFile = createTempFile()
        FileWriter(csvFile).use { writer ->
            writer.write("name,age,score\n")
        }

        val df = DataFrame.readCSV(csvFile, header = true, autoType = true)
        assertEquals(0, df.shape().first)
        assertEquals(3, df.shape().second)
        assertEquals(listOf("name", "age", "score"), df.columns())

        csvFile.delete()
    }

    // 辅助方法：创建临时文件
    private fun createTempFile(): File {
        val file = File.createTempFile("test_csv_", ".csv")
        file.deleteOnExit()
        return file
    }

    private fun Map<String, Series<Any>?>?.cell(name: String): Any? = this?.get(name)?.get(0)

}
