# NumPy / SciPy 数值内核性能与一致性实验

## 目的

验证 0.2.0 预览新增的 `NDArray`、线性代数、FFT、统计/回归、卷积与稀疏矩阵接口：
功能正确、与 Python NumPy 数值一致，并量化 JNI 相对 Kotlin 回退的收益。

## 环境

| 项目 | 配置 |
| --- | --- |
| 主机 | Apple M2, macOS 27.0.1 |
| Python | 3.10.11, NumPy 1.26.0 |
| JVM | OpenJDK 21.0.12, `-Xmx2g` |
| Android | 真机 Xiaomi M531DA / MT6899 / ARM Mali-G720 MC8, Android 17/API 37, 256 MB heap |
| 构建 | AGP 8.13.2, Kotlin 2.2.20, NDK arm64-v8a |

## 方法

- 固定 seed `20261102`，输入为 1M 随机 double、300×300 矩阵、1024 点 FFT、
  100k 信号与 16 抽头卷积核、20k×5 回归特征。
- 每个操作 warmup 1–2 次，取 3–5 次运行中位数；JVM 与 Android 使用同一组尺寸。
- Python 使用 NumPy 向量化实现；JVM 使用 Kotlin 回退；Android 使用 JNI。
- 原始数据：`bench/results/numpy_scipy_{python,jvm,android}.{json,csv,log}`。

## 结果

| 操作 | NumPy (ms) | JVM 回退 (ms) | Android JNI (ms) | Android/NumPy |
| --- | ---: | ---: | ---: | ---: |
| add, 1M | 0.95 | 8.65 | 10.30 | 10.8× |
| sum, 1M | 0.22 | 2.03 | 0.92 | 4.2× |
| matmul, 300×300 | 34.49 | 32.80 | 31.21 | 0.9× |
| FFT, 1024 | 0.008 | 17.36 | 0.61 | 73.9× |
| quantile, 1M | 32.90 | 92.30 | 124.57 | 3.8× |
| OLS, 20k×5 | 1.00 | 46.92 | 25.75 | 25.9× |
| convolve, 100k×16 | 0.75 | 4.52 | 2.09 | 2.8× |

关键结论：

- JNI radix-2 FFT 相对 Kotlin DFT 回退提升约 **28×**（17.36 ms → 0.61 ms），消除了 O(n²) 瓶颈；
- 原生 matmul 让 OLS 从 46.9 ms 降到 25.8 ms（1.8×）；
- 300×300 matmul 已与 NumPy 同量级，说明正常数级矩阵乘法不再需要额外优化；
- 逐元素 add/sum 仍受 JVM/Kotlin 调用与装箱影响，适合后续用批量 primitive kernel 进一步压缩；
- 真机 CPU/GPU、热节流与 256 MB 测试堆会放大分配/GC 成本，绝对时间不能直接与桌面 NumPy 对比。

## 一致性

参考值由 `bench/numpy_consistency.py` 生成（NumPy + `math.erf` + mpmath 正则化不完全 Beta），
覆盖 43 个指标：

- 求和、均值、样本标准差、方差、三分位；
- 3×3 行列式、线性方程解、3×3 matmul；
- 8 点 FFT 的实部/虚部；
- 4 点卷积、OLS 斜率/截距；
- 正态 CDF、单样本 t 统计量与 p 值。

JVM 测试 `NumpyConsistencyTest` 与 Android 测试 `NumpyConsistencyInstrumentedTest` 读取同一份
`numpy_consistency.csv`。浮点相对容差 `1e-10`，FFT 与极端 t p 值使用 `1e-9`；43/43 通过。

## 复现

```bash
python3 bench/numpy_consistency.py
python3 bench/numpy_scipy_bench.py

# JVM fallback
KOTLIN_STDLIB=$(ls ~/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.2.20/*/kotlin-stdlib-2.2.20.jar | head -1)
javac -cp "android-pandas/build/tmp/kotlin-classes/debug:$KOTLIN_STDLIB" \
  -d bench/java/out bench/java/NumpyBench.java
java -Xmx2g -cp "android-pandas/build/tmp/kotlin-classes/debug:$KOTLIN_STDLIB:bench/java/out" NumpyBench

# Android JNI
adb logcat -c
TEST_CLASS=cn.ac.oac.libs.andas.NumpyPerfInstrumentedTest ./bench/run_android_adb.sh
# 或 adb 直接执行：
# adb -s $ANDROID_SERIAL shell am instrument -w -r \
#   -e class cn.ac.oac.libs.andas.NumpyPerfInstrumentedTest \
#   cn.ac.oac.libs.andas.test/androidx.test.runner.AndroidJUnitRunner

# 一致性
./gradlew :android-pandas:testDebugUnitTest --tests '*NumpyConsistencyTest'
./gradlew :android-pandas:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=cn.ac.oac.libs.andas.NumpyConsistencyInstrumentedTest
```

## 结论

数值栈在功能、数值一致性上通过 JVM/Android 双端验证；JNI 对 FFT、矩阵乘法和回归有明确收益。
逐元素操作与分位数仍有优化空间，主要来自 Kotlin 循环与排序分配，而非算法复杂度。
