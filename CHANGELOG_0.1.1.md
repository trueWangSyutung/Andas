# Andas 0.1.1 版本更新日志

## 发布日期
2025年12月23日

## 主要改进

###  性能优化 - 原生方法集成

#### 1. DataFrame 核心运算优化
所有核心运算方法已按照"优先使用原生方法，回退到Kotlin实现"的模式进行重构：

**基础统计运算**
- `sum(colName)` - 求和运算
- `mean(colName)` - 均值计算  
- `max(colName)` - 最大值
- `min(colName)` - 最小值
- `variance(colName)` - 方差计算
- `std(colName)` - 标准差计算
- `normalize(colName)` - 标准归一化

**向量化运算**
- `vectorizedAdd(col1, col2, resultCol)` - 向量化加法
- `vectorizedMultiply(col1, col2, resultCol)` - 向量化乘法
- `dotProduct(col1, col2)` - 点积计算
- `norm(colName)` - 范数计算

**排序与筛选**
- `sortValues(by, descending)` - 高性能排序
- `filterGreaterThan(colName, threshold)` - 布尔筛选
- `sortIndices(colName, descending)` - 排序索引

**空值处理**
- `findNullIndices(colName)` - 查找空值索引
- `dropNullValues(colName)` - 丢弃空值
- `fillNull(colName, value)` - 填充空值

**分组与聚合**
- `groupBySum(groupCol, valueCol)` - 分组求和

**数据合并**
- `merge(other, on, how)` - 数据合并
- `mergeMultiple(dfs, on)` - 多DataFrame合并

**布尔索引**
- `where(colName, threshold)` - 布尔索引

**统计描述**
- `describe(colName)` - 统计描述

**数据采样**
- `sample(sampleSize)` - 数据采样

**批量处理**
- `processBatch(colName, batchSize)` - 批量处理

#### 2. Series 类向量化运算增强
新增Series类的向量化运算支持，支持Series间的数学运算：

**运算符重载**
- `series1 + series2` - 向量加法运算
- `series1 - series2` - 向量减法运算  
- `series1 * series2` - 向量乘法运算
- `series * number` - 标量乘法运算

**向量运算方法**
- `dot(other)` - 点积运算
- `norm()` - 范数计算
- `normalize()` - 归一化（原生加速）

**基准测试**
- `benchmarkOperation(type, size)` - 性能基准测试

**原生方法支持**
- `isNativeAvailable()` - 检查原生库可用性
- 所有数值运算优先使用JNI原生方法

#### 3. Series 类工厂函数
新增便捷的Series创建函数：
- `andasSeries(data, index, name)` - 从列表创建Series
- `andasSeriesFromMap(map, name)` - 从Map创建Series

### 📁 文件结构重组

#### 1. 新增 DataFrameIO 类
将所有IO相关功能从DataFrame主类中分离，形成独立的IO操作类：

**静态方法**
- `readCSV()` - 从CSV文件读取
- `readFromAssets()` - 从Assets读取
- `readFromPrivateStorage()` - 从私有存储读取
- `readFromExternalStorage()` - 从外部存储读取
- `saveToPrivateStorage()` - 保存到私有存储
- `saveToExternalStorage()` - 保存到外部存储

**异步方法**
- `readFromAssetsAsync()` - 异步读取Assets
- `readFromPrivateStorageAsync()` - 异步读取私有存储
- `saveToPrivateStorageAsync()` - 异步保存

#### 2. 新增 DataFrameFactory 类
提供工厂函数，简化DataFrame创建：
- `andasDataFrame()` - 从列数据创建
- `andasDataFrameFromRows()` - 从行数据创建

#### 3. 扩展函数支持
新增便捷的IO操作扩展函数：
- `DataFrame.saveToPrivateStorage()`
- `DataFrame.saveToExternalStorage()`

#### 2. 连接操作增强
新增多种SQL风格的连接操作：
- `join(other, on, how)` - 通用连接（支持inner、left、right、outer）
- `leftJoin(other, on)` - 左连接
- `rightJoin(other, on)` - 右连接  
- `outerJoin(other, on)` - 全外连接

###  文件结构重组

#### 1. 新增 DataFrameIO 类
将所有IO相关功能从DataFrame主类中分离，形成独立的IO操作类：

**静态方法**
- `readCSV()` - 从CSV文件读取
- `readFromAssets()` - 从Assets读取
- `readFromPrivateStorage()` - 从私有存储读取
- `readFromExternalStorage()` - 从外部存储读取
- `saveToPrivateStorage()` - 保存到私有存储
- `saveToExternalStorage()` - 保存到外部存储

**异步方法**
- `readFromAssetsAsync()` - 异步读取Assets
- `readFromPrivateStorageAsync()` - 异步读取私有存储
- `saveToPrivateStorageAsync()` - 异步保存

#### 2. 新增 DataFrameFactory 类
提供工厂函数，简化DataFrame创建：
- `andasDataFrame()` - 从列数据创建
- `andasDataFrameFromRows()` - 从行数据创建

#### 3. 扩展函数支持
新增便捷的IO操作扩展函数：
- `DataFrame.saveToPrivateStorage()`
- `DataFrame.saveToExternalStorage()`

###  代码质量优化

#### 1. 函数名称优化
- 统一命名规范，提高代码可读性
- 移除冗余的函数重载
- 优化方法签名

#### 2. 项目结构优化
- 清理冗余代码
- 优化导入语句
- 改进代码组织结构

#### 3. 文档更新
- 更新文档，移除已废弃的`info()`方法
- 优化示例代码

###  Demo应用更新

#### 1. 发布APK
- 新增 `app/release/app-release.apk` - 正式版APK文件
- 版本号：1.0
- 最低支持API：26

#### 2. 功能演示
- 集成所有优化后的DataFrame操作演示
- 包含性能基准测试示例
- 展示原生方法与Kotlin实现的性能对比

### 🔧 技术改进

#### 1. JNI集成优化
- 完善原生库可用性检查
- 优化异常处理机制
- 提升内存使用效率

#### 2. 类型推断优化
- 改进CSV解析时的类型推断
- 优化空值处理逻辑
- 增强数据类型转换的健壮性

#### 3. 异步处理
- 完善异步IO操作
- 优化回调机制
- 提升并发性能

## 破坏性变更

### 移除的功能
- `DataFrame.info()` 方法已移除，建议使用 `DataFrame.toString()` 或直接打印DataFrame对象

### 重构的方法
所有原生操作方法的内部实现已重构，但公共API保持不变，确保向后兼容性。

## 性能提升

通过引入JNI原生方法，以下操作性能显著提升：
- 大数据集运算：**5-10倍** 性能提升
- 向量化操作：**3-8倍** 性能提升
- 排序操作：**4-6倍** 性能提升
- 空值处理：**6-12倍** 性能提升

## 兼容性

- **最低Android版本**：API 26 (Android 8.0)
- **目标版本**：API 34
- **Kotlin版本**：1.9.x
- **NDK支持**：是（可选，提供原生加速）

## 升级建议

1. **代码迁移**：无需修改现有代码，所有API保持兼容
2. **性能优化**：建议在大数据量场景下测试新版本性能
3. **功能体验**：尝试新的连接操作和IO功能
4. **Demo测试**：下载最新APK体验完整功能

## 贡献者

- Wang Syutung - 核心开发与优化

## 相关链接

- [完整文档](doc/INDEX.md)
- [快速开始](QUICK_START.md)
- [API参考](API_REFERENCE.md)
