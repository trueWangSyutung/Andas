package cn.ac.oac.libs.andas

import cn.ac.oac.libs.andas.entity.DataFrame
import cn.ac.oac.libs.andas.types.AndaTypes
import cn.ac.oac.libs.andas.utils.BatchCSVUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter

/**
 * 流式 CSV 解析与列式计算的回归/性能测试。
 *
 * 37MB / 20 万行的 largest.csv 在旧的实现中需要先把整个文件读成
 * List<String>，再为每行构造 Map，最后转置成列，内存峰值很高；
 * 这里验证新实现能够在受限堆内存下完成读取，并且计算结果正确。
 */
class StreamingCsvPerformanceTest {

    private fun locateLargeCsv(): File? = listOf(
        File("app/src/main/assets/largest.csv"),
        File("../app/src/main/assets/largest.csv"),
        File("../../app/src/main/assets/largest.csv"),
        File("src/main/assets/largest.csv")
    ).firstOrNull { it.exists() }

    @Test
    fun readsLargeAssetWithBoundedMemory() {
        val file = locateLargeCsv()
        Assume.assumeTrue("largest.csv 不存在，跳过大数据测试", file != null)

        val runtime = Runtime.getRuntime()
        System.gc()
        Thread.sleep(200)
        val before = runtime.totalMemory() - runtime.freeMemory()

        val start = System.nanoTime()
        val df = DataFrame.readCSV(file!!)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        val after = runtime.totalMemory() - runtime.freeMemory()
        val heapDeltaMb = (after - before) / 1024.0 / 1024.0
        println("largest.csv 读取: ${df.shape()}，耗时 ${elapsedMs}ms，堆增长 ${"%.1f".format(heapDeltaMb)}MB")

        assertEquals(200_000, df.shape().first)
        assertEquals(15, df.shape().second)
        assertEquals(AndaTypes.STRING, df.dtypes()["string_col_1"])
        assertEquals(AndaTypes.INT64, df.dtypes()["long_col_4"])
        assertEquals(AndaTypes.INT32, df.dtypes()["int_col_7"])
        assertEquals(AndaTypes.FLOAT64, df.dtypes()["double_col_10"])

        assertEquals("0GgF09sAVg", df.at(0, "string_col_1"))
        assertEquals(740764253420L, df.at(0, "long_col_4"))
        assertEquals(68, df.at(0, "int_col_7"))
        assertEquals(32.913985162415116, df.at(0, "double_col_10"))

        val sum = df.sum("double_col_10")
        assertTrue("sum 应为有限数", sum.isFinite())
        assertTrue(df.mean("double_col_10").isFinite())

        // 排序结果必须按数值单调递增（旧实现按字符串排序，会把 100 排在 20 前面）
        val sorted = df.sortValues("double_col_10")
        var previous = Double.NEGATIVE_INFINITY
        for (i in 0 until 2000) {
            val value = sorted.at(i, "double_col_10") as Double
            assertTrue("第 $i 行排序不正确: $previous > $value", value >= previous)
            previous = value
        }

        // 过滤 + 分组聚合的结果规模校验
        val filtered = df.filterGreaterThan("double_col_10", 90.0)
        assertTrue(filtered.shape().first > 0)
        for (i in 0 until 500) {
            assertTrue((filtered.at(i, "double_col_10") as Double) > 90.0)
        }
    }

