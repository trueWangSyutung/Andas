package cn.ac.oac.demo.andas.chapters

import android.content.Context
import cn.ac.oac.demo.andas.chapters.examples.*
import cn.ac.oac.libs.andas.Andas
import cn.ac.oac.libs.andas.core.NativeMath

/**
 * 章节管理器 - 管理各章节，示例代码分散在独立文件中
 */
object ChapterManager {
    
    // 章节类型定义
    const val CHAPTER_INIT = "init"
    const val CHAPTER_SERIES = "series"
    const val CHAPTER_DATAFRAME = "dataframe"
    const val CHAPTER_ASYNC = "async"
    const val CHAPTER_JNI = "jni"
    const val CHAPTER_PERFORMANCE = "performance"
    const val CHAPTER_BATCH_CSV = "batch_csv"
    
    /**
     * 获取所有章节信息
     */
    fun getChapters(): List<ChapterInfo> {
        return listOf(
            ChapterInfo(
                id = CHAPTER_INIT,
                title = "第一章：SDK 初始化",
                description = "学习如何初始化 Andas SDK 和配置参数",
                icon = "🚀"
            ),
            ChapterInfo(
                id = CHAPTER_SERIES,
                title = "第二章：Series API",
                description = "掌握 Series 数据结构的核心操作",
                icon = "📊"
            ),
            ChapterInfo(
                id = CHAPTER_DATAFRAME,
                title = "第三章：DataFrame API",
                description = "学习 DataFrame 的创建和操作",
                icon = "📋"
            ),
            ChapterInfo(
                id = CHAPTER_ASYNC,
                title = "第四章：异步操作 API",
                description = "掌握异步文件读写和处理",
                icon = "⚡"
            ),
            ChapterInfo(
                id = CHAPTER_JNI,
                title = "第五章：JNI 原生 API",
                description = "体验高性能原生计算",
                icon = "🔧"
            ),
            ChapterInfo(
                id = CHAPTER_PERFORMANCE,
                title = "第六章：性能测试",
                description = "性能基准测试和对比",
                icon = "📈"
            ),
            ChapterInfo(
                id = CHAPTER_BATCH_CSV,
                title = "第七章：CSV分批处理",
                description = "大型CSV文件的分批处理工具",
                icon = "🗂️"
            )
        )
    }
    
    /**
     * 获取指定章节的示例代码
     */
    fun getChapterExamples(chapterId: String): List<Example> {
        return when (chapterId) {
            CHAPTER_INIT -> getInitExamples()
            CHAPTER_SERIES -> getSeriesExamples()
            CHAPTER_DATAFRAME -> getDataFrameExamples()
            CHAPTER_ASYNC -> getAsyncExamples()
            CHAPTER_JNI -> getJniExamples()
            CHAPTER_PERFORMANCE -> getPerformanceExamples()
            CHAPTER_BATCH_CSV -> getBatchCSVExamples()
            else -> emptyList()
        }
    }
    
    /**
     * 第一章：SDK 初始化示例
     */
    private fun getInitExamples(): List<Example> {
        return InitExamples.getExamples()
    }
    
    /**
     * 第二章：Series API 示例
     */
    private fun getSeriesExamples(): List<Example> {
        return SeriesExamples.getExamples()
    }
    
    /**
     * 第三章：DataFrame API 示例
     */
    private fun getDataFrameExamples(): List<Example> {
        return DataFrameExamples.getExamples()
    }
    
    /**
     * 第四章：异步操作示例
     */
    private fun getAsyncExamples(): List<Example> {
        return AsyncExamples.getExamples()
    }
    
    /**
     * 第五章：JNI 原生示例
     */
    private fun getJniExamples(): List<Example> {
        return JniExamples.getExamples()
    }
    
    /**
     * 第六章：性能测试示例
     */
    private fun getPerformanceExamples(): List<Example> {
        return PerformanceBenchmarkExamples.getExamples()
    }
    
    /**
     * 第七章：CSV分批处理示例
     */
    private fun getBatchCSVExamples(): List<Example> {
        // 合并原有的示例和新的性能实验
        val originalExamples = BatchCSVExamples.getExamples()
        val experimentExamples = BatchCSVPerformanceExperiment.getExperiments()
        return originalExamples + experimentExamples
    }
}

/**
 * 章节信息
 */
data class ChapterInfo(
    val id: String,
    val title: String,
    val description: String,
    val icon: String
)

/**
 * 示例代码
 */
data class Example(
    val id: String,
    val title: String,
    val description: String,
    val code: String,
    val action: (Context, (String) -> Unit) -> Unit
)
