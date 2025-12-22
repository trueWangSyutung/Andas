package cn.ac.oac.libs.andas

import cn.ac.oac.libs.andas.core.NativeMath

/**
 * 简单的JNI功能验证程序
 * 可以在Android设备上运行来验证JNI函数是否正常工作
 */
object TestNativeMath {
    
    @JvmStatic
    fun main(args: Array<String>) {
        println("=== NativeMath JNI功能测试 ===\n")
        
        try {
            // 测试1: 检查可用性
            println("1. 检查NativeMath是否可用...")
            val available = NativeMath.isAvailable()
            println("   结果: $available")
            if (!available) {
                println("❌ NativeMath不可用")
                return
            }
            println("✅ NativeMath可用\n")
            
            // 测试2: multiplyDoubleArray
            println("2. 测试 multiplyDoubleArray...")
            val array1 = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
            val result1 = NativeMath.multiplyDoubleArray(array1, 2.0)
            println("   输入: ${array1.joinToString()}")
            println("   乘数: 2.0")
            println("   结果: ${result1.joinToString()}")
            println("   预期: [2.0, 4.0, 6.0, 8.0, 10.0]")
            val success1 = result1.contentEquals(doubleArrayOf(2.0, 4.0, 6.0, 8.0, 10.0))
            println("   ${if (success1) "✅" else "❌"} 测试${if (success1) "通过" else "失败"}\n")
            
            // 测试3: sumDoubleArray
            println("3. 测试 sumDoubleArray...")
            val array2 = doubleArrayOf(582567.0354414985, -86165.50884370191, 619267.936907152)
            val result2 = NativeMath.sumDoubleArray(array2)
            println("   输入: ${array2.joinToString()}")
            println("   结果: $result2")
            val expected2 = 582567.0354414985 + (-86165.50884370191) + 619267.936907152
            println("   预期: $expected2")
            val success2 = Math.abs(result2 - expected2) < 0.001
            println("   ${if (success2) "✅" else "❌"} 测试${if (success2) "通过" else "失败"}\n")
            
            // 测试4: meanDoubleArray
            println("4. 测试 meanDoubleArray...")
            val array3 = doubleArrayOf(582567.0354414985, -86165.50884370191, 619267.936907152, -261499.41100525402, -368748.1343063426)
            val result3 = NativeMath.meanDoubleArray(array3)
            println("   输入: ${array3.joinToString()}")
            println("   结果: $result3")
            val expected3 = array3.average()
            println("   预期: $expected3")
            val success3 = Math.abs(result3 - expected3) < 0.001
            println("   ${if (success3) "✅" else "❌"} 测试${if (success3) "通过" else "失败"}\n")
            
            // 测试5: maxDoubleArray
            println("5. 测试 maxDoubleArray...")
            val array4 = doubleArrayOf(582567.0354414985, -86165.50884370191, 619267.936907152, -261499.41100525402)
            val result4 = NativeMath.maxDoubleArray(array4)
            println("   输入: ${array4.joinToString()}")
            println("   结果: $result4")
            val expected4 = array4.maxOrNull() ?: Double.NaN
            println("   预期: $expected4")
            val success4 = Math.abs(result4 - expected4) < 0.001
            println("   ${if (success4) "✅" else "❌"} 测试${if (success4) "通过" else "失败"}\n")
            
            // 测试6: minDoubleArray
            println("6. 测试 minDoubleArray...")
            val array5 = doubleArrayOf(582567.0354414985, -86165.50884370191, 619267.936907152, -261499.41100525402)
            val result5 = NativeMath.minDoubleArray(array5)
            println("   输入: ${array5.joinToString()}")
            println("   结果: $result5")
            val expected5 = array5.minOrNull() ?: Double.NaN
            println("   预期: $expected5")
            val success5 = Math.abs(result5 - expected5) < 0.001
            println("   ${if (success5) "✅" else "❌"} 测试${if (success5) "通过" else "失败"}\n")
            
            // 测试7: 基于真实数据集的测试
            println("7. 基于真实CSV数据的综合测试...")
            val doubleCol10 = doubleArrayOf(582567.0354414985, -86165.50884370191, 619267.936907152, -261499.41100525402, -368748.1343063426)
            val doubleCol11 = doubleArrayOf(-86165.50884370191, -368748.1343063426, -759575.3713505617, -368748.1343063426, -759575.3713505617)
            
            println("   列10数据: ${doubleCol10.joinToString()}")
            println("   列11数据: ${doubleCol11.joinToString()}")
            
            val sum10 = NativeMath.sumDoubleArray(doubleCol10)
            val sum11 = NativeMath.sumDoubleArray(doubleCol11)
            val mean10 = NativeMath.meanDoubleArray(doubleCol10)
            val mean11 = NativeMath.meanDoubleArray(doubleCol11)
            val max10 = NativeMath.maxDoubleArray(doubleCol10)
            val min10 = NativeMath.minDoubleArray(doubleCol10)
            val multiplied = NativeMath.multiplyDoubleArray(doubleCol10, 2.0)
            
            println("   列10求和: $sum10 (预期: ${doubleCol10.sum()})")
            println("   列11求和: $sum11 (预期: ${doubleCol11.sum()})")
            println("   列10均值: $mean10 (预期: ${doubleCol10.average()})")
            println("   列11均值: $mean11 (预期: ${doubleCol11.average()})")
            println("   列10最大值: $max10 (预期: ${doubleCol10.maxOrNull()})")
            println("   列10最小值: $min10 (预期: ${doubleCol10.minOrNull()})")
            println("   列10 × 2.0: ${multiplied.joinToString()}")
            
            val success7 = Math.abs(sum10 - doubleCol10.sum()) < 0.001 &&
                          Math.abs(sum11 - doubleCol11.sum()) < 0.001 &&
                          Math.abs(mean10 - doubleCol10.average()) < 0.001 &&
                          Math.abs(mean11 - doubleCol11.average()) < 0.001 &&
                          Math.abs(max10 - doubleCol10.maxOrNull()!!) < 0.001 &&
                          Math.abs(min10 - doubleCol10.minOrNull()!!) < 0.001
            
            println("   ${if (success7) "✅" else "❌"} 综合测试${if (success7) "通过" else "失败"}\n")
            
            // 总结
            val allSuccess = success1 && success2 && success3 && success4 && success5 && success7
            println("=== 测试总结 ===")
            if (allSuccess) {
                println("🎉 所有测试通过！JNI函数工作正常。")
            } else {
                println("❌ 部分测试失败，请检查实现。")
            }
            
        } catch (e: Exception) {
            println("❌ 测试过程中发生异常: ${e.message}")
            e.printStackTrace()
        }
    }
}