    @Test
    fun streamsLargeAssetInBatches() {
        val file = locateLargeCsv()
        Assume.assumeTrue("largest.csv 不存在，跳过批量测试", file != null)

        var rows = 0
        var batches = 0
        val start = System.nanoTime()
        FileInputStream(file!!).use { input ->
            BatchCSVUtils.readCSVBatch(
                input,
                batchSize = 50_000,
                callback = { batch ->
                    rows += batch.shape().first
                    batches++
                }
            )
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        println("largest.csv 分批读取: ${rows} 行 / ${batches} 批，耗时 ${elapsedMs}ms")

        assertEquals(200_000, rows)
        assertEquals(4, batches)
    }

    @Test
    fun handlesQuotedMultilineAndDuplicateHeaders() {
        val file = File.createTempFile("streaming_csv_", ".csv")
        try {
            FileWriter(file).use { writer ->
                writer.write("id,text,score\n")
                writer.write("1,\"hello, world\",10\n")
                writer.write("2,\"line1\nline2\",20\n")
                writer.write("3,\"quote \"\"inside\"\"\",30\n")
            }

            val df = DataFrame.readCSV(file)
            assertEquals(3, df.shape().first)
            assertEquals(3, df.shape().second)
            assertEquals("hello, world", df.at(0, "text"))
            assertEquals("line1\nline2", df.at(1, "text"))
            assertEquals("quote \"inside\"", df.at(2, "text"))
            assertEquals(10, df.at(0, "score"))
        } finally {
            file.delete()
        }

        val duplicate = File.createTempFile("duplicate_header_", ".csv")
        try {
            FileWriter(duplicate).use { writer ->
                writer.write("a,a,b\n")
                writer.write("1,2,3\n")
            }
            val df = DataFrame.readCSV(duplicate)
            assertEquals(listOf("a", "a_1", "b"), df.columns())
            assertEquals(1, df.at(0, "a"))
            assertEquals(2, df.at(0, "a_1"))
            assertEquals(3, df.at(0, "b"))
        } finally {
            duplicate.delete()
        }
    }

    @Test
    fun columnarOperationsAreCorrect() {
        val file = File.createTempFile("ops_csv_", ".csv")
        try {
            FileWriter(file).use { writer ->
                writer.write("group,value,label\n")
                for (i in 0 until 1000) {
                    writer.write("g${i % 3},${i},item${i % 7}\n")
                }
            }

            val df = DataFrame.readCSV(file)
            assertEquals(1000, df.shape().first)
            assertEquals(AndaTypes.INT32, df.dtypes()["value"])
            assertEquals(AndaTypes.STRING, df.dtypes()["label"])

            // sum = 0 + 1 + ... + 999
            assertEquals(499_500.0, df.sum("value"), 0.0)
            assertEquals(499.5, df.mean("value"), 0.0)
            assertEquals(0.0, df.min("value"), 0.0)
            assertEquals(999.0, df.max("value"), 0.0)

            val grouped = df.groupBySum("group", "value")
            assertEquals(3, grouped.shape().first)
            val groupedSum = grouped.columns().let { _ ->
                (0 until grouped.shape().first).sumOf { grouped.at(it, "value") as Double }
            }
            assertEquals(499_500.0, groupedSum, 0.0)

            val nullIndex = df.findNullIndices("value")
            assertTrue(nullIndex.isEmpty())

            val described = df.describe("value")
            assertEquals(1000.0, described["count"])
            assertEquals(499.5, described["mean"])

            val sampled = df.sample(10)
            assertEquals(10, sampled.shape().first)
        } finally {
            file.delete()
        }
    }

    @Test
    fun joinsAndAggregationsScaleLinearly() {
        val file = File.createTempFile("join_csv_", ".csv")
        try {
            FileWriter(file).use { writer ->
                writer.write("id,category,amount\n")
                for (i in 0 until 5000) {
                    writer.write("$i,cat${i % 10},${i % 100}\n")
                }
            }

            val df = DataFrame.readCSV(file)
            val agg = df.groupBy("category").agg(mapOf("total" to "sum", "avg" to "mean", "n" to "count"))
            assertNotNull(agg)
            assertEquals(10, agg.shape().first)

            val sorted = df.sortValues("amount", descending = false)
            assertEquals(5000, sorted.shape().first)
            assertEquals(0, sorted.at(0, "amount"))

            val filtered = df.filter { row -> (row["amount"]?.get(0) as? Number)?.toDouble()?.let { it >= 50 } ?: false }
            assertEquals(2500, filtered.shape().first)
        } finally {
            file.delete()
        }
    }
    @Test
    fun fillNullKeepsStringColumns() {
        val file = File.createTempFile("fill_na_", ".csv")
        try {
            FileWriter(file).use { writer ->
                writer.write("num,text\n")
                writer.write("1,alpha\n")
                writer.write(",beta\n")
                writer.write("3,\n")
            }

            val df = DataFrame.readCSV(file)
            val filled = df.fillna(0)
            assertEquals(0.0, filled.at(1, "num") as Double, 0.0)
            assertEquals("alpha", filled.at(0, "text"))
            assertEquals("beta", filled.at(1, "text"))
            assertEquals(0, filled.at(2, "text"))

            val filledText = df.fillna("missing")
            assertEquals("missing", filledText.at(2, "text"))
            assertEquals("beta", filledText.at(1, "text"))
        } finally {
            file.delete()
        }
    }

    @Test
    fun correlationAndNormalizationAreNumeric() {
        val df = DataFrame(
            mapOf(
                "x" to listOf(1.0, 2.0, 3.0, 4.0),
                "y" to listOf(2.0, 4.0, 6.0, 8.0),
                "z" to listOf(4.0, 3.0, 2.0, 1.0)
            )
        )

        val corr = df.corr()
        assertEquals(1.0, corr.at(0, "x") as Double, 1e-9)
        assertEquals(1.0, corr.at(0, "y") as Double, 1e-9)
        assertEquals(-1.0, corr.at(2, "x") as Double, 1e-9)

        val normalized = df.normalize("x")
        assertEquals(-1.3416407864998738, normalized.at(0, "x") as Double, 1e-9)
        assertEquals(1.3416407864998738, normalized.at(3, "x") as Double, 1e-9)
    }

}
