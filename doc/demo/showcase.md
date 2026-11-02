# Demo 应用

`app` 是 **MULLION 驱动的完整 Demo**，默认启动入口即 MULLION 界面，包含全部原有页面与新增数值能力：

- **指南**：7 个章节（初始化、Series、DataFrame、异步、JNI、性能、CSV 批处理），每章示例可点击“运行示例”；
- **数值**：7 项能力（NDArray、Stats、Regression、基础绘图、更多图形、pandas ↔ numpy、扩展注解），结果实时计算；
- **论文**：JVM/Python 一致性与性能实验，可点击“开始真实实验”。

## 运行

```bash
./gradlew :app:installDebug
adb shell am start -n cn.ac.oac.demo.andas/.MainActivity
```

安装后默认启动 MULLION 入口；也可显式启动：

```bash
adb shell am start -n cn.ac.oac.demo.andas/.MullionShowcaseActivity
```

## MULLION 功能展廊

展廊使用 [MULLION](https://github.com/Mullion-Official/Mullion-Android-Kotlin-Compose) 的
`MullionTheme`、`MullionWindowProvider`、`AdaptiveScaffold`、`MultiPane` 与 `OrbitNav`：
窄屏使用 BottomBar + 单栏，宽屏自动切换 Rail/Drawer 与 2–3 栏。章节与论文页面在
窄屏自动从双栏切成“列表 → 详情”，避免文字被压成单列。

![MULLION 默认入口：学习指南](images/mullion-home.png)

![更多图形：热力图、箱线图、堆叠柱、横向柱](images/mullion-charts.png)

![论文实验在窄屏自动切换为单栏](images/mullion-paper.png)

展廊包含：

| 模块 | 展示内容 |
| --- | --- |
| NumPy · NDArray | 创建/reshape/广播/matmul、`Linalg.det`、FFT |
| SciPy · Stats | 正态 CDF、t 检验、中位数 |
| SciPy · Regression | 多元线性回归系数、截距与 R² |
| Matplotlib · Figure | 折线 + 面积 + 误差棒 Bitmap |
| Matplotlib · 更多图形 | 热力图、箱线图、堆叠柱、横向柱、阶梯/面积子图 |
| pandas ↔ numpy | `DataFrame.toNDArray()`、`toDataFrame()` 与 NaN/null |
| 扩展与注解 | `AndasExtensions` 注册与 `@AndasApi` 系列注解 |

## 设计约定

- 所有计算结果在设备本地实时执行，不依赖网络；
- 绘图使用 `Figure.renderBitmap()`，Compose 通过 `asImageBitmap()` 展示；
- 回归/统计结果直接展示，便于对照文档中的公式；
- Demo 仅用于功能展示，生产环境请按业务规模做性能与内存验证。
