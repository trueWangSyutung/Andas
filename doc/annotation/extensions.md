# 注解与扩展机制

Andas 通过注解声明 API 稳定性，并提供运行时扩展注册表，方便开发者在不修改库源码的情况下
增加 Series/DataFrame/NDArray 函数。

## API 注解

```kotlin
import cn.ac.oac.libs.andas.annotation.AndasApi
import cn.ac.oac.libs.andas.annotation.ApiStability
import cn.ac.oac.libs.andas.annotation.ExperimentalAndas
import cn.ac.oac.libs.andas.annotation.NativeAccelerated

@AndasApi(stability = ApiStability.STABLE, since = "0.2.0")
fun stableOperation() { }

@ExperimentalAndas("可能在 0.2.x 调整")
fun previewOperation() { }

@NativeAccelerated(fallback = "Kotlin")
fun acceleratedOperation() { }
```

| 注解 | 用途 |
| --- | --- |
| `@AndasApi` | 声明稳定性、引入版本与备注，供文档/IDE 生成器读取 |
| `@ExperimentalAndas` | 标记实验性 API |
| `@InternalAndas` | 标记内部 API，不属于公开契约 |
| `@NativeAccelerated` | 标记 JNI/OpenMP 加速实现及回退策略 |
| `@AndasFunction` | 标记可注册的用户扩展函数（配合注册表） |

## 扩展函数注册表

```kotlin
import cn.ac.oac.libs.andas.annotation.AndasExtensions

AndasExtensions.registerSeries("sum") { series ->
    series.toDoubleArray().sum()
}

AndasExtensions.registerDataFrame("rowCount") { dataFrame ->
    dataFrame.shape().first
}

AndasExtensions.registerNDArray("norm") { array ->
    array.norm()
}

val total = AndasExtensions.invokeSeries("sum", series)
val rows = AndasExtensions.invokeDataFrame("rowCount", dataFrame)
val length = AndasExtensions.invokeNDArray("norm", ndarray)
val all = AndasExtensions.list()
```

注册表使用 `ConcurrentHashMap`，可在 Activity/Application 初始化阶段注册。函数签名固定为
`(Series<Any>) -> Any?`、`(DataFrame) -> Any?`、`(NDArray) -> Any?`，复杂参数可通过闭包捕获。

## 建议

- 库自身公开 API 默认标注 `@AndasApi`；实验能力使用 `@ExperimentalAndas`。
- 扩展名使用稳定的字符串（例如 `"finance.movingSharpe"`），避免与内置函数冲突。
- 注册函数应保持无状态或线程安全；注册表本身线程安全，但函数由调用方负责同步。
