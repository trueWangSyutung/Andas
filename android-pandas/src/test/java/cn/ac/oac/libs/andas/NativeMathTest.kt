package cn.ac.oac.libs.andas

import cn.ac.oac.libs.andas.core.NativeMath
import org.junit.Test
import org.junit.Assert.*

/**
 * NativeMath JNI函数测试
 * 基于large_dataset.csv数据格式进行测试
 */
class NativeMathTest {

    @Test
    fun testMultiplyDoubleArray() {
        println("=== 测试 multiplyDoubleArray ===")
        val array = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val multiplier = 2.0
        
        val result = NativeMath.multiplyDoubleArray(array, multiplier)
        
        println("输入: ${array.joinToString()}")
        println("乘数: $multiplier")
        println("结果: ${result.joinToString()}")
        
        assertArrayEquals(doubleArrayOf(2.0, 4.0, 6.0, 8.0, 10.0), result, 0.001)
        println("✅ 测试通过\n")
    }

    @Test
    fun testSumDoubleArray() {
        println("=== 测试 sumDoubleArray ===")
        val array = doubleArrayOf(582567.0354414985, -86165.50884370191, 619267.936907152)
        
        val result = NativeMath.sumDoubleArray(array)
        
        println("输入: ${array.joinToString()}")
        println("求和结果: $result")
        
        val expected = 582567.0354414985 + (-86165.50884370191) + 619267.936907152
        assertEquals(expected, result, 0.001)
        println("✅ 测试通过\n")
    }

    @Test
    fun testMeanDoubleArray() {
        println("=== 测试 meanDoubleArray ===")
        val array = doubleArrayOf(582567.0354414985, -86165.50884370191, 619267.936907152, -261499.41100525402, -368748.1343063426)
        
        val result = NativeMath.meanDoubleArray(array)
        
        println("输入: ${array.joinToString()}")
        println("均值结果: $result")
        
        val expected = array.average()
        assertEquals(expected, result, 0.001)
        println("✅ 测试通过\n")
    }

    @Test
    fun testMaxDoubleArray() {
        println("=== 测试 maxDoubleArray ===")
        val array = doubleArrayOf(582567.0354414985, -86165.50884370191, 619267.936907152, -261499.41100525402)
        
        val result = NativeMath.maxDoubleArray(array)
        
        println("输入: ${array.joinToString()}")
        println("最大值结果: $result")
        
        val expected = array.maxOrNull() ?: Double.NaN
        assertEquals(expected, result, 0.001)
        println("✅ 测试通过\n")
    }

    @Test
    fun testMinDoubleArray() {
        println("=== 测试 minDoubleArray ===")
        val array = doubleArrayOf(582567.0354414985, -86165.50884370191, 619267.936907152, -261499.41100525402)
        
        val result = NativeMath.minDoubleArray(array)
        
        println("输入: ${array.joinToString()}")
        println("最小值结果: $result")
        
        val expected = array.minOrNull() ?: Double.NaN
        assertEquals(expected, result, 0.001)
        println("✅ 测试通过\n")
    }

    @Test
    fun testRealDatasetOperations() {
        println("=== 基于真实数据集的综合测试 ===")
        
        // 模拟CSV中的double列数据
        val doubleCol10 = doubleArrayOf(582567.0354414985, -86165.50884370191, 619267.936907152, -261499.41100525402, -368748.1343063426)
        val doubleCol11 = doubleArrayOf(-86165.50884370191, -368748.1343063426, -759575.3713505617, -368748.1343063426, -759575.3713505617)
        
        println("列10数据: ${doubleCol10.joinToString()}")
        println("列11数据: ${doubleCol11.joinToString()}")
        
        // 测试求和
        val sum10 = NativeMath.sumDoubleArray(doubleCol10)
        val sum11 = NativeMath.sumDoubleArray(doubleCol11)
        println("列10求和: $sum10")
        println("列11求和: $sum11")
        
        // 测试均值
        val mean10 = NativeMath.meanDoubleArray(doubleCol10)
        val mean11 = NativeMath.meanDoubleArray(doubleCol11)
        println("列10均值: $mean10")
        println("列11均值: $mean11")
        
        // 测试最大值/最小值
        val max10 = NativeMath.maxDoubleArray(doubleCol10)
        val min10 = NativeMath.minDoubleArray(doubleCol10)
        println("列10最大值: $max10, 最小值: $min10")
        
        // 测试数组乘法
        val multiplied = NativeMath.multiplyDoubleArray(doubleCol10, 2.0)
        println("列10 × 2.0: ${multiplied.joinToString()}")
        
        // 验证计算正确性
        assertEquals(doubleCol10.sum(), sum10, 0.001)
        assertEquals(doubleCol10.average(), mean10, 0.001)
        assertEquals(doubleCol10.maxOrNull()!!, max10, 0.001)
        assertEquals(doubleCol10.minOrNull()!!, min10, 0.001)
        
        println("✅ 所有真实数据集测试通过\n")
    }

    @Test
    fun testEdgeCases() {
        println("=== 边界情况测试 ===")
        
        // 空数组
        try {
            val empty = doubleArrayOf()
            val result = NativeMath.sumDoubleArray(empty)
            println("空数组求和: $result")
            assertEquals(0.0, result, 0.001)
        } catch (e: Exception) {
            println("空数组处理异常: ${e.message}")
        }
        
        // 单元素数组
        val single = doubleArrayOf(42.0)
        println("单元素数组: ${single.joinToString()}")
        println("求和: ${NativeMath.sumDoubleArray(single)}")
        println("均值: ${NativeMath.meanDoubleArray(single)}")
        println("最大值: ${NativeMath.maxDoubleArray(single)}")
        println("最小值: ${NativeMath.minDoubleArray(single)}")
        
        // 包含NaN的数组
        val withNaN = doubleArrayOf(1.0, Double.NaN, 3.0, 4.0)
        println("包含NaN的数组: ${withNaN.joinToString()}")
        println("均值(应忽略NaN): ${NativeMath.meanDoubleArray(withNaN)}")
        println("最大值(应忽略NaN): ${NativeMath.maxDoubleArray(withNaN)}")
        
        println("✅ 边界情况测试完成\n")
    }

    @Test
    fun testPerformance() {
        println("=== 性能测试 ===")
        
        // 创建大数据集
        val size = 1000000
        val largeArray = DoubleArray(size) { it * 1.5 }
        
        println("测试数组大小: $size")
        
        // 测试求和性能
        val startSum = System.nanoTime()
        val sum = NativeMath.sumDoubleArray(largeArray)
        val endSum = System.nanoTime()
        println("求和耗时: ${(endSum - startSum) / 1_000_000.0} ms")
        
        // 测试均值性能
        val startMean = System.nanoTime()
        val mean = NativeMath.meanDoubleArray(largeArray)
        val endMean = System.nanoTime()
        println("均值耗时: ${(endMean - startMean) / 1_000_000.0} ms")
        
        // 测试乘法性能
        val startMultiply = System.nanoTime()
        val multiplied = NativeMath.multiplyDoubleArray(largeArray, 2.0)
        val endMultiply = System.nanoTime()
        println("乘法耗时: ${(endMultiply - startMultiply) / 1_000_000.0} ms")
        
        println("✅ 性能测试完成\n")
    }

    @Test
    fun testIsAvailable() {
        println("=== 可用性检查 ===")
        val available = NativeMath.isAvailable()
        println("NativeMath是否可用: $available")
        assertTrue("NativeMath应该可用", available)
        println("✅ 可用性测试通过\n")
    }
}
