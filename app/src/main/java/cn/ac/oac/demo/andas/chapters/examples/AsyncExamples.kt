package cn.ac.oac.demo.andas.chapters.examples

import android.content.Context
import android.util.Log
import cn.ac.oac.libs.andas.Andas
import cn.ac.oac.demo.andas.chapters.Example

/**
 * 第四章：异步操作示例
 */
object AsyncExamples {
    
    fun getExamples(): List<Example> {
        return listOf(
            csvRead(),
            csvWrite()
        )
    }
    
    private fun csvRead(): Example {
        return Example(
            id = "async_csv_read",
            title = "4.1 异步读取 CSV",
            description = "从 Assets 异步读取 CSV 文件",
            code = """
                Andas.getInstance().readCSVFromAssetsAsync(
                    "large_dataset.csv",
                    ",",
                    onSuccess = { df ->
                        Log.i("Andas", "读取成功: \$\{df.shape()}")
                    },
                    onError = { error ->
                        Log.e("Andas", "读取失败", error)
                    }
                )
            """.trimIndent(),
            action = { context, callback ->
                try {
                    Andas.getInstance().readCSVFromAssetsAsync(
                        "large_dataset.csv",
                        ",",
                        onSuccess = { df ->
                            callback("✅ 异步读取成功\n形状: ${df.shape()}\n列: ${df.columns()}\n预览:\n${df.head(3)}")
                        },
                        onError = { error ->
                            callback("❌ 读取失败: ${error.message}")
                        }
                    )
                } catch (e: Exception) {
                    callback("❌ 异步操作失败: ${e.message}")
                }
            }
        )
    }
    
    private fun csvWrite(): Example {
        return Example(
            id = "async_write_csv",
            title = "4.2 异步写入 CSV",
            description = "异步保存 DataFrame 到私有存储",
            code = """
                val df = Andas.getInstance().createDataFrame(
                    mapOf("id" to (1..1000).toList())
                )
                
                Andas.getInstance().saveToPrivateStorageAsync(
                    "output.csv", df,
                    onSuccess = {
                        Log.i("Andas", "保存成功")
                    },
                    onError = { error ->
                        Log.e("Andas", "保存失败", error)
                    }
                )
            """.trimIndent(),
            action = { context, callback ->
                try {
                    val df = Andas.getInstance().createDataFrame(
                        mapOf("id" to (1..1000).toList())
                    )
                    Andas.getInstance().saveToPrivateStorageAsync(
                        "output.csv", df,
                        onSuccess = {
                            callback("✅ 异步写入成功\n文件: output.csv")
                        },
                        onError = { error ->
                            callback("❌ 写入失败: ${error.message}")
                        }
                    )
                } catch (e: Exception) {
                    callback("❌ 异步写入失败: ${e.message}")
                }
            }
        )
    }
}
