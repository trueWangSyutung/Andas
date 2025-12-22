package cn.ac.oac.demo.andas

import cn.ac.oac.demo.andas.chapters.ChapterManager
import org.junit.Test
import org.junit.Assert.*

/**
 * Demo 功能测试
 */
class DemoTest {
    
    @Test
    fun testChapterManager() {
        val manager = ChapterManager
        
        // 测试获取所有章节
        val chapters = manager.getChapters()
        assertTrue("应该有6个章节", chapters.size == 6)
        
        // 验证章节信息
        assertEquals("第一章应该是初始化", "第一章：SDK 初始化", chapters[0].title)
        assertEquals("第二章应该是Series", "第二章：Series API", chapters[1].title)
        assertEquals("第三章应该是DataFrame", "第三章：DataFrame API", chapters[2].title)
        
        // 测试获取示例
        val initExamples = manager.getChapterExamples("init")
        assertTrue("初始化章节应该有示例", initExamples.isNotEmpty())
        
        val seriesExamples = manager.getChapterExamples("series")
        assertTrue("Series章节应该有示例", seriesExamples.isNotEmpty())
        
        val dfExamples = manager.getChapterExamples("dataframe")
        assertTrue("DataFrame章节应该有示例", dfExamples.isNotEmpty())
        
        println("✅ ChapterManager 测试通过")
        println("章节列表:")
        chapters.forEach { 
            println("  ${it.id}: ${it.title} (${it.icon})") 
        }
    }
    
    @Test
    fun testExampleStructure() {
        val manager = ChapterManager
        val examples = manager.getChapterExamples("series")
        
        // 验证示例结构
        val example = examples.first()
        assertNotNull("示例应该有ID", example.id)
        assertNotNull("示例应该有标题", example.title)
        assertNotNull("示例应该有描述", example.description)
        assertNotNull("示例应该有代码", example.code)
        assertNotNull("示例应该有动作", example.action)
        
        // 验证代码格式
        assertTrue("代码应该包含关键字", example.code.contains("Andas") || example.code.contains("Series"))
        
        println("✅ Example 结构测试通过")
        println("示例代码预览:")
        println(example.code)
    }
    
    @Test
    fun testAllChaptersHaveExamples() {
        val manager = ChapterManager
        val chapters = manager.getChapters()
        
        // 确保每个章节都有示例
        chapters.forEach { chapter ->
            val examples = manager.getChapterExamples(chapter.id)
            assertTrue("章节 ${chapter.title} 应该有示例", examples.isNotEmpty())
            println("✅ ${chapter.title} 有 ${examples.size} 个示例")
        }
    }
}
