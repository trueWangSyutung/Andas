package cn.ac.oac.libs.andas.types

import java.time.LocalDateTime

// 定义空值变量 NaN
const val NaN = Double.NaN

/**
 * Anda数据类型枚举，参考pandas的dtype设计
 * 提供了与pandas相似的数据类型系统，便于数据分析操作
 */
enum class AndaTypes(val dtype: String) {
    // 整数类型
    INT8("INT8"),
    INT16("INT16"),
    INT32("INT32"),
    INT64("INT64"),
    
    // 浮点数类型
    FLOAT32("FLOAT32"),
    FLOAT64("FLOAT64"),
    
    // 字符串类型
    STRING("STRING"),
    // 布尔类型
    BOOL("BOOL"),

    OBJECTS("OBJECT");



    companion object {
        /**
         * 根据Java/Kotlin类型获取对应的AndaTypes
         */
        fun fromClass(clazz: Class<*>): AndaTypes {
            return when (clazz) {
                Byte::class.java, java.lang.Byte::class.java -> INT8
                Short::class.java, java.lang.Short::class.java -> INT16
                Integer::class.java, Integer::class.java -> INT32
                Long::class.java, java.lang.Long::class.java -> INT64
                Float::class.java, java.lang.Float::class.java -> FLOAT32
                Double::class.java, java.lang.Double::class.java -> FLOAT64
                Boolean::class.java, java.lang.Boolean::class.java -> BOOL
                String::class.java, java.lang.String::class.java -> STRING
                else -> OBJECTS
            }
        }
    }
}

/**
 * 将AndaTypes转换为对应的Java类型
 */
fun AndaTypes.toJavaType(): Class<*> {
    return when (this) {
        AndaTypes.INT8 -> Byte::class.java
        AndaTypes.INT16 -> Short::class.java
        AndaTypes.INT32 -> Integer::class.java
        AndaTypes.INT64 -> Long::class.java
        AndaTypes.FLOAT32 -> Float::class.java
        AndaTypes.FLOAT64 -> Double::class.java
        AndaTypes.BOOL -> Boolean::class.java
        AndaTypes.STRING ->  String::class.java
        AndaTypes.OBJECTS -> Any::class.java
    }
}

/**
 * 将JSON对象转换为指定类型
 */
fun <T> jsonToObject(json: Any?): T? {
    if (json == null) {
        return null
    }
    @Suppress("UNCHECKED_CAST")
    return json as T
}

/**
 * 获取类型名称
 */
fun AndaTypes.typeName(): String {
    return this.dtype
}