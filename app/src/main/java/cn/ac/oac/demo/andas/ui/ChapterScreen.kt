package cn.ac.oac.demo.andas.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.ac.oac.demo.andas.chapters.ChapterInfo
import cn.ac.oac.demo.andas.chapters.Example

/**
 * 章节选择界面
 */
@Composable
fun ChapterSelectionScreen(
    chapters: List<ChapterInfo>,
    onChapterClick: (String) -> Unit,
    onPaperExperimentClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        // 标题区域
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "Andas SDK Demo",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "选择章节开始学习",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        
        // 论文实验按钮（置顶显示）
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPaperExperimentClick() }
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFFFFFF), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔬",
                        fontSize = 24.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // 文字信息
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "论文实验：JVM与Python一致性验证",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "JVM大小对比与Python一致性实验",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE8F5E9),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 箭头
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "进入",
                    tint = Color.White,
                    modifier = Modifier.rotate(180f)
                )
            }
        }
        
        // 章节列表
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chapters) { chapter ->
                ChapterCard(chapter = chapter, onClick = { onChapterClick(chapter.id) })
            }
        }
    }
}

/**
 * 章节卡片
 */
@Composable
fun ChapterCard(chapter: ChapterInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chapter.icon,
                    fontSize = 24.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 文字信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                Text(
                    text = chapter.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 箭头
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "进入",
                tint = Color(0xFF1976D2),
                modifier = Modifier.rotate(180f)
            )
        }
    }
}

/**
 * 章节详情界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterDetailScreen(
    chapter: ChapterInfo,
    examples: List<Example>,
    onBack: () -> Unit,
    context: Context
) {
    var selectedExample by remember { mutableStateOf<Example?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chapter.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 左侧示例列表
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFF5F5F5))
            ) {
                ExampleList(
                    examples = examples,
                    onExampleClick = { selectedExample = it }
                )
            }
            
            // 右侧代码展示和结果
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .background(Color.White)
            ) {
                if (selectedExample != null) {
                    ExampleDetail(
                        example = selectedExample!!,
                        context = context
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "请选择左侧的示例",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

/**
 * 示例列表
 */
@Composable
fun ExampleList(
    examples: List<Example>,
    onExampleClick: (Example) -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(examples) { example ->
            ExampleListItem(example = example, onClick = { onExampleClick(example) })
        }
    }
}

/**
 * 示例列表项
 */
@Composable
fun ExampleListItem(example: Example, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = example.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Text(
                text = example.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * 示例详情（代码展示和运行）
 */
@Composable
fun ExampleDetail(
    example: Example,
    context: Context
) {
    var output by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = example.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // 描述
        Text(
            text = example.description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 代码展示
        Text(
            text = "代码实现:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        CodeDisplay(code = example.code)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 运行按钮
        Button(
            onClick = {
                if (!isRunning) {
                    isRunning = true
                    output = ""
                    example.action(context) { result ->
                        output = result
                        isRunning = false
                    }
                }
            },
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("运行示例")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 输出结果
        if (output.isNotEmpty()) {
            Text(
                text = "执行结果:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            OutputDisplay(output = output)
        }
    }
}

/**
 * 代码展示组件（带语法高亮效果）
 */
@Composable
fun CodeDisplay(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF263238), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color(0xFFD4D4D4),
            lineHeight = 16.sp
        )
    }
}

/**
 * 输出结果展示组件
 */
@Composable
fun OutputDisplay(output: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(
            text = output,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color(0xFF333333),
            lineHeight = 16.sp
        )
    }
}
