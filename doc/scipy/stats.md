# SciPy 风格科学计算

`cn.ac.oac.libs.andas.scipy` 提供统计、回归、优化、积分、插值、信号与特殊函数。
底层数组使用 `NDArray`，JNI 可用时自动加速 `erf`、`gamma`、卷积与直方图。

## 统计

```kotlin
import cn.ac.oac.libs.andas.numpy.NDArray
import cn.ac.oac.libs.andas.scipy.Stats

val x = NDArray.of(1.0, 2.0, 3.0, 4.0, 5.0)
val description = Stats.describe(x)
val median = Stats.median(x)
val q90 = Stats.quantile(x, 0.90)
val p = Stats.normalCdf(2.0)
val pdf = Stats.normalPdf(2.0)
val r = Stats.pearson(x, x)
val t = Stats.ttestOneSample(x, popMean = 3.0)
val regression = Stats.linregress(x, NDArray.of(3.0, 5.0, 7.0, 9.0))
```

`TTestResult` 包含 `t`、双侧 `p` 与自由度；`LinearRegressionResult` 包含斜率、截距、相关系数、
p 与标准误。

## 回归

```kotlin
import cn.ac.oac.libs.andas.scipy.MultipleLinearRegression
import cn.ac.oac.libs.andas.scipy.PolynomialRegression
import cn.ac.oac.libs.andas.scipy.RidgeRegression
import cn.ac.oac.libs.andas.scipy.LogisticRegression

val model = MultipleLinearRegression.fit(features, target, listOf("x1", "x2"))
val prediction = model.predict(newFeatures)
val summary = model.summary()          // r2 / adjusted_r2 / rmse / coefficients

val polynomial = PolynomialRegression.fit(x, y, degree = 3)
val polyPrediction = PolynomialRegression.predict(polynomial, NDArray.of(4.0))

val ridge = RidgeRegression.fit(features, target, lambda = 0.1)

val logistic = LogisticRegression(learningRate = 0.1, iterations = 1000)
    .fit(binaryFeatures, binaryLabels)
val labels = logistic.predict(binaryFeatures)
val accuracy = logistic.accuracy(binaryFeatures, binaryLabels)
```

## 优化与积分

```kotlin
import cn.ac.oac.libs.andas.scipy.Optimize
import cn.ac.oac.libs.andas.scipy.Integrate

val minimum = Optimize.goldenSection({ (it - 2.0) * (it - 2.0) }, -5.0, 5.0)
val root = Optimize.newton1D({ it * it - 2.0 }, { 2.0 * it }, start = 1.0)
val weights = Optimize.gradientDescent(
    gradient = { x -> doubleArrayOf(2 * x[0], 2 * x[1]) },
    start = doubleArrayOf(3.0, -2.0)
)
val area = Integrate.trapezoid(NDArray.of(0.0, 1.0, 4.0))
val simpson = Integrate.simpson(NDArray.of(0.0, 1.0, 4.0))
```

## 插值与信号

```kotlin
import cn.ac.oac.libs.andas.scipy.LinearInterpolator
import cn.ac.oac.libs.andas.scipy.CubicSpline
import cn.ac.oac.libs.andas.scipy.Signal

val linear = LinearInterpolator(NDArray.of(0.0, 10.0), NDArray.of(0.0, 100.0))
val value = linear.evaluate(5.0)

val spline = CubicSpline(x, y)
val smooth = spline.evaluate(2.5)

val convolution = Signal.convolve(NDArray.of(1.0, 2.0, 3.0), NDArray.of(1.0, 2.0))
val smoothed = Signal.movingAverage(data, window = 5)
val window = Signal.hann(64)
val detrended = Signal.detrend(data)
val (frequencies, magnitudes) = Signal.spectralMagnitude(data, sampleRate = 100.0)
```

## 稀疏矩阵

```kotlin
import cn.ac.oac.libs.andas.scipy.sparse.SparseMatrix

val sparse = SparseMatrix.fromDense(denseMatrix)
val product = sparse.multiply(vector)
val transposed = sparse.transpose()
val denseAgain = sparse.toDense()
val fromTriplets = SparseMatrix.fromTriplets(
    rows = 3, cols = 3,
    triplets = listOf(
        SparseMatrix.Triplet(0, 0, 1.0),
        SparseMatrix.Triplet(2, 1, 2.0)
    )
)
```

## 图像处理

```kotlin
import cn.ac.oac.libs.andas.scipy.image.ImageOps

val gray = ImageOps.grayscale(rgb)                 // H x W x 3/4 -> H x W
val blurred = ImageOps.gaussianBlur(gray, sigma = 1.5)
val edges = ImageOps.sobel(gray)
val mask = ImageOps.threshold(gray, threshold = 120.0)
val resized = ImageOps.resizeNearest(gray, 256, 256)
```

## 积分扩展

```kotlin
val adaptive = Integrate.quad({ kotlin.math.sin(it) }, 0.0, Math.PI)
```

## 特殊函数

```kotlin
import cn.ac.oac.libs.andas.scipy.Special

val erf = Special.erf(1.0)
val gamma = Special.gamma(6.0)
val beta = Special.beta(2.0, 3.0)
val logBeta = Special.logBeta(2.0, 3.0)
```

## 分布与检验扩展

```kotlin
val z = Stats.normalInv(0.975)          // 1.96
val corr = Stats.corrcoef(x, y)
val welch = Stats.ttestInd(x, y)
val standardized = Stats.zscore(x)
val uniform = Stats.uniformCdf(0.95)
```
