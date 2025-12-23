package cn.ac.oac.demo.andas.chapters.examples

import android.content.Context
import cn.ac.oac.libs.andas.Andas
import cn.ac.oac.demo.andas.chapters.Example
import cn.ac.oac.libs.andas.factory.andasDataFrame
import cn.ac.oac.libs.andas.factory.andasDataFrameFromRows
import cn.ac.oac.libs.andas.entity.DataFrameIO

/**
 * 第三章：DataFrame API 示例
 */
object DataFrameExamples {
    
    fun getExamples(): List<Example> {
        return listOf(
            // 创建和查看
            createMap(),
            createRows(),
            viewInfo(),
            
            // 数据选择
            selectColumns(),
            selectRows(),
            filterData(),
            
            // 数据操作
            addColumns(),
            dropColumns(),
            renameColumns(),
            
            // 统计计算
            basicStats(),
            correlation(),
            describeStats(),
            normalizeData(),
            
            // 向量化运算
            vectorizedAdd(),
            vectorizedMultiply(),
            dotProduct(),
            normCalc(),
            
            // 空值处理
            nullDetection(),
            nullHandling(),
            
            // 分组聚合
            groupBySum(),
            groupByAgg(),
            
            // 排序筛选
            sortData(),
            advancedFilter(),
            
            // 数据合并
            mergeData(),
            joinData(),
            
            // 采样批量
            dataSampling(),
            batchProcessing(),
            
            // 数据导出
            exportData(),
            asyncIO()
        )
    }
    
