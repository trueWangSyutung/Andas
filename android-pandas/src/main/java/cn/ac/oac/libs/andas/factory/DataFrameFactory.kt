package cn.ac.oac.libs.andas.factory

import cn.ac.oac.libs.andas.entity.DataFrame

// 工厂函数
fun andasDataFrame(columnsData: Map<String, List<Any?>>): DataFrame {
    return DataFrame(columnsData)
}

fun andasDataFrameFromRows(rowsData: List<Map<String, Any?>>): DataFrame {
    return DataFrame(rowsData)
}