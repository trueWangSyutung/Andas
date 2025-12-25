package cn.ac.oac.demo.andas

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import cn.ac.oac.demo.andas.chapters.ChapterManager
import cn.ac.oac.demo.andas.ui.ChapterDetailScreen
import cn.ac.oac.demo.andas.ui.ChapterSelectionScreen
import cn.ac.oac.demo.andas.ui.PaperExperimentScreen
import cn.ac.oac.demo.andas.ui.theme.AndasTheme
import cn.ac.oac.libs.andas.Andas

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 初始化Andas SDK
        initializeAndasSDK()

        setContent {
            AndasTheme {
                DemoApp()
            }
        }
    }

    private fun initializeAndasSDK() {
        try {
            Andas.initialize(this) {
                debugMode = true
                logLevel = Andas.LogLevel.DEBUG
                timeoutSeconds = 60L
                cachePath = "andas_demo_cache"
            }
            Log.i("AndasDemo", "SDK初始化成功")
        } catch (e: Exception) {
            Log.e("AndasDemo", "SDK初始化失败", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 销毁SDK，释放资源
        Andas.getInstance().destroy()
    }
}

/**
 * 主应用组件
 */
@Composable
fun DemoApp() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.ChapterList) }
    var currentChapterId by remember { mutableStateOf<String?>(null) }
    
    // 获取章节管理器
    val chapterManager = ChapterManager
    val chapters = chapterManager.getChapters()
    
    when (currentScreen) {
        is Screen.ChapterList -> {
            ChapterSelectionScreen(
                chapters = chapters,
                onChapterClick = { chapterId ->
                    currentChapterId = chapterId
                    currentScreen = Screen.ChapterDetail
                },
                onPaperExperimentClick = {
                    currentScreen = Screen.PaperExperiment
                }
            )
        }
        
        is Screen.ChapterDetail -> {
            currentChapterId?.let { chapterId ->
                val chapter = chapters.find { it.id == chapterId }
                val examples = chapterManager.getChapterExamples(chapterId)
                
                if (chapter != null) {
                    ChapterDetailScreen(
                        chapter = chapter,
                        examples = examples,
                        onBack = {
                            currentScreen = Screen.ChapterList
                            currentChapterId = null
                        },
                        context = context
                    )
                }
            }
        }
        
        is Screen.PaperExperiment -> {
            PaperExperimentScreen(
                onBack = {
                    currentScreen = Screen.ChapterList
                },
                context = context
            )
        }
    }
}

/**
 * 屏幕导航类型
 */
sealed class Screen {
    object ChapterList : Screen()
    object ChapterDetail : Screen()
    object PaperExperiment : Screen()
}
