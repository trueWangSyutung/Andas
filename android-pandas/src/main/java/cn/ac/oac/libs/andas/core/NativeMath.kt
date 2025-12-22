package cn.ac.oac.libs.andas.core

/**
 * 原生数学运算库 - JNI包装
 * 提供高性能的数学运算实现
 */
object NativeMath {
    
    init {
        System.loadLibrary("andas_native")
    }
    
    // 基础数学运算
    external fun multiplyDoubleArray(array: DoubleArray, multiplier: Double): DoubleArray
    external fun sumDoubleArray(array: DoubleArray): Double
    external fun meanDoubleArray(array: DoubleArray): Double
    external fun maxDoubleArray(array: DoubleArray): Double
    external fun minDoubleArray(array: DoubleArray): Double
    
    // 向量化运算
    external fun vectorizedAdd(a: DoubleArray, b: DoubleArray): DoubleArray
    external fun vectorizedMultiply(a: DoubleArray, b: DoubleArray): DoubleArray
    external fun dotProduct(a: DoubleArray, b: DoubleArray): Double
    external fun norm(array: DoubleArray): Double
    external fun normalize(array: DoubleArray): DoubleArray
    
    // 统计函数
    external fun variance(array: DoubleArray): Double
    external fun std(array: DoubleArray): Double
    
    // 排序和索引
    external fun argsort(array: DoubleArray): IntArray
    
    // 布尔运算
    external fun greaterThan(array: DoubleArray, threshold: Double): BooleanArray
    
    /**
     * 检查是否可用
     */
    fun isAvailable(): Boolean {
        return try {
            // 测试一个简单的调用
            val testArray = doubleArrayOf(1.0, 2.0, 3.0)
            sumDoubleArray(testArray) == 6.0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 性能测试方法
     */
    object Benchmark {
        // 测量操作时间（微秒）
        external fun measureOperationTime(operationType: Int, dataSize: Int): Long
        
        // 操作类型定义
        const val OP_ARRAY_CREATE = 1
        const val OP_MATH_OPERATION = 2
        const val OP_STATISTICS = 3
        const val OP_FILTER = 4
    }
}
