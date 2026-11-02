package cn.ac.oac.libs.andas.numpy

/** NDArray 支持的存储类型。 */
enum class DType(val bytes: Int, val floating: Boolean) {
    FLOAT64(8, true),
    FLOAT32(4, true),
    INT32(4, false),
    INT64(8, false),
    BOOLEAN(1, false);

    companion object {
        fun fromName(name: String): DType = entries.firstOrNull { it.name.equals(name, true) }
            ?: throw IllegalArgumentException("不支持的 dtype: $name")
    }
}
