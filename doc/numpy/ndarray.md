# NumPy 风格 NDArray

`cn.ac.oac.libs.andas.numpy` 提供 Android 可用的 NumPy 子集：一/二维 `float64` 数组、
向量化运算、基础广播、线性代数、随机数与 FFT。JNI 不可用时自动回退 Kotlin。

## 创建与形状

```kotlin
import cn.ac.oac.libs.andas.numpy.NDArray

val a = NDArray.arange(0.0, 6.0).reshape(2, 3)
val b = NDArray.zeros(2, 3)
val c = NDArray.ones(3, 3)
val d = NDArray.linspace(0.0, 1.0, 5)
val e = NDArray.from2D(arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)))
val identity = NDArray.eye(3)
```

| 属性/方法 | 说明 |
| --- | --- |
| `shape` / `ndim` / `size` | 形状、维度、元素数 |
| `reshape(...)` | 改变形状；非连续时自动复制 |
| `transpose()` / `T()` | 二维转置 |
| `row(i)` / `col(j)` | 取行/列（副本） |
| `slice(start, end)` | 按第一维切片 |
| `toDoubleArray()` | 按逻辑顺序复制为一维数组 |

## 运算与广播

```kotlin
val sum = a + b
val scaled = a * 2.0
val squared = a.pow(2.0)
val clipped = a.clip(-1.0, 1.0)

// 2D 与 1D 的行/列广播
val row = NDArray.of(10.0, 20.0, 30.0)
val shifted = a + row
```

支持的逐元素函数：`abs`、`sqrt`、`exp`、`log`、`sin`、`cos`、`pow`、`clip`。

## 归约

```kotlin
val total = a.sum()
val mean = a.mean()
val sd = a.std()          // 样本标准差（ddof=1）
val min = a.min()
val max = a.max()
val columnSums = a.sum(axis = 0)
val rowMeans = a.mean(axis = 1)
```

## 线性代数

```kotlin
import cn.ac.oac.libs.andas.numpy.Linalg

val matrix = NDArray.from2D(arrayOf(doubleArrayOf(4.0, 7.0), doubleArrayOf(2.0, 6.0)))
val determinant = Linalg.det(matrix)
val inverse = Linalg.inv(matrix)
val solution = Linalg.solve(matrix, NDArray.of(1.0, 2.0))
val (slope, intercept) = Linalg.lstsq(x, y).let { it.get(0) to it.get(1) }
val (eigenvalue, eigenvector) = Linalg.powerIteration(matrix)
val product = matrix.matmul(inverse)
```

## FFT 与随机数

```kotlin
import cn.ac.oac.libs.andas.numpy.FFT
import cn.ac.oac.libs.andas.numpy.NumpyRandom

val spectrum = FFT.fft(NDArray.of(1.0, 1.0, 1.0, 1.0))
val magnitudes = spectrum.magnitude()

val random = NumpyRandom(seed = 42)
val normal = random.normal(loc = 0.0, scale = 1.0, size = 1000)
val uniform = random.uniform(0.0, 1.0, size = 1000)
```

长度为 2 的幂时 FFT 使用 JNI radix-2；其他长度回退 DFT。

## 与 Andas DataFrame 转换

```kotlin
import cn.ac.oac.libs.andas.numpy.toNDArray
import cn.ac.oac.libs.andas.numpy.toDataFrame

val array = dataFrame.toNDArray()                 // 默认数值列，null -> NaN
val frame = array.toDataFrame(listOf("a", "b"))   // NaN -> null
val series = array.toSeries("values")
```

## dtype

```kotlin
import cn.ac.oac.libs.andas.numpy.DType

val f32 = NDArray.ofType(DType.FLOAT32, 1.0, 2.0, 3.0)
val i32 = NDArray.fromIntArray(intArrayOf(1, 2, 3, 4), 2, 2)
val i64 = NDArray.fromLongArray(longArrayOf(1L, 2L))
val flags = NDArray.fromBooleanArray(booleanArrayOf(true, false, true), 3)

val exact = i32.getLong(1, 1)     // 精确整数
val converted = i32.astype(DType.FLOAT64)
val ints = converted.toIntArray()
```

| dtype | 存储 | 说明 |
| --- | --- | --- |
| `FLOAT64` | `DoubleArray` | 默认；统计/线性代数主路径 |
| `FLOAT32` | `FloatArray` | 移动端省内存 |
| `INT32` | `IntArray` | 整数索引/计数 |
| `INT64` | `LongArray` | 大整数、时间戳 |
| `BOOLEAN` | `BooleanArray` | 掩码 |

- `get(...)` 统一返回 `Double` 以兼容旧 API；精确整数用 `getLong/getInt`。
- 算术、统计结果提升为 `FLOAT64`；需要目标类型时显式 `astype`。
- 支持任意维度右对齐广播：`(2,3,4)` 与 `(1,1,4)`、标量、同形状均可。
- `transpose`/`slice` 返回视图或副本，写操作通过 `set` 显式进行。
