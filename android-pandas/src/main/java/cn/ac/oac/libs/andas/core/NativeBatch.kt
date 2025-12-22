package cn.ac.oac.libs.andas.core

/**
 * 原生批量处理库 - JNI包装
 * 提供高性能的批量数据处理操作
 */
object NativeBatch {
    
    init {
        System.loadLibrary("andas_native")
    }
    
    // 批量处理操作
    external fun processBatch(array: DoubleArray, batchSize: Int): DoubleArray
    
    /**
     * 检查是否可用
     */
    fun isAvailable(): Boolean {
        return try {
            val testArray = doubleArrayOf(1.0, 2.0, 3.0)
            val result = processBatch(testArray, 1000)
            result.size == 3
        } catch (e: Exception) {
            false
        }
    }
}
