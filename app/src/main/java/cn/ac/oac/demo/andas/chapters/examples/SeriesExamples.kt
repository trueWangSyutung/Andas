package cn.ac.oac.demo.andas.chapters.examples

import android.content.Context
import cn.ac.oac.libs.andas.Andas
import cn.ac.oac.demo.andas.chapters.Example
import cn.ac.oac.libs.andas.andasSeries
import cn.ac.oac.libs.andas.andasSeriesFromMap

/**
 * 第二章：Series API 示例
 */
object SeriesExamples {
    
    fun getExamples(): List<Example> {
        return listOf(
            // 创建
            createList(),
            createMap(),
            createWithFactory(),
            
            // 访问
            accessData(),
            viewData(),
            
            // 空值处理
            nullHandling(),
            nullHandlingNative(),
            
            // 统计
            statistics(),
            uniqueCount(),
            
            // 变换
            filterData(),
            sortData(),
            operations(),
            
            // 高性能操作
            batchProcessing(),
            sampling(),
            vectorizedOps(),
            
            // 状态检查
            nativeStatus()
        )
    }
    
    private fun createList(): Example {
        return Example(
            id = "series_create_list",
            title = "2.2.1 创建 Series - 列表构造",
            description = "使用列表创建 Series，支持自定义索引",
            code = """
                // 默认索引
                val series1 = andasSeries(listOf(1, 2, 3, 4, 5))
                
                // 自定义索引
                val series2 = andasSeries(
                    data = listOf(10, 20, 30, 40, 50),
                    index = listOf("a", "b", "c", "d", "e"),
                    name = "scores"
                )
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series1 = andasSeries(listOf(1, 2, 3, 4, 5))
                    val series2 = andasSeries(
                        data = listOf(10, 20, 30, 40, 50),
                        index = listOf("a", "b", "c", "d", "e"),
                        name = "scores"
                    )
                    callback("✅ Series 创建成功\n默认索引:\n$series1\n自定义索引:\n$series2")
                } catch (e: Exception) {
                    callback("❌ 创建失败: ${e.message}")
                }
            }
        )
    }
    
    private fun createMap(): Example {
        return Example(
            id = "series_create_map",
            title = "2.2.2 创建 Series - Map 构造",
            description = "使用 Map 创建 Series",
            code = """
                val grades = andasSeriesFromMap(
                    map = mapOf(
                        "Alice" to 85,
                        "Bob" to 92,
                        "Charlie" to 78
                    ),
                    name = "grades"
                )
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val grades = andasSeriesFromMap(
                        map = mapOf(
                            "Alice" to 85,
                            "Bob" to 92,
                            "Charlie" to 78
                        ),
                        name = "grades"
                    )
                    callback("✅ Map 创建 Series 成功\n$grades")
                } catch (e: Exception) {
                    callback("❌ 创建失败: ${e.message}")
                }
            }
        )
    }
    
    private fun createWithFactory(): Example {
        return Example(
            id = "series_create_factory",
            title = "2.2.3 创建 Series - 工厂函数",
            description = "使用工厂函数创建 Series",
            code = """
                // 使用 Andas 工厂
                val series = Andas.getInstance().createSeries(
                    data = listOf(1, 2, 3, 4, 5),
                    name = "numbers"
                )
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series = Andas.getInstance().createSeries(
                        data = listOf(1, 2, 3, 4, 5),
                        name = "numbers"
                    )
                    callback("✅ 工厂函数创建成功\n$series")
                } catch (e: Exception) {
                    callback("❌ 创建失败: ${e.message}")
                }
            }
        )
    }
    
    private fun accessData(): Example {
        return Example(
            id = "series_access",
            title = "2.3 数据访问",
            description = "位置访问和标签访问",
            code = """
                val series = andasSeries(
                    data = listOf(10, 20, 30, 40, 50),
                    index = listOf("a", "b", "c", "d", "e")
                )
                
                // 位置访问
                val value1 = series[2]  // 30
                
                // 标签访问
                val value2 = series["b"]  // 20
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series = andasSeries(
                        data = listOf(10, 20, 30, 40, 50),
                        index = listOf("a", "b", "c", "d", "e")
                    )
                    val value1 = series[2]
                    val value2 = series["b"]
                    callback("✅ 数据访问测试\n位置访问[2]: $value1\n标签访问['b']: $value2")
                } catch (e: Exception) {
                    callback("❌ 访问失败: ${e.message}")
                }
            }
        )
    }
    
    private fun viewData(): Example {
        return Example(
            id = "series_head_tail",
            title = "2.3.3 查看数据",
            description = "获取前/后几个元素",
            code = """
                val series = andasSeries((1..100).toList())
                
                val head5 = series.head()      // 前5个
                val head3 = series.head(3)     // 前3个
                val tail5 = series.tail()      // 后5个
                val tail3 = series.tail(3)     // 后3个
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series = andasSeries((1..100).toList())
                    val head5 = series.head()
                    val head3 = series.head(3)
                    val tail5 = series.tail()
                    val tail3 = series.tail(3)
                    callback("✅ 查看数据测试\n前5个: $head5\n前3个: $head3\n后5个: $tail5\n后3个: $tail3")
                } catch (e: Exception) {
                    callback("❌ 查看失败: ${e.message}")
                }
            }
        )
    }
    
    private fun nullHandling(): Example {
        return Example(
            id = "series_null_handling",
            title = "2.4.2 空值处理",
            description = "检测和处理空值",
            code = """
                val series = andasSeries(listOf(1, null, 3, null, 5))
                
                val isNull = series.isnull()          // [false, true, false, true, false]
                val notNull = series.notnull()        // [true, false, true, false, true]
                val cleaned = series.dropna()         // 删除空值
                val filled = series.fillna(0)         // 填充空值
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series = andasSeries(listOf(1, null, 3, null, 5))
                    val isNull = series.isnull()
                    val notNull = series.notnull()
                    val cleaned = series.dropna()
                    val filled = series.fillna(0)
                    callback("✅ 空值处理测试\n空值检测: $isNull\n非空检测: $notNull\n删除后: $cleaned\n填充后: $filled")
                } catch (e: Exception) {
                    callback("❌ 空值处理失败: ${e.message}")
                }
            }
        )
    }
    
    private fun nullHandlingNative(): Example {
        return Example(
            id = "series_null_handling_native",
            title = "2.4.3 原生空值处理",
            description = "高性能原生空值处理",
            code = """
                val series = andasSeries(listOf(1.0, null, 3.0, null, 5.0))
                
                val nullIndices = series.findNullIndices()  // [1, 3]
                val dropped = series.dropNullValues()       // 原生丢弃
                val filled = series.fillNullWithConstant(0.0) // 原生填充
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series = andasSeries(listOf(1.0, null, 3.0, null, 5.0))
                    val nullIndices = series.findNullIndices()
                    val dropped = series.dropNullValues()
                    val filled = series.fillNullWithConstant(0.0)
                    callback("✅ 原生空值处理测试\n空值索引: $nullIndices\n丢弃后: $dropped\n填充后: $filled")
                } catch (e: Exception) {
                    callback("❌ 原生处理失败: ${e.message}")
                }
            }
        )
    }
    
    private fun statistics(): Example {
        return Example(
            id = "series_statistics",
            title = "2.5.2 数值统计",
            description = "基础统计计算（原生加速）",
            code = """
                val series = andasSeries(listOf(1.0, 2.0, 3.0, 4.0, 5.0))
                
                val sum = series.sum()        // 15.0
                val mean = series.mean()      // 3.0
                val max = series.max()        // 5.0
                val min = series.min()        // 1.0
                val variance = series.variance()  // 2.5
                val std = series.std()        // 1.5811
                val desc = series.describe()  // 统计描述
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series = andasSeries(listOf(1.0, 2.0, 3.0, 4.0, 5.0))
                    val sum = series.sum()
                    val mean = series.mean()
                    val max = series.max()
                    val min = series.min()
                    val variance = series.variance()
                    val std = series.std()
                    val desc = series.describe()
                    callback("✅ 统计功能测试\nsum=$sum, mean=$mean\nmax=$max, min=$min\nvariance=$variance, std=$std\n描述: $desc")
                } catch (e: Exception) {
                    callback("❌ 统计失败: ${e.message}")
                }
            }
        )
    }
    
    private fun uniqueCount(): Example {
        return Example(
            id = "series_unique_count",
            title = "2.5.1 唯一值统计",
            description = "获取唯一值和频次统计",
            code = """
                val fruits = andasSeries(listOf("apple", "banana", "apple", "cherry", "banana"))
                
                val unique = fruits.unique()      // ["apple", "banana", "cherry"]
                val counts = fruits.valueCounts() // {apple=2, banana=2, cherry=1}
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val fruits = andasSeries(listOf("apple", "banana", "apple", "cherry", "banana"))
                    val unique = fruits.unique()
                    val counts = fruits.valueCounts()
                    callback("✅ 唯一值统计测试\n唯一值: $unique\n频次: $counts")
                } catch (e: Exception) {
                    callback("❌ 统计失败: ${e.message}")
                }
            }
        )
    }
    
    private fun filterData(): Example {
        return Example(
            id = "series_filter",
            title = "2.6.2 筛选过滤",
            description = "条件筛选和过滤",
            code = """
                val series = andasSeries(listOf(1, 2, 3, 4, 5))
                
                // Kotlin 筛选
                val filtered = series.filter { it?.compareTo(3) ?: -1 > 0 }
                
                // 原生高性能筛选
                val filteredNative = series.filterGreaterThan(3.0)
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series = andasSeries(listOf(1, 2, 3, 4, 5))
                    val filtered = series.filter { it?.compareTo(3) ?: -1 > 0 }
                    val filteredNative = series.filterGreaterThan(3.0)
                    callback("✅ 数据筛选测试\nKotlin筛选: $filtered\n原生筛选: $filteredNative")
                } catch (e: Exception) {
                    callback("❌ 筛选失败: ${e.message}")
                }
            }
        )
    }
    
    private fun sortData(): Example {
        return Example(
            id = "series_sort",
            title = "2.6.3 排序",
            description = "按值或索引排序",
            code = """
                val unsorted = andasSeries(
                    data = listOf(3, 1, 4, 2, 5),
                    index = listOf("c", "a", "d", "b", "e")
                )
                
                val byValue = unsorted.sortValues()           // 升序
                val descending = unsorted.sortValues(true)    // 降序
                val byIndex = unsorted.sortIndex()            // 按索引排序
                val native = unsorted.sortValuesNative(true)  // 原生降序
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val unsorted = andasSeries(
                        data = listOf(3, 1, 4, 2, 5),
                        index = listOf("c", "a", "d", "b", "e")
                    )
                    val byValue = unsorted.sortValues()
                    val descending = unsorted.sortValues(true)
                    val byIndex = unsorted.sortIndex()
                    val native = unsorted.sortValuesNative(true)
                    callback("✅ 数据排序测试\n按值升序: $byValue\n按值降序: $descending\n按索引: $byIndex\n原生降序: $native")
                } catch (e: Exception) {
                    callback("❌ 排序失败: ${e.message}")
                }
            }
        )
    }
    
    private fun operations(): Example {
        return Example(
            id = "series_operations",
            title = "2.7 数值计算",
            description = "映射、变换和计算",
            code = """
                val series = andasSeries(listOf(1.0, 2.0, 3.0, 4.0, 5.0))
                
                val doubled = series.map { it?.times(2) }     // [2, 4, 6, 8, 10]
                val cumsum = series.cumsum()                  // [1, 3, 6, 10, 15]
                val normalized = series.normalize()           // [0, 0.25, 0.5, 0.75, 1.0]
                val multiplied = series * 2                   // [2, 4, 6, 8, 10]
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series = andasSeries(listOf(1.0, 2.0, 3.0, 4.0, 5.0))
                    val doubled = series.map { it?.times(2) }
                    val cumsum = series.cumsum()
                    val normalized = series.normalize()
                    val multiplied = series * 2
                    callback("✅ 数值运算测试\n加倍: $doubled\n累加: $cumsum\n归一化: $normalized\n乘法: $multiplied")
                } catch (e: Exception) {
                    callback("❌ 运算失败: ${e.message}")
                }
            }
        )
    }
    
    private fun batchProcessing(): Example {
        return Example(
            id = "series_batch_processing",
            title = "2.9.1 批量处理",
            description = "大数据集批量处理",
            code = """
                val series = andasSeries((1..10000).map { it.toDouble() })
                
                val processed = series.processBatch(batchSize = 1000)
                // 自动分块处理，避免内存溢出
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series = andasSeries((1..10000).map { it.toDouble() })
                    val processed = series.processBatch(batchSize = 1000)
                    callback("✅ 批量处理测试\n原始大小: ${series.size()}\n处理后大小: ${processed.size()}")
                } catch (e: Exception) {
                    callback("❌ 批量处理失败: ${e.message}")
                }
            }
        )
    }
    
    private fun sampling(): Example {
        return Example(
            id = "series_sampling",
            title = "2.9.2 采样操作",
            description = "随机采样",
            code = """
                val series = andasSeries((1..10000).toList())
                
                val sample = series.sample(100)  // 采样100个元素
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series = andasSeries((1..10000).toList())
                    val sample = series.sample(100)
                    callback("✅ 采样测试\n原始大小: ${series.size()}\n采样大小: ${sample.size()}")
                } catch (e: Exception) {
                    callback("❌ 采样失败: ${e.message}")
                }
            }
        )
    }
    
    private fun vectorizedOps(): Example {
        return Example(
            id = "series_vectorized_ops",
            title = "2.9.3 向量化运算",
            description = "向量化加法、点积、范数",
            code = """
                val series1 = andasSeries(listOf(1.0, 2.0, 3.0))
                val series2 = andasSeries(listOf(10.0, 20.0, 30.0))
                
                val sum = series1 + series2      // [11.0, 22.0, 33.0]
                val minus = series2 + series1      // [9.0, 18.0, 27.0]
                val times = series2 * series1  // [10.0, 40.0, 90.0]
                val dotProduct = series1.dot(series2)  // 140.0
                val norm = series1.norm()        // sqrt(14) ≈ 3.74
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series1 = andasSeries(listOf(1.0, 2.0, 3.0))
                    val series2 = andasSeries(listOf(10.0, 20.0, 30.0))
                    val sum = series1 + series2
                    val minus = series2 - series1      // [9.0, 18.0, 27.0]
                    val times = series2 * series1  // [10.0, 40.0, 90.0]
                    val dotProduct = series1.dot(series2)
                    val norm = series1.norm()
                    callback("✅ 向量化运算测试\n加法: $sum\n点积: $dotProduct\n范数: $norm\n乘法：$times\n减法:\n$minus")
                } catch (e: Exception) {
                    callback("❌ 向量化运算失败: ${e.message}")
                }
            }
        )
    }
    
    private fun nativeStatus(): Example {
        return Example(
            id = "series_native_status",
            title = "2.10 原生库状态",
            description = "检查原生库可用性和性能",
            code = """
                val series = andasSeries(listOf(1.0, 2.0, 3.0))
                
                val isNativeAvailable = series.isNativeAvailable()
                val time = series.benchmarkOperation(1, 1000000)  // 基准测试
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series = andasSeries(listOf(1.0, 2.0, 3.0))
                    val isNativeAvailable = series.isNativeAvailable()
                    val time = series.benchmarkOperation(1, 1000000)
                    callback("✅ 原生库状态测试\n可用性: $isNativeAvailable\n基准测试耗时: ${time}ms")
                } catch (e: Exception) {
                    callback("❌ 状态检查失败: ${e.message}")
                }
            }
        )
    }
}
