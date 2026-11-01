package cn.ac.oac.libs.andas

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.ac.oac.libs.andas.entity.DataFrame
import cn.ac.oac.libs.andas.entity.DataFrameIO
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.max

/**
 * 跨语言一致性测试：同一份 5k × 15 CSV，在 Android 上用 Andas 计算，
 * 与 pandas 2.1.0 生成的期望值逐项比较。
 *
 * 期望值来自 bench/consistency_pandas.py，作为 androidTest asset 随测试 APK 安装。
 */
@RunWith(AndroidJUnit4::class)
class ConsistencyInstrumentedTest {

    @Test
    fun metricsMatchPandas() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val expected = JSONObject(
            context.assets.open("consistency_pandas.json").bufferedReader().use { it.readText() }
        ).getJSONObject("metrics")

        val df = context.assets.open("consistency_5000_c15.csv").use { DataFrameIO.readCSV(it) }
        val left = context.assets.open("join_left_5000.csv").use { DataFrameIO.readCSV(it) }
        val right = context.assets.open("join_right_5000.csv").use { DataFrameIO.readCSV(it) }

        val actual = metrics(df, left, right)
        val failures = JSONArray()
        var maxError = 0.0

        val exactInt = setOf(
            "rows", "cols", "filter_count", "groupby_count", "merge_rows"
        )
        for (key in expected.keys()) {
            if (key == "groupby_sum") continue
            val expectedValue = expected.get(key)
            val actualValue = actual.get(key)
            if (key in exactInt) {
                if (expectedValue.toString() != actualValue.toString()) {
                    failures.put("$key: pandas=$expectedValue android=$actualValue")
                }
            } else {
                val e = (expectedValue as Number).toDouble()
                val a = (actualValue as Number).toDouble()
                val rel = relativeError(a, e)
                maxError = max(maxError, rel)
                if (!within(a, e)) {
                    failures.put("$key: pandas=$e android=$a rel=$rel")
                }
            }
        }

        val expectedGroups = expected.getJSONObject("groupby_sum")
        val actualGroups = actual.getJSONObject("groupby_sum")
        for (key in expectedGroups.keys()) {
            if (!actualGroups.has(key)) {
                failures.put("groupby_sum missing group $key")
                continue
            }
            val e = expectedGroups.getDouble(key)
            val a = actualGroups.getDouble(key)
            val rel = relativeError(a, e)
            maxError = max(maxError, rel)
            if (!within(a, e)) {
                failures.put("groupby_sum[$key]: pandas=$e android=$a rel=$rel")
            }
        }

        val report = JSONObject()
            .put("generator", "andas-android")
            .put("device", android.os.Build.MODEL)
            .put("sdk", android.os.Build.VERSION.SDK_INT)
            .put("max_rel_error", maxError)
            .put("failures", failures)
            .put("metrics", actual)
        Log.i("ANDAS_CONSISTENCY", report.toString())

        assertEquals("Android 结果与 pandas 不一致: $failures", 0, failures.length())
    }

    private fun metrics(df: DataFrame, left: DataFrame, right: DataFrame): JSONObject {
        val rows = df.shape().first
        val sorted = df.sortValues("double_col_10", false)
        val filtered = df.filterGreaterThan("double_col_10", 90.0)
        val grouped = df.groupBySum("string_col_3", "double_col_10")
        val merged = left.merge(right, "id", "inner")
        val normalized = df.normalize("double_col_10")
        val corr = df.corr()
        val describe = df.describe("double_col_10")

        var sortFirst50 = 0.0
        var sortLast50 = 0.0
        for (i in 0 until 50) {
            sortFirst50 += (sorted.at(i, "double_col_10") as Number).toDouble()
            sortLast50 += (sorted.at(rows - 1 - i, "double_col_10") as Number).toDouble()
        }

        val groupSums = JSONObject()
        for (i in 0 until grouped.shape().first) {
            groupSums.put(
                grouped.at(i, "string_col_3").toString(),
                (grouped.at(i, "double_col_10") as Number).toDouble()
            )
        }

        val corrRow = corr.columns().indexOf("double_col_10")
        return JSONObject()
            .put("rows", rows)
            .put("cols", df.shape().second)
            .put("sum_double_col_10", df.sum("double_col_10"))
            .put("mean_double_col_10", df.mean("double_col_10"))
            .put("std_double_col_10", df.std("double_col_10"))
            .put("min_double_col_10", df.min("double_col_10"))
            .put("max_double_col_10", df.max("double_col_10"))
            .put("mean_long_col_4", df.mean("long_col_4"))
            .put("describe_count", describe.getValue("count"))
            .put("describe_mean", describe.getValue("mean"))
            .put("describe_std", describe.getValue("std"))
            .put("describe_min", describe.getValue("min"))
            .put("describe_max", describe.getValue("max"))
            .put("sort_first", (sorted.at(0, "double_col_10") as Number).toDouble())
            .put("sort_last", (sorted.at(rows - 1, "double_col_10") as Number).toDouble())
            .put("sort_first50_sum", sortFirst50)
            .put("sort_last50_sum", sortLast50)
            .put("filter_count", filtered.shape().first)
            .put("filter_sum", filtered.sum("double_col_10"))
            .put("groupby_count", grouped.shape().first)
            .put("groupby_sum", groupSums)
            .put("merge_rows", merged.shape().first)
            .put("merge_amount_sum", merged.sum("amount"))
            .put("corr_double_10_11", (corr.at(corrRow, "double_col_11") as Number).toDouble())
            .put("normalize_first", (normalized.at(0, "double_col_10") as Number).toDouble())
            .put("normalize_last", (normalized.at(rows - 1, "double_col_10") as Number).toDouble())
    }

    private fun relativeError(actual: Double, expected: Double): Double =
        abs(actual - expected) / max(1.0, abs(expected))

    private fun within(actual: Double, expected: Double): Boolean =
        relativeError(actual, expected) <= 1e-10
}
