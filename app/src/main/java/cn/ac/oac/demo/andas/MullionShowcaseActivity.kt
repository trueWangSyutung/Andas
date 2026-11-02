package cn.ac.oac.demo.andas

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.ac.oac.demo.andas.chapters.ChapterInfo
import cn.ac.oac.demo.andas.chapters.ChapterManager
import cn.ac.oac.demo.andas.ui.ChapterDetailScreen
import cn.ac.oac.demo.andas.ui.PaperExperimentScreen
import cn.ac.oac.libs.andas.Andas
import cn.ac.oac.libs.andas.annotation.AndasExtensions
import cn.ac.oac.libs.andas.numpy.FFT
import cn.ac.oac.libs.andas.numpy.Linalg
import cn.ac.oac.libs.andas.numpy.NDArray
import cn.ac.oac.libs.andas.numpy.toDataFrame
import cn.ac.oac.libs.andas.numpy.toNDArray
import cn.ac.oac.libs.andas.plot.Figure
import cn.ac.oac.libs.andas.scipy.MultipleLinearRegression
import cn.ac.oac.libs.andas.scipy.Stats
import dev.mullion.compose.adaptive.AdaptiveScaffold
import dev.mullion.compose.adaptive.MultiPane
import dev.mullion.compose.adaptive.OrbitNav
import dev.mullion.compose.adaptive.OrbitNavItem
import dev.mullion.compose.theme.MullionTheme
import dev.mullion.compose.window.MullionWindowProvider
import kotlin.isNaN

/**
 * MULLION 驱动的完整 Demo：原有章节、论文实验与新增数值/绘图/回归/转换/注解全部可点击运行。
 * 页面本身不写宽度判断，AdaptiveScaffold + MultiPane 自动选择导航与 1–3 栏。
 */
class MullionShowcaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Andas.initialize(this) {
                debugMode = true
                logLevel = Andas.LogLevel.DEBUG
                timeoutSeconds = 60L
                cachePath = "andas_demo_cache"
            }
        } catch (error: Exception) {
            Log.e("AndasDemo", "SDK initialization failed", error)
        }
        setContent {
            MullionTheme {
                MullionWindowProvider {
                    ShowcaseScreen(onBack = { finish() })
                }
            }
        }
    }
}

private enum class Section(val label: String) {
    GUIDE("指南"),
    NUMERIC("数值"),
    PAPER("论文")
}

private data class Feature(
    val title: String,
    val description: String,
    val code: String
)

