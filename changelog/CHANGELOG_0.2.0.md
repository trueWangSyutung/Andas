# Andas 0.2.0：数值计算、扩展注解与 Demo

发布日期：2026-11-02

## 新增

### NumPy 风格 NDArray

- 支持 `FLOAT64` / `FLOAT32` / `INT32` / `INT64` / `BOOLEAN` dtype；
- 任意维度 reshape、transpose、axis 归约、concatenate、where；
- 标量/同形状/任意维度右对齐广播；
- `Linalg`：det、inv、solve、lstsq、幂迭代、matmul；
- `FFT`：JNI radix-2 + Kotlin DFT 回退；`NumpyRandom` 可复现随机数。

### SciPy 风格科学计算

- 统计：describe、分位数、正态 CDF/逆 CDF、Pearson、t 检验、Welch 检验、直方图；
- 回归：多元线性/多项式/岭/逻辑回归；
- 优化、积分（含自适应 `quad`）、线性/三次样条插值；
- 信号：卷积、FFT 卷积、滑动平均、Hann/Hamming/Blackman、去趋势、频谱；
- 稀疏矩阵（CSR）、图像处理（灰度、高斯模糊、Sobel、阈值、缩放）；
- 特殊函数：erf、gamma、beta、logBeta。

### Matplotlib 风格绘图

- 折线、散点、柱状、直方图、饼图与子图；
- 轴范围、对数轴、线宽、散点大小、柱状分类标签；
- Bitmap PNG 保存与跨平台 SVG 导出。

### 扩展机制

- `@AndasApi`、`@ExperimentalAndas`、`@InternalAndas`、`@NativeAccelerated`、`@AndasFunction`；
- `AndasExtensions` 运行时注册 Series/DataFrame/NDArray 扩展函数。

### 互转与 Demo

- `DataFrame/Series ↔ NDArray`，NaN 与 null 可配置；
- MULLION 自适应 Demo：`AdaptiveScaffold` + `MultiPane` + `OrbitNav` 展示全部数值能力；
- Android Demo APK 随 GitHub Release 发布。

## 实验

- 43 项 NumPy/mpmath 参考值一致性校验，JVM/Android 全部通过；
- JNI FFT 相对 Kotlin 回退约 62×；原生 matmul 让 OLS 从 46.9 ms 降至 13.7 ms；
- 详见 [bench/NUMPY_SCIPY_BENCHMARK.md](../bench/NUMPY_SCIPY_BENCHMARK.md) 与
  [report/andas-technical-report.pdf](../report/andas-technical-report.pdf)。

## 兼容性

- 公开 pandas 风格 API 保持兼容；
- 最低 Android API 仍为 26；
- Kotlin 升级至 2.2.20，Compose Demo 使用 Compose BOM 2025.09.00；
- Demo APK 使用 debug keystore 签名，仅用于体验，不用于生产分发。

## 安装

```kotlin
implementation("com.github.trueWangSyutung:Andas:0.2.0")
```

Demo APK：见 [Release 0.2.0](https://github.com/trueWangSyutung/Andas/releases/tag/0.2.0)。
