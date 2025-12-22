package cn.ac.oac.demo.andas.chapters.examples

import android.content.Context
import cn.ac.oac.libs.andas.Andas
import cn.ac.oac.demo.andas.chapters.Example

/**
 * 第三章：DataFrame API 示例
 */
object DataFrameExamples {
    
    fun getExamples(): List<Example> {
        return listOf(
            createMap(),
            info(),
            headTail(),
            filter(),
            select(),
            stats(),
            chainOperations()
        )
    }
    
    private fun createMap(): Example {
        return Example(
            id = "df_create_map",
            title = "3.1 创建 DataFrame - Map 构造",
            description = "使用 Map 创建 DataFrame",
            code = """
                val df = Andas.getInstance().createDataFrame(
                    mapOf(
                        "name" to listOf("Alice", "Bob", "Charlie"),
                        "age" to listOf(25, 30, 35),
                        "salary" to listOf(50000, 60000, 70000)
                    )
                )
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = Andas.getInstance().createDataFrame(
                        mapOf(
                            "name" to listOf("Alice", "Bob", "Charlie"),
                            "age" to listOf(25, 30, 35),
                            "salary" to listOf(50000, 60000, 70000)
                        )
                    )
                    callback("✅ DataFrame 创建成功\n${df.toString()}")
                } catch (e: Exception) {
                    callback("❌ 创建失败: ${e.message}")
                }
            }
        )
    }
    
    private fun info(): Example {
        return Example(
            id = "df_info",
            title = "3.2 DataFrame 信息",
            description = "获取形状、列名、统计信息",
            code = """
                val df = Andas.getInstance().createDataFrame(
                    mapOf(
                        "name" to listOf("Alice", "Bob", "Charlie"),
                        "age" to listOf(25, 30, 35)
                    )
                )
                
                val shape = df.shape()          // (3, 2)
                val columns = df.columns()      // ["name", "age"]
                val info = df.info()            // 详细信息
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = Andas.getInstance().createDataFrame(
                        mapOf(
                            "name" to listOf("Alice", "Bob", "Charlie"),
                            "age" to listOf(25, 30, 35)
                        )
                    )
                    val shape = df.shape()
                    val columns = df.columns()
                    val info = df.info()
                    callback("✅ DataFrame 信息\n形状: $shape\n列: $columns\n信息: $info")
                } catch (e: Exception) {
                    callback("❌ 获取信息失败: ${e.message}")
                }
            }
        )
    }
    
    private fun headTail(): Example {
        return Example(
            id = "df_head_tail",
            title = "3.3 查看数据",
            description = "查看 DataFrame 的前/后几行",
            code = """
                val df = Andas.getInstance().createDataFrame(
                    mapOf(
                        "id" to (1..100).toList(),
                        "value" to (1..100).map { it * 1.5 }
                    )
                )
                
                val head = df.head()      // 前5行
                val tail = df.tail(3)     // 后3行
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = Andas.getInstance().createDataFrame(
                        mapOf(
                            "id" to (1..100).toList(),
                            "value" to (1..100).map { it * 1.5 }
                        )
                    )
                    val head = df.head()
                    val tail = df.tail(3)
                    callback("✅ 查看数据测试\n前5行:\n$head\n后3行:\n$tail")
                } catch (e: Exception) {
                    callback("❌ 查看失败: ${e.message}")
                }
            }
        )
    }
    
    private fun filter(): Example {
        return Example(
            id = "df_filter",
            title = "3.4 条件筛选",
            description = "按条件筛选行",
            code = """
                val df = Andas.getInstance().createDataFrame(
                    mapOf(
                        "name" to listOf("Alice", "Bob", "Charlie", "David"),
                        "age" to listOf(25, 30, 35, 40),
                        "salary" to listOf(50000, 60000, 70000, 80000)
                    )
                )
                
                val filtered = df.filter { row ->
                    val age = row["age"]?.get(0) as? Int
                    age != null && age > 30
                }
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = Andas.getInstance().createDataFrame(
                        mapOf(
                            "name" to listOf("Alice", "Bob", "Charlie", "David"),
                            "age" to listOf(25, 30, 35, 40),
                            "salary" to listOf(50000, 60000, 70000, 80000)
                        )
                    )
                    val filtered = df.filter { row ->
                        val age = row["age"]?.get(0) as? Int
                        age != null && age > 30
                    }
                    callback("✅ 条件筛选成功\n年龄>30的记录:\n$filtered")
                } catch (e: Exception) {
                    callback("❌ 筛选失败: ${e.message}")
                }
            }
        )
    }
    
    private fun select(): Example {
        return Example(
            id = "df_select",
            title = "3.5 列选择",
            description = "选择特定列，支持单列和多列选择",
            code = """
                val df = Andas.getInstance().createDataFrame(
                    mapOf(
                        "name" to listOf("Alice", "Bob", "Charlie"),
                        "age" to listOf(25, 30, 35),
                        "salary" to listOf(50000, 60000, 70000)
                    )
                )
                
                // 单列选择
                val names = df["name"]
                
                // 多列选择 - 使用 List
                val ageSalary = df[listOf("age", "salary")]
                
                // 多列选择 - 使用 selectColumns 方法
                val ageSalary2 = df.selectColumns("age", "salary")
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = Andas.getInstance().createDataFrame(
                        mapOf(
                            "name" to listOf("Alice", "Bob", "Charlie"),
                            "age" to listOf(25, 30, 35),
                            "salary" to listOf(50000, 60000, 70000)
                        )
                    )
                    val names = df["name"]
                    val ageSalary = df[listOf("age", "salary")]
                    val ageSalary2 = df.selectColumns("age", "salary")
                    callback("✅ 列选择测试\n单列选择:\n$names\n多列选择(df[listOf]):\n$ageSalary\n多列选择(selectColumns):\n$ageSalary2")
                } catch (e: Exception) {
                    callback("❌ 选择失败: ${e.message}")
                }
            }
        )
    }
    
    private fun stats(): Example {
        return Example(
            id = "df_stats",
            title = "3.6 统计计算",
            description = "DataFrame 统计分析",
            code = """
                val df = Andas.getInstance().createDataFrame(
                    mapOf(
                        "math" to listOf(85.0, 92.0, 78.0, 88.0),
                        "english" to listOf(76.0, 85.0, 90.0, 82.0)
                    )
                )
                
                val mathStats = df["math"]?.describe()
                val mathMean = df.meanNative("math")
                val mathSum = df.sumNative("math")
                val corr = df.corr()
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = Andas.getInstance().createDataFrame(
                        mapOf(
                            "math" to listOf(85.0, 92.0, 78.0, 88.0),
                            "english" to listOf(76.0, 85.0, 90.0, 82.0)
                        )
                    )
                    val mathStats = df["math"]?.describe()
                    val mathMean = df.meanNative("math")
                    val mathSum = df.sumNative("math")
                    val corr = df.corr()
                    callback("✅ 统计计算测试\n数学成绩统计: $mathStats\n数学平均值: $mathMean\n数学总和: $mathSum\n相关系数:$corr")
                } catch (e: Exception) {
                    callback("❌ 统计失败: ${e.message}")
                }
            }
        )
    }
    
    private fun chainOperations(): Example {
        return Example(
            id = "df_chain_operations",
            title = "3.7 链式操作",
            description = "DataFrame 链式操作：筛选、新增列、分组聚合",
            code = """
                val df = Andas.getInstance().createDataFrame(
                    mapOf(
                        "name" to listOf("Alice", "Bob", "Charlie", "David"),
                        "age" to listOf(25, 30, 35, 40),
                        "salary" to listOf(50000, 60000, 70000, 80000),
                        "department" to listOf("IT", "HR", "IT", "Finance")
                    )
                )
                
                val result = df
                        .filter { it["ages"]?.get(0) as Int > 25 }
                        .addColumn("category") { row ->
                            val age = row["ages"] as Int
                            when (age) {
                                in 20..29 -> "青年"
                                in 30..39 -> "中年"
                                else -> "其他"
                            }
                        }
                        .groupBy("category")
                        .agg(mapOf("salary" to "mean"))
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = Andas.getInstance().createDataFrame(
                        mapOf(
                            "name" to listOf("Alice", "Bob", "Charlie", "David"),
                            "ages" to listOf(25, 30, 35, 40),
                            "salary" to listOf(50000, 60000, 70000, 80000),
                            "department" to listOf("IT", "HR", "IT", "Finance")
                        )
                    )

                    val result = df
                        .filter { it["ages"]?.get(0) as Int > 25 }
                        .addColumn("category") { row ->
                            val age = row["ages"] as Int
                            when (age) {
                                in 20..29 -> "青年"
                                in 30..39 -> "中年"
                                else -> "其他"
                            }
                        }
                        .groupBy("category")
                        .agg(mapOf("salary" to "mean"))
                    
                    callback("✅ 链式操作成功\n结果:\n$result")
                } catch (e: Exception) {
                    callback("❌ 链式操作失败: ${e.message}")
                }
            }
        )
    }
}
