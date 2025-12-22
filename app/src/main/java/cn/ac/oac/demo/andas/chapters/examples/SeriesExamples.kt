package cn.ac.oac.demo.andas.chapters.examples

import android.content.Context
import cn.ac.oac.libs.andas.Andas
import cn.ac.oac.demo.andas.chapters.Example

/**
 * 第二章：Series API 示例
 */
object SeriesExamples {
    
    fun getExamples(): List<Example> {
        return listOf(
            createList(),
            createMap(),
            accessData(),
            viewData(),
            nullHandling(),
            statistics(),
            uniqueCount(),
            filterData(),
            sortData(),
            operations()
        )
    }
    
    private fun createList(): Example {
        return Example(
            id = "series_create_list",
            title = "2.1 创建 Series - 列表构造",
            description = "使用列表创建 Series，支持自定义索引",
            code = """
                // 默认索引
                val series1 = Andas.getInstance().createSeries(
                    listOf(1, 2, 3, 4, 5),
                    name = "numbers"
                )
                
                // 自定义索引
                val series2 = Andas.getInstance().createSeries(
                    data = listOf(10, 20, 30, 40, 50),
                    index = listOf("a", "b", "c", "d", "e"),
                    name = "scores"
                )
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series1 = Andas.getInstance().createSeries(
                        listOf(1, 2, 3, 4, 5),
                        name = "numbers"
                    )
                    val series2 = Andas.getInstance().createSeries(
                        data = listOf(10, 20, 30, 40, 50),
                        index = listOf("a", "b", "c", "d", "e"),
                        name = "scores"
                    )
                    callback("✅ Series 创建成功\n默认索引: $series1\n自定义索引: $series2")
                } catch (e: Exception) {
                    callback("❌ 创建失败: ${e.message}")
                }
            }
        )
    }
    
    private fun createMap(): Example {
        return Example(
            id = "series_create_map",
            title = "2.2 创建 Series - Map 构造",
            description = "使用 Map 创建 Series",
            code = """
                val grades = Andas.getInstance().createSeriesFromMap(
                    mapOf(
                        "Alice" to 85,
                        "Bob" to 92,
                        "Charlie" to 78
                    ),
                    name = "grades"
                )
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val grades = Andas.getInstance().createSeriesFromMap(
                        mapOf(
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
    
    private fun accessData(): Example {
        return Example(
            id = "series_access",
            title = "2.3 数据访问",
            description = "位置访问和标签访问",
            code = """
                val series = Andas.getInstance().createSeries(
                    listOf(10, 20, 30, 40, 50),
                    listOf("a", "b", "c", "d", "e")
                )
                
                // 位置访问
                val value1 = series[2]  // 30
                
                // 标签访问
                val value2 = series["b"]  // 20
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series = Andas.getInstance().createSeries(
                        listOf(10, 20, 30, 40, 50),
                        listOf("a", "b", "c", "d", "e")
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
            title = "2.4 查看数据",
            description = "获取前/后几个元素",
            code = """
                val series = Andas.getInstance().createSeries((1..100).toList())
                
                val head5 = series.head()      // 前5个
                val tail3 = series.tail(3)     // 后3个
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series = Andas.getInstance().createSeries((1..100).toList())
                    val head5 = series.head()
                    val tail3 = series.tail(3)
                    callback("✅ 查看数据测试\n前5个: $head5\n后3个: $tail3")
                } catch (e: Exception) {
                    callback("❌ 查看失败: ${e.message}")
                }
            }
        )
    }
    
    private fun nullHandling(): Example {
        return Example(
            id = "series_null_handling",
            title = "2.5 空值处理",
            description = "检测和处理空值",
            code = """
                val series = Andas.getInstance().createSeries(
                    listOf(1, null, 3, null, 5)
                )
                
                val isNull = series.isnull()          // [false, true, false, true, false]
                val cleaned = series.dropna()         // 删除空值
                val filled = series.fillna(0)         // 填充空值
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series = Andas.getInstance().createSeries(
                        listOf(1, null, 3, null, 5)
                    )
                    val isNull = series.isnull()
                    val cleaned = series.dropna()
                    val filled = series.fillna(0)
                    callback("✅ 空值处理测试\n空值检测: $isNull\n删除后: $cleaned\n填充后: $filled")
                } catch (e: Exception) {
                    callback("❌ 空值处理失败: ${e.message}")
                }
            }
        )
    }
    
    private fun statistics(): Example {
        return Example(
            id = "series_statistics",
            title = "2.6 统计功能",
            description = "基础统计计算（原生加速）",
            code = """
                val series = Andas.getInstance().createSeries(
                    listOf(1.0, 2.0, 3.0, 4.0, 5.0)
                )
                
                val sum = series.sum()        // 15.0
                val mean = series.mean()      // 3.0
                val max = series.max()        // 5.0
                val min = series.min()        // 1.0
                val variance = series.variance()  // 2.5
                val std = series.std()        // 1.5811
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series = Andas.getInstance().createSeries(
                        listOf(1.0, 2.0, 3.0, 4.0, 5.0)
                    )
                    val sum = series.sum()
                    val mean = series.mean()
                    val max = series.max()
                    val min = series.min()
                    val variance = series.variance()
                    val std = series.std()
                    callback("✅ 统计功能测试\nsum=$sum, mean=$mean\nmax=$max, min=$min\nvariance=$variance, std=$std")
                } catch (e: Exception) {
                    callback("❌ 统计失败: ${e.message}")
                }
            }
        )
    }
    