@Composable
private fun ShowcaseScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val chapters = remember { ChapterManager.getChapters() }
    val features = remember { featureList() }
    var section by remember { mutableIntStateOf(0) }
    var selectedChapter by remember { mutableStateOf<ChapterInfo?>(null) }
    var selectedFeature by remember { mutableIntStateOf(0) }
    var detailOpen by remember { mutableStateOf(false) }
    val items = Section.entries.map { OrbitNavItem(it.label) }

    AdaptiveScaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "←",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable {
                        if (detailOpen) detailOpen = false else onBack()
                    }.padding(end = 12.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text("Andas × MULLION", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("完整功能 Demo：章节 / 数值 / 论文实验", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = "Release 0.2.1",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        bottomBar = {
            OrbitNav(items, section, onSelect = {
                section = it
                detailOpen = it == Section.PAPER.ordinal
            })
        },
        rail = {
            OrbitNav(items, section, onSelect = {
                section = it
                detailOpen = it == Section.PAPER.ordinal
            })
        },
        drawer = {
            OrbitNav(items, section, onSelect = {
                section = it
                detailOpen = it == Section.PAPER.ordinal
            })
        },
        content = { padding ->
            MultiPane(
                list = {
                    when (Section.entries[section]) {
                        Section.GUIDE -> ChapterList(chapters, padding) { chapter ->
                            selectedChapter = chapter
                            detailOpen = true
                        }
                        Section.NUMERIC -> FeatureList(features, selectedFeature, padding) { index ->
                            selectedFeature = index
                            detailOpen = true
                        }
                        Section.PAPER -> PaperList(padding) { detailOpen = true }
                    }
                },
                detail = {
                    when {
                        section == Section.GUIDE.ordinal && detailOpen && selectedChapter != null -> {
                            val chapter = selectedChapter!!
                            ChapterDetailScreen(
                                chapter = chapter,
                                examples = ChapterManager.getChapterExamples(chapter.id),
                                onBack = { detailOpen = false },
                                context = context
                            )
                        }
                        section == Section.NUMERIC.ordinal && detailOpen -> {
                            FeatureDetail(features[selectedFeature])
                        }
                        section == Section.PAPER.ordinal && detailOpen -> {
                            PaperExperimentScreen(onBack = { detailOpen = false }, context = context)
                        }
                        else -> Overview(Section.entries[section])
                    }
                },
                preview = {
                    Preview(Section.entries[section], selectedChapter, features[selectedFeature])
                },
                showDetail = detailOpen,
                onBack = { detailOpen = false }
            )
        }
    )
}

@Composable
private fun ChapterList(chapters: List<ChapterInfo>, padding: androidx.compose.foundation.layout.PaddingValues, onClick: (ChapterInfo) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("学习指南", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("共 ${chapters.size} 章；点击进入并运行每个示例。", style = MaterialTheme.typography.bodySmall)
        }
        items(chapters) { chapter ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onClick(chapter) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(chapter.icon, style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(40.dp))
                    Column {
                        Text(chapter.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(chapter.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureList(features: List<Feature>, selected: Int, padding: androidx.compose.foundation.layout.PaddingValues, onClick: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("数值能力", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("点击任意卡片运行对应计算并查看实时结果。", style = MaterialTheme.typography.bodySmall)
        }
        items(features) { feature ->
            val index = features.indexOf(feature)
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onClick(index) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (index == selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(feature.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(feature.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun PaperList(padding: androidx.compose.foundation.layout.PaddingValues, onClick: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("论文实验", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("JVM/Python 一致性、性能与 Android 实测。", style = MaterialTheme.typography.bodySmall)
        }
        items(listOf("JVM 与 Python 一致性验证", "本地 CSV 性能实验")) { title ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onClick() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("点击打开并运行实验", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun Overview(section: Section) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when (section) {
                    Section.GUIDE -> "Andas 学习指南"
                    Section.NUMERIC -> "数值计算"
                    Section.PAPER -> "论文实验"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                when (section) {
                    Section.GUIDE -> "选择左侧章节，进入后点击“运行”执行示例。"
                    Section.NUMERIC -> "选择左侧功能，查看代码并执行实时计算。"
                    Section.PAPER -> "选择实验，查看一致性/性能结果。"
                }
            )
            Spacer(Modifier.height(16.dp))
            Text("MULLION 会根据窗口宽度自动切换导航与分栏。", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FeatureDetail(feature: Feature) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text(feature.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(feature.description, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(14.dp))
        Text("示例代码", style = MaterialTheme.typography.labelLarge)
        Text(
            feature.code,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            ).padding(12.dp).fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        Text("运行结果", style = MaterialTheme.typography.labelLarge)
        when {
            feature.title.startsWith("matplotlib") -> {
                val bitmap = remember(feature.title) {
                    if (feature.title.contains("更多")) {
                        Figure(900, 620, rows = 2, cols = 2).apply {
                            title = "Andas Chart Gallery"
                            axes(0, 0).apply {
                                title = "heatmap"
                                heatmap(NDArray.from2D(arrayOf(
                                    doubleArrayOf(1.0, 3.0, 5.0),
                                    doubleArrayOf(2.0, 4.0, 6.0),
                                    doubleArrayOf(5.0, 2.0, 1.0)
                                )))
                            }
                            axes(0, 1).apply {
                                title = "box"
                                box(listOf("A", "B"), listOf(
                                    doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0),
                                    doubleArrayOf(2.0, 3.0, 3.5, 4.5, 6.0)
                                ))
                            }
                            axes(1, 0).apply {
                                title = "stacked"
                                stackedBar(
                                    listOf("Q1", "Q2", "Q3"),
                                    listOf(doubleArrayOf(1.0, 2.0, 1.5), doubleArrayOf(2.0, 1.0, 2.5)),
                                    listOf("线上", "线下")
                                )
                            }
                            axes(1, 1).apply {
                                title = "barh"
                                barh(listOf("A", "B", "C"), doubleArrayOf(4.0, 8.0, 6.0))
                            }
                        }.renderBitmap()
                    } else {
                        Figure(640, 460).apply {
                            title = "Andas Plot Gallery"
                            val axes = axes()
                            axes.title = "line + area + errorbar"
                            axes.line(doubleArrayOf(0.0, 1.0, 2.0, 3.0), doubleArrayOf(1.0, 4.0, 9.0, 16.0), "line")
                            axes.area(doubleArrayOf(0.0, 1.0, 2.0, 3.0), doubleArrayOf(0.5, 2.0, 4.0, 8.0), "area")
                            axes.errorBar(
                                doubleArrayOf(0.0, 1.0, 2.0, 3.0),
                                doubleArrayOf(1.0, 4.0, 9.0, 16.0),
                                doubleArrayOf(0.3, 0.5, 0.8, 1.2),
                                "error"
                            )
                        }.renderBitmap()
                    }
                }
                Image(bitmap.asImageBitmap(), contentDescription = "Andas plot gallery", modifier = Modifier.fillMaxWidth())
            }
            feature.title.startsWith("numPy") -> {
                val a = NDArray.arange(0.0, 6.0).reshape(2, 3)
                val product = a.matmul(a.transpose())
                val spectrum = FFT.fft(NDArray.of(1.0, 1.0, 1.0, 1.0))
                Text("matmul = ${product.to2DArray().joinToString { it.contentToString() }}\n" +
                    "fft[0] = ${spectrum.real[0]}\n" +
                    "det = ${Linalg.det(NDArray.from2D(arrayOf(doubleArrayOf(4.0, 7.0), doubleArrayOf(2.0, 6.0))))}")
            }
            feature.title.startsWith("scipy · Stats") -> {
                val x = NDArray.of(1.0, 2.0, 3.0, 4.0, 5.0)
                Text("normalCdf(2) = ${Stats.normalCdf(2.0)}\n" +
                    "t = ${Stats.ttestOneSample(x, 3.0).t}\n" +
                    "median = ${Stats.median(x)}")
            }
            feature.title.startsWith("scipy · Regression") -> {
                val x = NDArray.from2D(arrayOf(
                    doubleArrayOf(1.0, 2.0), doubleArrayOf(2.0, 1.0),
                    doubleArrayOf(3.0, 4.0), doubleArrayOf(4.0, 3.0)
                ))
                val y = NDArray.of(9.0, 8.0, 19.0, 18.0)
                val model = MultipleLinearRegression.fit(x, y, listOf("x1", "x2"))
                Text("intercept = ${model.intercept}\ncoefficients = ${model.coefficients.toDoubleArray().contentToString()}\nR² = ${model.r2}")
            }
            feature.title.startsWith("pandas") -> {
                val df = remember {
                    cn.ac.oac.libs.andas.entity.DataFrame(
                        mapOf("a" to listOf(1, null, 3), "b" to listOf(4.0, 5.0, 6.0))
                    )
                }
                val array = df.toNDArray()
                Text("shape = ${array.shape.contentToString()}\nNaN at (1,0) = ${array.get(1, 0).isNaN()}\nround-trip null = ${array.toDataFrame(listOf("a", "b")).at(1, "a")}")
            }
            else -> {
                Text("Registered extensions: ${AndasExtensions.list().size}\n" +
                    "调用 AndasExtensions.registerSeries / registerNDArray / registerDataFrame 可注册自定义函数。")
            }
        }
    }
}

@Composable
private fun Preview(section: Section, chapter: ChapterInfo?, feature: Feature) {
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Text("MULLION preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            when (section) {
                Section.GUIDE -> chapter?.title ?: "选择章节"
                Section.NUMERIC -> feature.title
                Section.PAPER -> "一致性 / 性能实验"
            },
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        Text("同一份 Screen 在窄屏为 BottomBar + 单栏，宽屏为 Rail/Drawer + 2–3 栏。",
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        Text("全部入口", style = MaterialTheme.typography.labelMedium)
        Text("章节 7 · 数值 7 · 论文实验 2", style = MaterialTheme.typography.bodySmall)
    }
}

private fun featureList(): List<Feature> = listOf(
    Feature(
        "numPy · NDArray",
        "Dtype、广播、矩阵乘法、线性代数与 FFT；JNI 不可用时自动回退 Kotlin。",
        """
val a = NDArray.arange(0.0, 6.0).reshape(2, 3)
val product = a.matmul(a.transpose())
val spectrum = FFT.fft(NDArray.of(1.0, 1.0, 1.0, 1.0))
        """.trimIndent()
    ),
    Feature(
        "scipy · Stats",
        "描述统计、正态分布、t 检验、Pearson 相关与分位数。",
        """
val x = NDArray.of(1.0, 2.0, 3.0, 4.0, 5.0)
val test = Stats.ttestOneSample(x, 3.0)
val cdf = Stats.normalCdf(2.0)
        """.trimIndent()
    ),
    Feature(
        "scipy · Regression",
        "多元线性回归、多项式回归、岭回归与逻辑回归，均可直接预测。",
        """
val model = MultipleLinearRegression.fit(features, target)
val fit = model.predict(newFeatures)
        """.trimIndent()
    ),
    Feature(
        "matplotlib · Figure",
        "折线、面积、误差棒、散点、柱状、直方图、饼图、热力图与子图。",
        """
val figure = Figure(640, 460)
figure.axes().line(x, y, "line")
figure.axes().area(x, y, "area")
val bitmap = figure.renderBitmap()
        """.trimIndent()
    ),
    Feature(
        "matplotlib · 更多图形",
        "热力图、箱线图、堆叠柱、横向柱、阶梯线与面积图，支持子图布局。",
        """
val grid = Figure(900, 600, rows = 2, cols = 2)
grid.axes(0, 0).heatmap(matrix)
grid.axes(0, 1).box(labels, distributions)
grid.axes(1, 0).stackedBar(labels, stacks, seriesLabels)
grid.axes(1, 1).barh(labels, values)
        """.trimIndent()
    ),
    Feature(
        "pandas ↔ numpy",
        "DataFrame/Series 与 NDArray 双向转换，NaN 与 null 可配置。",
        """
val array = dataFrame.toNDArray()
val restored = array.toDataFrame(columns)
val series = ndarray.toSeries()
        """.trimIndent()
    ),
    Feature(
        "扩展与注解",
        "AndasApi / ExperimentalAndas / NativeAccelerated 与可注册扩展函数。",
        """
AndasExtensions.registerSeries("sum") { s -> s.toDoubleArray().sum() }
val value = AndasExtensions.invokeSeries("sum", series)
        """.trimIndent()
    )
)
