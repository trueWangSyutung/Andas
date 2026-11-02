package cn.ac.oac.libs.andas.annotation

import cn.ac.oac.libs.andas.entity.DataFrame
import cn.ac.oac.libs.andas.entity.Series
import cn.ac.oac.libs.andas.numpy.NDArray
import java.util.concurrent.ConcurrentHashMap

/** API 稳定性级别，供库使用者和 IDE/文档生成器判断兼容性。 */
enum class ApiStability { STABLE, EXPERIMENTAL, DEPRECATED, INTERNAL }

/**
 * 标记公开 API 的稳定性与引入版本。
 *
 * 示例：
 * ```kotlin
 * @AndasApi(stability = ApiStability.EXPERIMENTAL, since = "0.2.0")
 * fun myFeature() { ... }
 * ```
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AndasApi(
    val stability: ApiStability = ApiStability.STABLE,
    val since: String = "0.1.5",
    val note: String = ""
)

/** 实验性 API：可能在小版本中调整，使用方需自行承担兼容性风险。 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ExperimentalAndas(val note: String = "")

/** 内部 API：不属于公开契约，可能随时修改或删除。 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class InternalAndas

/** 标记由 JNI/OpenMP 加速的实现；不可用时自动回退到 Kotlin。 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class NativeAccelerated(val fallback: String = "Kotlin")

/**
 * 用户扩展函数标记。
 *
 * 该注解本身不执行反射发现；配合 [AndasExtensions.registerSeries] 等注册 API 使用，
 * 便于在运行时/插件系统中按名称调用用户函数。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AndasFunction(
    val name: String = "",
    val description: String = "",
    val version: String = "0.1.5"
)

enum class ExtensionKind { SERIES, DATAFRAME, NDARRAY }

data class ExtensionDescriptor(
    val kind: ExtensionKind,
    val name: String,
    val description: String,
    val version: String
)

/**
 * 扩展函数注册表：让宿主应用把自定义的 Series/DataFrame/NDArray 函数暴露给脚本、
 * 低代码配置或插件系统，而不需要修改库源码。
 */
object AndasExtensions {
    private data class Entry(
        val descriptor: ExtensionDescriptor,
        val series: ((Series<Any>) -> Any?)? = null,
        val dataFrame: ((DataFrame) -> Any?)? = null,
        val ndarray: ((NDArray) -> Any?)? = null
    )

    private val registry = ConcurrentHashMap<String, Entry>()

    fun registerSeries(
        name: String,
        description: String = "",
        version: String = "0.1.5",
        function: (Series<Any>) -> Any?
    ) {
        registry[key(ExtensionKind.SERIES, name)] = Entry(
            ExtensionDescriptor(ExtensionKind.SERIES, name, description, version),
            series = function
        )
    }

    fun registerDataFrame(
        name: String,
        description: String = "",
        version: String = "0.1.5",
        function: (DataFrame) -> Any?
    ) {
        registry[key(ExtensionKind.DATAFRAME, name)] = Entry(
            ExtensionDescriptor(ExtensionKind.DATAFRAME, name, description, version),
            dataFrame = function
        )
    }

    fun registerNDArray(
        name: String,
        description: String = "",
        version: String = "0.1.5",
        function: (NDArray) -> Any?
    ) {
        registry[key(ExtensionKind.NDARRAY, name)] = Entry(
            ExtensionDescriptor(ExtensionKind.NDARRAY, name, description, version),
            ndarray = function
        )
    }

    fun invokeSeries(name: String, series: Series<*>): Any? {
        @Suppress("UNCHECKED_CAST")
        val function = registry[key(ExtensionKind.SERIES, name)]?.series
            ?: throw NoSuchElementException("未注册 Series 扩展: $name")
        return function(series as Series<Any>)
    }

    fun invokeDataFrame(name: String, dataFrame: DataFrame): Any? =
        registry[key(ExtensionKind.DATAFRAME, name)]?.dataFrame?.invoke(dataFrame)
            ?: throw NoSuchElementException("未注册 DataFrame 扩展: $name")

    fun invokeNDArray(name: String, ndarray: NDArray): Any? =
        registry[key(ExtensionKind.NDARRAY, name)]?.ndarray?.invoke(ndarray)
            ?: throw NoSuchElementException("未注册 NDArray 扩展: $name")

    fun list(): List<ExtensionDescriptor> = registry.values.map { it.descriptor }.sortedBy { it.name }

    fun clear() = registry.clear()

    private fun key(kind: ExtensionKind, name: String): String = "${kind.name}:$name"
}
