package cn.ac.oac.libs.andas.core

/**
 * 原生数据处理库 - JNI包装
 * 提供高性能的数据处理操作
 */
object NativeData {
    
    init {
        System.loadLibrary("andas_native")
    }
    
    // 空值处理
    external fun findNullIndices(array: DoubleArray): IntArray
    external fun dropNullValues(array: DoubleArray): DoubleArray
    external fun fillNullWithConstant(array: DoubleArray, value: Double): DoubleArray
    
    // 分组聚合
    external fun groupBySum(values: DoubleArray, groups: IntArray): Map<String, Double>
    
    // 排序和索引
    external fun sortIndices(array: DoubleArray, descending: Boolean): IntArray
    
    // 数据合并
    external fun mergeIndices(left: DoubleArray, right: DoubleArray): IntArray
    
    // 布尔索引
    external fun where(mask: BooleanArray): IntArray
    
    // 统计描述
    external fun describe(array: DoubleArray): DoubleArray
    
    // 数据采样
    external fun sample(array: DoubleArray, sampleSize: Int): DoubleArray
    
    /**
     * 检查是否可用
     */
    fun isAvailable(): Boolean {
        return try {
            val testArray = doubleArrayOf(1.0, 2.0, Double.NaN, 4.0)
            val nullIndices = findNullIndices(testArray)
            nullIndices.size == 1 && nullIndices[0] == 2
        } catch (e: Exception) {
            false
        }
    }
}
