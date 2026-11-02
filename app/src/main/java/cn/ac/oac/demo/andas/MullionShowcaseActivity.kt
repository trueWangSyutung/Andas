package cn.ac.oac.demo.andas

import android.os.Bundle
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.isNaN
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

/**
 * MULLION 驱动的功能展廊：用同一份页面展示 Andas / NumPy / SciPy / 绘图 / 回归 / 注解。
 * 页面本身不写 if (width > 600)：AdaptiveScaffold + MultiPane 自动选择导航与分栏。
 */
class MullionShowcaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MullionTheme {
                MullionWindowProvider {
                    ShowcaseScreen(onBack = { finish() })
                }
            }
        }
    }
}

private data class Feature(
    val title: String,
    val description: String,
    val code: String
)

@Composable
private fun ShowcaseScreen(onBack: () -> Unit) {
    val features = remember {
        listOf(
            Feature(
                "numPy · NDArray",
                "创建、广播、矩阵乘法、线性代数与 FFT；JNI 不可用时自动回退 Kotlin。",
                """
val a = NDArray.arange(0.0, 6.0).reshape(2, 3)
val b = a.transpose()
val product = a.matmul(b)
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
                "折线、散点、柱状、直方图、饼图与子图；导出 SVG 或 Android Bitmap。",
                """
val figure = Figure(640, 420)
figure.plot(x, y, "series")
val bitmap = figure.renderBitmap()
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
    }
    var selected by remember { mutableIntStateOf(0) }
    var detailOpen by remember { mutableStateOf(false) }
    val items = listOf("NumPy", "Stats", "回归", "绘图", "转换", "扩展")
        .map { OrbitNavItem(it) }

    AdaptiveScaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "←",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable { onBack() }.padding(end = 12.dp)
                )
                Column {
                    Text("Andas × MULLION", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("自适应组件 + 数值计算", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        bottomBar = {
            OrbitNav(items, selected, onSelect = { selected = it; detailOpen = true })
        },
        rail = { OrbitNav(items, selected, onSelect = { selected = it; detailOpen = true }) },
        drawer = { OrbitNav(items, selected, onSelect = { selected = it; detailOpen = true }) },
        content = { padding ->
            MultiPane(
                list = {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text("功能展廊", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("MULLION 会按窗口宽度自动切换 BottomBar / Rail / Drawer 与 1–3 栏。",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        items(features) { feature ->
                            val index = features.indexOf(feature)
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selected = index
                                    detailOpen = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (index == selected)
                                        MaterialTheme.colorScheme.primaryContainer
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
                },
                detail = {
                    if (detailOpen) {
                        FeatureDetail(features[selected])
                    } else {
                        Overview()
                    }
                },
                preview = {
                    Preview(features[selected])
                },
                showDetail = detailOpen,
                onBack = { detailOpen = false }
            )
        }
    )
}

@Composable
private fun Overview() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Andas SDK", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("选择左侧功能，查看可运行代码与实时计算结果。")
            Spacer(Modifier.height(16.dp))
            Text("NDArray · SciPy · Figure · Regression · Annotations",
                style = MaterialTheme.typography.bodySmall)
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
                val bitmap = remember {
                    Figure(640, 420).apply {
                        title = "Andas Plot"
                        val axes = axes()
                        axes.title = "line + scatter"
                        axes.line(doubleArrayOf(0.0, 1.0, 2.0, 3.0), doubleArrayOf(1.0, 4.0, 9.0, 16.0), "square")
                        axes.scatter(doubleArrayOf(0.0, 1.0, 2.0, 3.0), doubleArrayOf(1.0, 4.0, 9.0, 16.0))
                    }.renderBitmap()
                }
                Image(bitmap.asImageBitmap(), contentDescription = "Andas plot", modifier = Modifier.fillMaxWidth())
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
                    "Try: registerSeries / registerNDArray / registerDataFrame")
            }
        }
    }
}

@Composable
private fun Preview(feature: Feature) {
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Text("MULLION preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(feature.title, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text("同一份 Screen 在窄屏为 BottomBar + 单栏，宽屏为 Rail/Drawer + 2–3 栏。",
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        Text("Andas API surface", style = MaterialTheme.typography.labelMedium)
        Text("Series · DataFrame · NDArray\nStats · Regression\nFigure · Annotations",
            style = MaterialTheme.typography.bodySmall)
    }
}
