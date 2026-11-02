package cn.ac.oac.libs.andas.numpy

import cn.ac.oac.libs.andas.entity.DataFrame
import cn.ac.oac.libs.andas.entity.Series

/**
 * NDArray（NumPy 风格）与 Andas（pandas 风格）之间的零依赖转换。
 *
 * 空值约定：NDArray 使用 `Double.NaN` 表示缺失；转回 DataFrame/Series 时
 * 可选择把 NaN 还原为 null。
 */
object PandasBridge {

    /** Series -> 一维 NDArray；非数值元素转换为 NaN。 */
    fun seriesToNDArray(series: Series<*>): NDArray =
        NDArray.fromArray(DoubleArray(series.size()) { i ->
            (series[i] as? Number)?.toDouble() ?: Double.NaN
        }, series.size())

    /** DataFrame 的数值列 -> 二维 NDArray（行主序，缺失值填 NaN）。 */
    fun dataframeToNDArray(
        dataFrame: DataFrame,
        columns: List<String>? = null,
        fillValue: Double = Double.NaN
    ): NDArray {
        val selected = columns ?: dataFrame.columns().filter {
            dataFrame[it].values().firstOrNull { value -> value != null } is Number
        }
        require(selected.isNotEmpty()) { "没有可转换的数值列" }
        val rows = dataFrame.shape().first
        val result = DoubleArray(rows * selected.size)
        for (c in selected.indices) {
            val series = dataFrame[selected[c]]
            for (r in 0 until rows) {
                val value = series[r]
                result[r * selected.size + c] = (value as? Number)?.toDouble() ?: fillValue
            }
        }
        return NDArray.fromArray(result, rows, selected.size)
    }

    /** 一维/二维 NDArray -> Series/DataFrame；NaN 可选择还原为 null。 */
    fun ndArrayToSeries(
        array: NDArray,
        name: String? = null,
        index: List<Any>? = null,
        nanAsNull: Boolean = true
    ): Series<Double> {
        require(array.ndim == 1) { "仅一维数组可转换为 Series" }
        val values = ArrayList<Any?>(array.size)
        for (i in 0 until array.size) {
            val value = array.get(i)
            values.add(if (nanAsNull && value.isNaN()) null else value)
        }
        @Suppress("UNCHECKED_CAST")
        return Series(values, index ?: (0 until array.size).toList(), name) as Series<Double>
    }

    fun ndArrayToDataFrame(
        array: NDArray,
        columns: List<String>? = null,
        nanAsNull: Boolean = true
    ): DataFrame {
        require(array.ndim == 2) { "仅二维数组可转换为 DataFrame" }
        val names = columns ?: (0 until array.shape[1]).map { "col$it" }
        require(names.size == array.shape[1]) { "列数不匹配" }
        val data = LinkedHashMap<String, List<Any?>>()
        for (c in names.indices) {
            val values = ArrayList<Any?>(array.shape[0])
            for (r in 0 until array.shape[0]) {
                val value = array.get(r, c)
                values.add(if (nanAsNull && value.isNaN()) null else value)
            }
            data[names[c]] = values
        }
        return DataFrame(data)
    }
}

fun Series<*>.toNDArray(): NDArray = PandasBridge.seriesToNDArray(this)
fun DataFrame.toNDArray(
    columns: List<String>? = null,
    fillValue: Double = Double.NaN
): NDArray = PandasBridge.dataframeToNDArray(this, columns, fillValue)

fun NDArray.toSeries(
    name: String? = null,
    index: List<Any>? = null,
    nanAsNull: Boolean = true
): Series<Double> = PandasBridge.ndArrayToSeries(this, name, index, nanAsNull)

fun NDArray.toDataFrame(
    columns: List<String>? = null,
    nanAsNull: Boolean = true
): DataFrame = PandasBridge.ndArrayToDataFrame(this, columns, nanAsNull)
