package cn.ac.oac.libs.andas

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.ac.oac.libs.andas.entity.DataFrame
import cn.ac.oac.libs.andas.entity.DataFrameIO
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Random

/**
 * 真机/模拟器上的 CSV 读取实验（供技术报告采集 Android 侧数据）。
 *
 * 通过 logcat 标签 `ANDAS_BENCH` 输出每档数据量的耗时、读取后存活堆内存和结果状态。
 */
@RunWith(AndroidJUnit4::class)
class CsvBenchInstrumentedTest {

    private val sizes = listOf(10_000, 25_000, 50_000, 100_000, 200_000)
    private val header =
        "string_col_1,string_col_2,string_col_3,long_col_4,long_col_5,long_col_6," +
            "int_col_7,int_col_8,int_col_9,double_col_10,double_col_11,double_col_12," +
            "float_col_13,float_col_14,float_col_15"

    @Test
    fun csvReadScaling() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Log.i(
            "ANDAS_BENCH",
            "device=${android.os.Build.MODEL} sdk=${android.os.Build.VERSION.SDK_INT} " +
                "maxHeapMb=${Runtime.getRuntime().maxMemory() / 1024 / 1024}"
        )

        for (rows in sizes) {
            val file = File(context.cacheDir, "bench_${rows}_c15.csv")
            generateIfNeeded(file, rows)

            val runtime = Runtime.getRuntime()
            runtime.gc()
            Thread.sleep(200)
            val before = runtime.totalMemory() - runtime.freeMemory()

            var df: DataFrame? = null
            var error: String? = null
            val started = System.nanoTime()
            try {
                df = DataFrameIO.readCSV(file)
            } catch (oom: OutOfMemoryError) {
                error = "OOM"
            } catch (t: Throwable) {
                error = t.javaClass.simpleName + ": " + t.message
            }
            val elapsedMs = (System.nanoTime() - started) / 1_000_000

            runtime.gc()
            Thread.sleep(120)
            val after = runtime.totalMemory() - runtime.freeMemory()
            val retainedMb = (after - before) / 1024 / 1024

            Log.i(
                "ANDAS_BENCH",
                "read rows=$rows ms=$elapsedMs retainedMb=$retainedMb " +
                    "status=${if (df != null) "ok" else error} shape=${df?.shape()}"
            )

            df = null
            runtime.gc()
        }
    }

    private fun generateIfNeeded(file: File, rows: Int) {
        if (file.exists() && file.length() > 1024) return
        val random = Random(20261101L)
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        fun randomString(): String {
            val chars = CharArray(10) { alphabet[random.nextInt(alphabet.length)] }
            return String(chars)
        }

        file.bufferedWriter(Charsets.UTF_8, 1 shl 16).use { writer ->
            writer.write(header)
            writer.newLine()
            val row = StringBuilder(220)
            for (i in 0 until rows) {
                row.setLength(0)
                row.append(randomString()).append(',')
                    .append(randomString()).append(',')
                    .append("cat_").append(i % 10).append(',')
                    .append(100_000_000_000L + random.nextInt(900_000_000).toLong()).append(',')
                    .append(100_000_000_000L + random.nextInt(900_000_000).toLong()).append(',')
                    .append(100_000_000_000L + random.nextInt(900_000_000).toLong()).append(',')
                    .append(random.nextInt(201) - 100).append(',')
                    .append(random.nextInt(201) - 100).append(',')
                    .append(random.nextInt(201) - 100).append(',')
                    .append(random.nextDouble() * 200 - 100).append(',')
                    .append(random.nextDouble() * 200 - 100).append(',')
                    .append(random.nextDouble() * 200 - 100).append(',')
                    .append(random.nextDouble() * 200 - 100).append(',')
                    .append(random.nextDouble() * 200 - 100).append(',')
                    .append(random.nextDouble() * 200 - 100)
                writer.write(row.toString())
                writer.newLine()
            }
        }
    }
}