    private fun createMap(): Example {
        return Example(
            id = "df_create_map",
            title = "3.1.1 创建 DataFrame - 列数据",
            description = "使用工厂函数从列数据创建 DataFrame",
            code = """
                import cn.ac.oac.libs.andas.factory.andasDataFrame
                
                val df = andasDataFrame(
                    mapOf(
                        "name" to listOf("Alice", "Bob", "Charlie", "David"),
                        "age" to listOf(25, 30, 35, 40),
                        "salary" to listOf(50000, 60000, 70000, 80000),
                        "department" to listOf("IT", "HR", "IT", "Finance")
                    )
                )
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "name" to listOf("Alice", "Bob", "Charlie", "David"),
                            "age" to listOf(25, 30, 35, 40),
                            "salary" to listOf(50000, 60000, 70000, 80000),
                            "department" to listOf("IT", "HR", "IT", "Finance")
                        )
                    )
                    callback("✅ 列数据创建成功\n${df.toString()}")
                } catch (e: Exception) {
                    callback("❌ 创建失败: ${e.message}")
                }
            }
        )
    }
    
    private fun createRows(): Example {
        return Example(
            id = "df_create_rows",
            title = "3.1.2 创建 DataFrame - 行数据",
            description = "使用工厂函数从行数据创建 DataFrame",
            code = """
                import cn.ac.oac.libs.andas.factory.andasDataFrameFromRows
                
                val rows = listOf(
                    mapOf("name" to "Alice", "age" to 25, "score" to 85.0),
                    mapOf("name" to "Bob", "age" to 30, "score" to 92.0),
                    mapOf("name" to "Charlie", "age" to 35, "score" to 78.0)
                )
                
                val df = andasDataFrameFromRows(rows)
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val rows = listOf(
                        mapOf("name" to "Alice", "age" to 25, "score" to 85.0),
                        mapOf("name" to "Bob", "age" to 30, "score" to 92.0),
                        mapOf("name" to "Charlie", "age" to 35, "score" to 78.0)
                    )
                    val df = andasDataFrameFromRows(rows)
                    callback("✅ 行数据创建成功\n$df")
                } catch (e: Exception) {
                    callback("❌ 创建失败: ${e.message}")
                }
            }
        )
    }
    
    private fun viewInfo(): Example {
        return Example(
            id = "df_view_info",
            title = "3.2 数据查看",
            description = "查看 DataFrame 基本信息、形状、列名、数据类型",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "name" to listOf("Alice", "Bob", "Charlie", "David"),
                        "age" to listOf(25, 30, 35, 40),
                        "salary" to listOf(50000, 60000, 70000, 80000)
                    )
                )
                
                val shape = df.shape()          // (4, 3)
                val columns = df.columns()      // [name, age, salary]
                val head = df.head()            // 前5行
                val tail = df.tail(3)           // 后3行
                val dtypes = df.dtypes()        // 数据类型
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "name" to listOf("Alice", "Bob", "Charlie", "David"),
                            "age" to listOf(25, 30, 35, 40),
                            "salary" to listOf(50000, 60000, 70000, 80000)
                        )
                    )
                    val shape = df.shape()
                    val columns = df.columns()
                    val head = df.head()
                    val tail = df.tail(3)
                    val dtypes = df.dtypes()
                    val isNative = df.isNativeAvailable()
                    
                    val result = """
                        ✅ 数据查看成功
                        形状: $shape
                        列名: $columns
                        前5行:
                        $head
                        后3行:
                        $tail
                        数据类型: $dtypes
                        原生库可用: $isNative
                    """.trimIndent()
                    callback(result)
                } catch (e: Exception) {
                    callback("❌ 查看失败: ${e.message}")
                }
            }
        )
    }
    
    private fun selectColumns(): Example {
        return Example(
            id = "df_select_columns",
            title = "3.3.1 列选择",
            description = "选择单列、多列",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "name" to listOf("Alice", "Bob", "Charlie"),
                        "age" to listOf(25, 30, 35),
                        "salary" to listOf(50000, 60000, 70000)
                    )
                )
                
                val names = df["name"]
                val selected = df[listOf("name", "salary")]
                val selected2 = df.selectColumns("name", "age")
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "name" to listOf("Alice", "Bob", "Charlie"),
                            "age" to listOf(25, 30, 35),
                            "salary" to listOf(50000, 60000, 70000)
                        )
                    )
                    val names = df["name"]
                    val selected = df[listOf("name", "salary")]
                    val selected2 = df.selectColumns("name", "age")
                    callback("✅ 列选择成功\n单列:\n$names\n多列1:\n$selected\n多列2:\n$selected2")
                } catch (e: Exception) {
                    callback("❌ 选择失败: ${e.message}")
                }
            }
        )
    }
    
    private fun selectRows(): Example {
        return Example(
            id = "df_select_rows",
            title = "3.3.2 行选择",
            description = "选择行、按标签/位置获取",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "name" to listOf("Alice", "Bob", "Charlie"),
                        "age" to listOf(25, 30, 35)
                    )
                )
                
                val row = df[0]
                val rowByLabel = df.loc(0)
                val rowByPos = df.iloc(0)
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "name" to listOf("Alice", "Bob", "Charlie"),
                            "age" to listOf(25, 30, 35)
                        )
                    )
                    val row = df[0]
                    val rowByLabel = df.loc(0)
                    val rowByPos = df.iloc(0)
                    callback("✅ 行选择成功\n第0行:\n$row\n标签获取:\n$rowByLabel\n位置获取:\n$rowByPos")
                } catch (e: Exception) {
                    callback("❌ 选择失败: ${e.message}")
                }
            }
        )
    }
    
    private fun filterData(): Example {
        return Example(
            id = "df_filter_data",
            title = "3.3.3 条件筛选",
            description = "按条件筛选行",
            code = """
                val df = andasDataFrame(
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
                    val df = andasDataFrame(
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
                    callback("✅ 条件筛选成功\n年龄 > 30:\n$filtered")
                } catch (e: Exception) {
                    callback("❌ 筛选失败: ${e.message}")
                }
            }
        )
    }
    
    private fun addColumns(): Example {
        return Example(
            id = "df_add_columns",
            title = "3.4.1 新增列",
            description = "添加计算列和数据列",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "name" to listOf("Alice", "Bob", "Charlie"),
                        "age" to listOf(25, 30, 35),
                        "salary" to listOf(50000, 60000, 70000)
                    )
                )
                
                val dfWithBonus = df.addColumn("bonus") { row ->
                    val salary = row["salary"]?.get(0) as? Int
                    salary?.times(0.1) ?: 0.0
                }
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "name" to listOf("Alice", "Bob", "Charlie"),
                            "age" to listOf(25, 30, 35),
                            "salary" to listOf(50000, 60000, 70000)
                        )
                    )
                    val dfWithBonus = df.addColumn("bonus") { row ->
                        val salary = row.get("salary") as Int
                        salary?.times(0.1) ?: 0.0
                    }
                    callback("✅ 新增列成功\n$dfWithBonus")
                } catch (e: Exception) {
                    callback("❌ 新增失败: ${e.message}")
                }
            }
        )
    }
    
    private fun dropColumns(): Example {
        return Example(
            id = "df_drop_columns",
            title = "3.4.2 删除列",
            description = "删除单列或多列",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "name" to listOf("Alice", "Bob", "Charlie"),
                        "age" to listOf(25, 30, 35),
                        "salary" to listOf(50000, 60000, 70000)
                    )
                )
                
                val dfWithoutAge = df.dropColumns("age")
                val dfSimple = df.dropColumns("age", "salary")
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "name" to listOf("Alice", "Bob", "Charlie"),
                            "age" to listOf(25, 30, 35),
                            "salary" to listOf(50000, 60000, 70000)
                        )
                    )
                    val dfWithoutAge = df.dropColumns("age")
                    val dfSimple = df.dropColumns("age", "salary")
                    callback("✅ 删除列成功\n删除age:\n$dfWithoutAge\n删除多列:\n$dfSimple")
                } catch (e: Exception) {
                    callback("❌ 删除失败: ${e.message}")
                }
            }
        )
    }
    
    private fun renameColumns(): Example {
        return Example(
            id = "df_rename_columns",
            title = "3.4.3 重命名列",
            description = "重命名 DataFrame 列",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "name" to listOf("Alice", "Bob", "Charlie"),
                        "age" to listOf(25, 30, 35),
                        "salary" to listOf(50000, 60000, 70000)
                    )
                )
                
                val dfRenamed = df.rename(
                    mapOf(
                        "name" to "姓名",
                        "age" to "年龄",
                        "salary" to "薪资"
                    )
                )
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "name" to listOf("Alice", "Bob", "Charlie"),
                            "age" to listOf(25, 30, 35),
                            "salary" to listOf(50000, 60000, 70000)
                        )
                    )
                    val dfRenamed = df.rename(
                        mapOf(
                            "name" to "姓名",
                            "age" to "年龄",
                            "salary" to "薪资"
                        )
                    )
                    callback("✅ 重命名成功\n$dfRenamed")
                } catch (e: Exception) {
                    callback("❌ 重命名失败: ${e.message}")
                }
            }
        )
    }
    
    private fun basicStats(): Example {
        return Example(
            id = "df_basic_stats",
            title = "3.5.1 基础统计",
            description = "求和、均值、最大最小、方差、标准差",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "math" to listOf(85.0, 92.0, 78.0, 88.0),
                        "english" to listOf(76.0, 85.0, 90.0, 82.0),
                        "age" to listOf(25, 30, 35, 40)
                    )
                )
                
                val mathSum = df.sum("math")
                val mathMean = df.mean("math")
                val mathMax = df.max("math")
                val mathMin = df.min("math")
                val mathVar = df.variance("math")
                val mathStd = df.std("math")
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "math" to listOf(85.0, 92.0, 78.0, 88.0),
                            "english" to listOf(76.0, 85.0, 90.0, 82.0),
                            "age" to listOf(25, 30, 35, 40)
                        )
                    )
                    val mathSum = df.sum("math")
                    val mathMean = df.mean("math")
                    val mathMax = df.max("math")
                    val mathMin = df.min("math")
                    val mathVar = df.variance("math")
                    val mathStd = df.std("math")
                    val result = """
                        ✅ 基础统计成功
                        数学总和: $mathSum
                        数学均值: $mathMean
                        数学最大值: $mathMax
                        数学最小值: $mathMin
                        数学方差: $mathVar
                        数学标准差: $mathStd
                    """.trimIndent()
                    callback(result)
                } catch (e: Exception) {
                    callback("❌ 统计失败: ${e.message}")
                }
            }
        )
    }
    
    private fun correlation(): Example {
        return Example(
            id = "df_correlation",
            title = "3.5.2 相关系数矩阵",
            description = "计算列间相关系数",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "math" to listOf(85.0, 92.0, 78.0, 88.0),
                        "english" to listOf(76.0, 85.0, 90.0, 82.0),
                        "age" to listOf(25, 30, 35, 40)
                    )
                )
                
                val correlation = df.corr()
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "math" to listOf(85.0, 92.0, 78.0, 88.0),
                            "english" to listOf(76.0, 85.0, 90.0, 82.0),
                            "age" to listOf(25, 30, 35, 40)
                        )
                    )
                    val correlation = df.corr()
                    callback("✅ 相关系数矩阵\n$correlation")
                } catch (e: Exception) {
                    callback("❌ 计算失败: ${e.message}")
                }
            }
        )
    }
    
    private fun describeStats(): Example {
        return Example(
            id = "df_describe_stats",
            title = "3.5.3 统计描述",
            description = "描述性统计分析",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "math" to listOf(85.0, 92.0, 78.0, 88.0)
                    )
                )
                
                val stats = df.describe("math")
                // 返回: count, mean, std, min, max
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "math" to listOf(85.0, 92.0, 78.0, 88.0)
                        )
                    )
                    val stats = df.describe("math")
                    val result = stats.entries.joinToString("\n") { (k, v) -> "$k: $v" }
                    callback("✅ 描述性统计\n$result")
                } catch (e: Exception) {
                    callback("❌ 统计失败: ${e.message}")
                }
            }
        )
    }
    
    private fun normalizeData(): Example {
        return Example(
            id = "df_normalize_data",
            title = "3.5.4 数据归一化",
            description = "标准归一化",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "math" to listOf(85.0, 92.0, 78.0, 88.0)
                    )
                )
                
                val normalizedDF = df.normalize("math")
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "math" to listOf(85.0, 92.0, 78.0, 88.0)
                        )
                    )
                    val normalizedDF = df.normalize("math")
                    callback("✅ 归一化成功\n$normalizedDF")
                } catch (e: Exception) {
                    callback("❌ 归一化失败: ${e.message}")
                }
            }
        )
    }
    
    private fun vectorizedAdd(): Example {
        return Example(
            id = "df_vectorized_add",
            title = "3.6.1 向量化加法",
            description = "列间向量化加法",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "col1" to listOf(1.0, 2.0, 3.0, 4.0),
                        "col2" to listOf(10.0, 20.0, 30.0, 40.0)
                    )
                )
                
                val resultDF = df.vectorizedAdd("col1", "col2", "sum")
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "col1" to listOf(1.0, 2.0, 3.0, 4.0),
                            "col2" to listOf(10.0, 20.0, 30.0, 40.0)
                        )
                    )
                    val resultDF = df.vectorizedAdd("col1", "col2", "sum")
                    callback("✅ 向量化加法\n$resultDF")
                } catch (e: Exception) {
                    callback("❌ 加法失败: ${e.message}")
                }
            }
        )
    }
    
    private fun vectorizedMultiply(): Example {
        return Example(
            id = "df_vectorized_multiply",
            title = "3.6.2 向量化乘法",
            description = "列间向量化乘法",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "col1" to listOf(1.0, 2.0, 3.0, 4.0),
                        "col2" to listOf(10.0, 20.0, 30.0, 40.0)
                    )
                )
                
                val resultDF = df.vectorizedMultiply("col1", "col2", "product")
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "col1" to listOf(1.0, 2.0, 3.0, 4.0),
                            "col2" to listOf(10.0, 20.0, 30.0, 40.0)
                        )
                    )
                    val resultDF = df.vectorizedMultiply("col1", "col2", "product")
                    callback("✅ 向量化乘法\n$resultDF")
                } catch (e: Exception) {
                    callback("❌ 乘法失败: ${e.message}")
                }
            }
        )
    }
    
    private fun dotProduct(): Example {
        return Example(
            id = "df_dot_product",
            title = "3.6.3 点积计算",
            description = "计算两列的点积",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "col1" to listOf(1.0, 2.0, 3.0, 4.0),
                        "col2" to listOf(10.0, 20.0, 30.0, 40.0)
                    )
                )
                
                val dotProduct = df.dotProduct("col1", "col2")
                // 结果: 300.0
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "col1" to listOf(1.0, 2.0, 3.0, 4.0),
                            "col2" to listOf(10.0, 20.0, 30.0, 40.0)
                        )
                    )
                    val dotProduct = df.dotProduct("col1", "col2")
                    callback("✅ 点积计算\n点积: $dotProduct")
                } catch (e: Exception) {
                    callback("❌ 点积失败: ${e.message}")
                }
            }
        )
    }
    
    private fun normCalc(): Example {
        return Example(
            id = "df_norm_calc",
            title = "3.6.4 范数计算",
            description = "计算向量范数",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "col1" to listOf(1.0, 2.0, 3.0, 4.0)
                    )
                )
                
                val norm = df.norm("col1")
                // 结果: sqrt(30) ≈ 5.477
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "col1" to listOf(1.0, 2.0, 3.0, 4.0)
                        )
                    )
                    val norm = df.norm("col1")
                    callback("✅ 范数计算\n范数: $norm")
                } catch (e: Exception) {
                    callback("❌ 范数失败: ${e.message}")
                }
            }
        )
    }
    
    private fun nullDetection(): Example {
        return Example(
            id = "df_null_detection",
            title = "3.7.1 空值检测",
            description = "检测和查找空值",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "name" to listOf("Alice", null, "Charlie", "David"),
                        "age" to listOf(25, 30, null, 40),
                        "salary" to listOf(50000, 60000, 70000, null)
                    )
                )
                
                val nullDF = df.isnull()
                val nullIndices = df.findNullIndices("name")
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "name" to listOf("Alice", null, "Charlie", "David"),
                            "age" to listOf(25, 30, null, 40),
                            "salary" to listOf(50000, 60000, 70000, null)
                        )
                    )
                    val nullDF = df.isnull()
                    val nullIndices = df.findNullIndices("name")
                    callback("✅ 空值检测\n空值矩阵:\n$nullDF\nname列空值索引: $nullIndices")
                } catch (e: Exception) {
                    callback("❌ 检测失败: ${e.message}")
                }
            }
        )
    }
    
    private fun nullHandling(): Example {
        return Example(
            id = "df_null_handling",
            title = "3.7.2 空值处理",
            description = "丢弃和填充空值",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "name" to listOf("Alice", null, "Charlie", "David"),
                        "age" to listOf(25, 30, null, 40),
                        "salary" to listOf(50000, 60000, 70000, null)
                    )
                )
                
                val cleanedDF = df.dropna()
                val filledDF = df.fillna("Unknown")
                val filledNativeDF = df.fillNull("salary", 0.0)
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "name" to listOf("Alice", null, "Charlie", "David"),
                            "age" to listOf(25, 30, null, 40),
                            "salary" to listOf(50000, 60000, 70000, null)
                        )
                    )
                    val cleanedDF = df.dropna()
                    val filledDF = df.fillna("Unknown")
                    val filledNativeDF = df.fillNull("salary", 0.0)
                    callback("✅ 空值处理\n丢弃空值:\n$cleanedDF\n填充空值:\n$filledDF\n原生填充:\n$filledNativeDF")
                } catch (e: Exception) {
                    callback("❌ 处理失败: ${e.message}")
                }
            }
        )
    }
    
    private fun groupBySum(): Example {
        return Example(
            id = "df_group_by_sum",
            title = "3.8.1 分组求和",
            description = "按列分组求和",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "department" to listOf("IT", "HR", "IT", "Finance", "HR"),
                        "salary" to listOf(50000, 60000, 70000, 80000, 65000)
                    )
                )
                
                val groupedSum = df.groupBySum("department", "salary")
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "department" to listOf("IT", "HR", "IT", "Finance", "HR"),
                            "salary" to listOf(50000, 60000, 70000, 80000, 65000)
                        )
                    )
                    val groupedSum = df.groupBySum("department", "salary")
                    callback("✅ 分组求和\n$groupedSum")
                } catch (e: Exception) {
                    callback("❌ 分组失败: ${e.message}")
                }
            }
        )
    }
    
    private fun groupByAgg(): Example {
        return Example(
            id = "df_group_by_agg",
            title = "3.8.2 分组聚合",
            description = "通用分组聚合操作",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "department" to listOf("IT", "HR", "IT", "Finance"),
                        "salary" to listOf(50000, 60000, 70000, 80000),
                        "age" to listOf(25, 30, 35, 40)
                    )
                )
                
                val groupBy = df.groupBy("department")
                val sumResult = groupBy.sum()
                val aggResult = df.agg(mapOf("salary" to "sum", "age" to "mean"))
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "department" to listOf("IT", "HR", "IT", "Finance"),
                            "salary" to listOf(50000, 60000, 70000, 80000),
                            "age" to listOf(25, 30, 35, 40)
                        )
                    )
                    val groupBy = df.groupBy("department")
                    val sumResult = groupBy.sum()
                    val aggResult = df.agg(mapOf("salary" to "sum", "age" to "mean"))
                    callback("✅ 分组聚合\n分组求和:\n$sumResult\n自定义聚合:\n$aggResult")
                } catch (e: Exception) {
                    callback("❌ 聚合失败: ${e.message}")
                }
            }
        )
    }
    
    private fun sortData(): Example {
        return Example(
            id = "df_sort_data",
            title = "3.9.1 数据排序",
            description = "按列排序和获取排序索引",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "name" to listOf("Alice", "Bob", "Charlie", "David"),
                        "age" to listOf(25, 30, 35, 40),
                        "salary" to listOf(50000, 60000, 70000, 80000)
                    )
                )
                
                val sortedAsc = df.sortValues("age", descending = false)
                val sortedDesc = df.sortValues("salary", descending = true)
                val indices = df.sortIndices("age")
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "name" to listOf("Alice", "Bob", "Charlie", "David"),
                            "age" to listOf(25, 30, 35, 40),
                            "salary" to listOf(50000, 60000, 70000, 80000)
                        )
                    )
                    val sortedAsc = df.sortValues("age", descending = false)
                    val sortedDesc = df.sortValues("salary", descending = true)
                    val indices = df.sortIndices("age")
                    callback("✅ 数据排序\n按年龄升序:\n$sortedAsc\n按工资降序:\n$sortedDesc\n年龄排序索引: $indices")
                } catch (e: Exception) {
                    callback("❌ 排序失败: ${e.message}")
                }
            }
        )
    }
    
    private fun advancedFilter(): Example {
        return Example(
            id = "df_advanced_filter",
            title = "3.9.2 高级筛选",
            description = "阈值筛选和布尔索引",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "name" to listOf("Alice", "Bob", "Charlie", "David"),
                        "age" to listOf(25, 30, 35, 40),
                        "salary" to listOf(50000, 60000, 70000, 80000)
                    )
                )
                
                val filtered = df.filterGreaterThan("salary", 65000)
                val whereResult = df.where("age", 30)
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "name" to listOf("Alice", "Bob", "Charlie", "David"),
                            "age" to listOf(25, 30, 35, 40),
                            "salary" to listOf(50000, 60000, 70000, 80000)
                        )
                    )
                    val filtered = df.filterGreaterThan("salary", 65000.0)
                    val whereResult = df.where("age", 30.0)
                    callback("✅ 高级筛选\n工资 > 65000:\n$filtered\n年龄 > 30:\n$whereResult")
                } catch (e: Exception) {
                    callback("❌ 筛选失败: ${e.message}")
                }
            }
        )
    }
    
    private fun mergeData(): Example {
        return Example(
            id = "df_merge_data",
            title = "3.10.1 数据合并",
            description = "合并两个DataFrame",
            code = """
                val df1 = andasDataFrame(
                    mapOf(
                        "id" to listOf(1, 2, 3),
                        "name" to listOf("Alice", "Bob", "Charlie")
                    )
                )
                
                val df2 = andasDataFrame(
                    mapOf(
                        "id" to listOf(2, 3, 4),
                        "salary" to listOf(60000, 70000, 80000)
                    )
                )
                
                val merged = df1.merge(df2, "id")
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df1 = andasDataFrame(
                        mapOf(
                            "id" to listOf(1, 2, 3),
                            "name" to listOf("Alice", "Bob", "Charlie")
                        )
                    )
                    val df2 = andasDataFrame(
                        mapOf(
                            "id" to listOf(2, 3, 4),
                            "salary" to listOf(60000, 70000, 80000)
                        )
                    )
                    val merged = df1.merge(df2, "id")
                    callback("✅ 数据合并\n$merged")
                } catch (e: Exception) {
                    callback("❌ 合并失败: ${e.message}")
                }
            }
        )
    }
    
    private fun joinData(): Example {
        return Example(
            id = "df_join_data",
            title = "3.10.2 SQL风格连接",
            description = "内连接、左连接、右连接、全外连接",
            code = """
                val df1 = andasDataFrame(
                    mapOf(
                        "id" to listOf(1, 2, 3),
                        "name" to listOf("Alice", "Bob", "Charlie")
                    )
                )
                
                val df2 = andasDataFrame(
                    mapOf(
                        "id" to listOf(2, 3, 4),
                        "salary" to listOf(60000, 70000, 80000)
                    )
                )
                
                val innerJoin = df1.join(df2, "id", "inner")
                val leftJoin = df1.join(df2, "id", "left")
                val rightJoin = df1.join(df2, "id", "right")
                val outerJoin = df1.join(df2, "id", "outer")
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df1 = andasDataFrame(
                        mapOf(
                            "id" to listOf(1, 2, 3),
                            "name" to listOf("Alice", "Bob", "Charlie")
                        )
                    )
                    val df2 = andasDataFrame(
                        mapOf(
                            "id" to listOf(2, 3, 4),
                            "salary" to listOf(60000, 70000, 80000)
                        )
                    )
                    val innerJoin = df1.join(df2, "id", "inner")
                    val leftJoin = df1.join(df2, "id", "left")
                    val rightJoin = df1.join(df2, "id", "right")
                    val outerJoin = df1.join(df2, "id", "outer")
                    callback("✅ SQL连接\n内连接:\n$innerJoin\n左连接:\n$leftJoin\n右连接:\n$rightJoin\n全外连接:\n$outerJoin")
                } catch (e: Exception) {
                    callback("❌ 连接失败: ${e.message}")
                }
            }
        )
    }
    
    private fun dataSampling(): Example {
        return Example(
            id = "df_data_sampling",
            title = "3.11.1 数据采样",
            description = "随机采样",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "id" to (1..1000).toList(),
                        "value" to (1..1000).map { it * 1.5 }
                    )
                )
                
                val sample = df.sample(100)
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "id" to (1..1000).toList(),
                            "value" to (1..1000).map { it * 1.5 }
                        )
                    )
                    val sample = df.sample(100)
                    callback("✅ 数据采样\n采样形状: ${sample.shape()}\n采样数据:\n$sample")
                } catch (e: Exception) {
                    callback("❌ 采样失败: ${e.message}")
                }
            }
        )
    }
    
    private fun batchProcessing(): Example {
        return Example(
            id = "df_batch_processing",
            title = "3.11.2 批量处理",
            description = "大数据集批量处理",
            code = """
                val df = andasDataFrame(
                    mapOf(
                        "id" to (1..10000).toList(),
                        "value" to (1..10000).map { it * 1.5 }
                    )
                )
                
                val processedDF = df.processBatch("value", batchSize = 1000)
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "id" to (1..10000).toList(),
                            "value" to (1..10000).map { it * 1.5 }
                        )
                    )
                    val processedDF = df.processBatch("value", batchSize = 1000)
                    callback("✅ 批量处理\n处理完成，形状: ${processedDF.shape()}")
                } catch (e: Exception) {
                    callback("❌ 批量处理失败: ${e.message}")
                }
            }
        )
    }
    
    private fun exportData(): Example {
        return Example(
            id = "df_export_data",
            title = "3.12.1 数据导出",
            description = "导出CSV文件",
            code = """
                import java.io.File
                import cn.ac.oac.libs.andas.entity.DataFrameIO
                
                val df = andasDataFrame(
                    mapOf(
                        "name" to listOf("Alice", "Bob", "Charlie"),
                        "age" to listOf(25, 30, 35)
                    )
                )
                
                // 导出到文件
                val file = File(context.filesDir, "output.csv")
                df.toCSV(file)
                
                // 使用DataFrameIO
                DataFrameIO.saveToPrivateStorage(context, "data.csv", df)
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = andasDataFrame(
                        mapOf(
                            "name" to listOf("Alice", "Bob", "Charlie"),
                            "age" to listOf(25, 30, 35)
                        )
                    )
                    // 使用DataFrameIO保存
                    DataFrameIO.saveToPrivateStorage(context, "test_output.csv", df)
                    callback("✅ 数据导出\n文件已保存到私有存储: test_output.csv")
                } catch (e: Exception) {
                    callback("❌ 导出失败: ${e.message}")
                }
            }
        )
    }
    
    private fun asyncIO(): Example {
        return Example(
            id = "df_async_io",
            title = "3.12.2 异步IO操作",
            description = "异步读取和保存",
            code = """
                import cn.ac.oac.libs.andas.entity.DataFrameIO
                
                // 异步读取Assets
                DataFrameIO.readFromAssetsAsync(
                    context.assets,
                    "data.csv",
                    onSuccess = { df ->
                        println("异步读取成功: \$\{df.shape()}")
                    },
                    onError = { error ->
                        println("读取失败: \$\{error.message}")
                    }
                )
                
                // 异步保存
                DataFrameIO.saveToPrivateStorageAsync(
                    context,
                    "output.csv",
                    df,
                    onSuccess = {
                        println("异步保存成功")
                    },
                    onError = { error ->
                        println("保存失败: \$\{error.message}")
                    }
                )
            """.trimIndent(),
            action = { context, callback ->
                try {
                    // 创建测试数据
                    val df = andasDataFrame(
                        mapOf(
                            "name" to listOf("Alice", "Bob", "Charlie"),
                            "age" to listOf(25, 30, 35)
                        )
                    )
                    
                    // 异步保存
                    DataFrameIO.saveToPrivateStorageAsync(
                        context,
                        "async_output.csv",
                        df,
                        onSuccess = {
                            callback("✅ 异步IO操作\n异步保存成功: async_output.csv")
                        },
                        onError = { error ->
                            callback("❌ 异步IO失败: ${error.message}")
                        }
                    )
                } catch (e: Exception) {
                    callback("❌ 异步IO失败: ${e.message}")
                }
            }
        )
    }
}