    private fun uniqueCount(): Example {
        return Example(
            id = "series_unique_count",
            title = "2.7 唯一值统计",
            description = "获取唯一值和频次统计",
            code = """
                val fruits = Andas.getInstance().createSeries(
                    listOf("apple", "banana", "apple", "cherry", "banana")
                )
                
                val unique = fruits.unique()      // ["apple", "banana", "cherry"]
                val counts = fruits.valueCounts() // {apple=2, banana=2, cherry=1}
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val fruits = Andas.getInstance().createSeries(
                        listOf("apple", "banana", "apple", "cherry", "banana")
                    )
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
            title = "2.8 数据筛选",
            description = "条件筛选和过滤",
            code = """
                val series = Andas.getInstance().createSeries(
                    listOf(1, 2, 3, 4, 5)
                )
                
                // 大于3的元素
                val filtered = series.filter { it?.compareTo(3) ?: -1 > 0 }
                // 原生高性能筛选
                val filteredNative = series.filterGreaterThan(3.0)
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series = Andas.getInstance().createSeries(
                        listOf(1, 2, 3, 4, 5)
                    )
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
            title = "2.9 数据排序",
            description = "按值或索引排序",
            code = """
                val unsorted = Andas.getInstance().createSeries(
                    listOf(3, 1, 4, 2, 5),
                    listOf("c", "a", "d", "b", "e")
                )
                
                val byValue = unsorted.sortValues()           // 升序
                val descending = unsorted.sortValues(true)    // 降序
                val byIndex = unsorted.sortIndex()            // 按索引排序
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val unsorted = Andas.getInstance().createSeries(
                        listOf(3, 1, 4, 2, 5),
                        listOf("c", "a", "d", "b", "e")
                    )
                    val byValue = unsorted.sortValues()
                    val descending = unsorted.sortValues(true)
                    val byIndex = unsorted.sortIndex()
                    callback("✅ 数据排序测试\n按值升序: $byValue\n按值降序: $descending\n按索引: $byIndex")
                } catch (e: Exception) {
                    callback("❌ 排序失败: ${e.message}")
                }
            }
        )
    }
    
    private fun operations(): Example {
        return Example(
            id = "series_operations",
            title = "2.10 数值运算",
            description = "映射、变换和计算",
            code = """
                val series = Andas.getInstance().createSeries(
                    listOf(1.0, 2.0, 3.0, 4.0, 5.0)
                )
                
                val doubled = series.map { it?.times(2) }     // [2, 4, 6, 8, 10]
                val cumsum = series.cumsum()                  // [1, 3, 6, 10, 15]
                val normalized = series.normalize()           // [0, 0.25, 0.5, 0.75, 1.0]
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val series = Andas.getInstance().createSeries(
                        listOf(1.0, 2.0, 3.0, 4.0, 5.0)
                    )
                    val doubled = series.map { it?.times(2) }
                    val cumsum = series.cumsum()
                    val normalized = series.normalize()
                    callback("✅ 数值运算测试\n加倍: $doubled\n累加: $cumsum\n归一化: $normalized")
                } catch (e: Exception) {
                    callback("❌ 运算失败: ${e.message}")
                }
            }
        )
    }
}
