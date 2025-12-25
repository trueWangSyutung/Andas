#include <jni.h>
#include <android/log.h>
#include <cmath>
#include <vector>
#include <algorithm>
#include <unordered_map>
#include <string>
#include <omp.h>

#define LOG_TAG "AndasData"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#include "double_harsh.h"
// 数据处理 优化实现

extern "C" JNIEXPORT jintArray JNICALL
Java_cn_ac_oac_libs_andas_core_NativeData_findNullIndices(
    JNIEnv* env,
    jobject /* this */,
    jdoubleArray array
) {
    jsize length = env->GetArrayLength(array);
    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);
    
    std::vector<int> nullIndices;
    
    // 并行查找空值索引
    #pragma omp parallel
    {
        std::vector<int> local_nullIndices;
        
        #pragma omp for nowait
        for (int i = 0; i < length; i++) {
            if (std::isnan(elements[i])) {
                local_nullIndices.push_back(i);
            }
        }
        
        #pragma omp critical
        {
            nullIndices.insert(nullIndices.end(), 
                               local_nullIndices.begin(), 
                               local_nullIndices.end());
        }
    }
    
    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);
    
    jintArray result = env->NewIntArray(nullIndices.size());
    env->SetIntArrayRegion(result, 0, nullIndices.size(), nullIndices.data());
    
    return result;
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_cn_ac_oac_libs_andas_core_NativeData_dropNullValues(
    JNIEnv* env,
    jobject /* this */,
    jdoubleArray array
) {
    jsize length = env->GetArrayLength(array);
    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);
    
    std::vector<double> nonNullValues;
    nonNullValues.reserve(length);
    
    // 并行查找非空值
    #pragma omp parallel
    {
        std::vector<double> local_nonNullValues;
        
        #pragma omp for nowait
        for (int i = 0; i < length; i++) {
            if (!std::isnan(elements[i])) {
                local_nonNullValues.push_back(elements[i]);
            }
        }
        
        #pragma omp critical
        {
            nonNullValues.insert(nonNullValues.end(), 
                                 local_nonNullValues.begin(), 
                                 local_nonNullValues.end());
        }
    }
    
    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);
    
    jdoubleArray result = env->NewDoubleArray(nonNullValues.size());
    env->SetDoubleArrayRegion(result, 0, nonNullValues.size(), nonNullValues.data());
    
    return result;
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_cn_ac_oac_libs_andas_core_NativeData_fillNullWithConstant(
    JNIEnv* env,
    jobject /* this */,
    jdoubleArray array,
    jdouble value
) {
    jsize length = env->GetArrayLength(array);
    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);
    
    jdoubleArray result = env->NewDoubleArray(length);
    jdouble* resultElements = env->GetDoubleArrayElements(result, nullptr);
    
    #pragma omp parallel for
    for (int i = 0; i < length; i++) {
        resultElements[i] = std::isnan(elements[i]) ? value : elements[i];
    }
    
    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);
    env->ReleaseDoubleArrayElements(result, resultElements, 0);
    
    return result;
}

// 分组聚合优化
extern "C" JNIEXPORT jobject JNICALL
Java_cn_ac_oac_libs_andas_core_NativeData_groupBySum(
    JNIEnv* env,
    jobject /* this */,
    jdoubleArray values,
    jintArray groups
) {
    jsize length = env->GetArrayLength(values);
    jdouble* valueElements = env->GetDoubleArrayElements(values, nullptr);
    jint* groupElements = env->GetIntArrayElements(groups, nullptr);
    
    // 使用哈希表存储分组结果
    std::unordered_map<int, double> groupSums;
    
    for (int i = 0; i < length; i++) {
        int group = groupElements[i];
        groupSums[group] += valueElements[i];
    }
    
    env->ReleaseDoubleArrayElements(values, valueElements, JNI_ABORT);
    env->ReleaseIntArrayElements(groups, groupElements, JNI_ABORT);
    
    // 创建返回结果
    jclass mapClass = env->FindClass("java/util/HashMap");
    jmethodID mapConstructor = env->GetMethodID(mapClass, "<init>", "()V");
    jmethodID putMethod = env->GetMethodID(mapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    
    jobject result = env->NewObject(mapClass, mapConstructor);
    
    for (const auto& pair : groupSums) {
        jstring key = env->NewStringUTF(std::to_string(pair.first).c_str());
        jdouble value = pair.second;
        jobject valueObj = env->NewObject(env->FindClass("java/lang/Double"), 
                                         env->GetMethodID(env->FindClass("java/lang/Double"), "<init>", "(D)V"), 
                                         value);
        env->CallObjectMethod(result, putMethod, key, valueObj);
    }
    
    return result;
}

// 数据排序优化
extern "C" JNIEXPORT jintArray JNICALL
Java_cn_ac_oac_libs_andas_core_NativeData_sortIndices(
    JNIEnv* env,
    jobject /* this */,
    jdoubleArray array,
    jboolean descending
) {
    jsize length = env->GetArrayLength(array);
    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);
    
    std::vector<int> indices(length);
    #pragma omp parallel for
    for (int i = 0; i < length; i++) {
        indices[i] = i;
    }
    
    if (descending) {
        std::sort(indices.begin(), indices.end(),
            [&](int a, int b) { return elements[a] > elements[b]; });
    } else {
        std::sort(indices.begin(), indices.end(),
            [&](int a, int b) { return elements[a] < elements[b]; });
    }
    
    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);
    
    jintArray result = env->NewIntArray(length);
    env->SetIntArrayRegion(result, 0, length, indices.data());
    
    return result;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_cn_ac_oac_libs_andas_core_NativeData_mergeIndices(
        JNIEnv* env,
        jobject /* this */,
        jdoubleArray left,
        jdoubleArray right
) {
    jsize leftLength = env->GetArrayLength(left);
    jsize rightLength = env->GetArrayLength(right);

    jdouble* leftElements = env->GetDoubleArrayElements(left, nullptr);
    jdouble* rightElements = env->GetDoubleArrayElements(right, nullptr);

    // 使用自定义哈希和相等比较函数的哈希表
    std::unordered_map<double, std::vector<int>, DoubleHash, DoubleEqual> rightValueMap;

    // 构建右侧数组的值到索引列表的映射 - 并行化
#pragma omp parallel for
    for (int j = 0; j < rightLength; j++) {
#pragma omp critical
        {
            rightValueMap[rightElements[j]].push_back(j);
        }
    }

    // 预分配结果向量大小，减少内存重新分配
    std::vector<int> mergedIndices;

    // 并行处理左侧数组
    #pragma omp parallel
    {
        std::vector<int> localMergedIndices;
        localMergedIndices.reserve(leftLength); // 预估大小

        #pragma omp for nowait
        for (int i = 0; i < leftLength; i++) {
            auto it = rightValueMap.find(leftElements[i]);
            if (it != rightValueMap.end()) {
                for (int rightIdx : it->second) {
                    localMergedIndices.push_back(i);
                    localMergedIndices.push_back(rightIdx);
                }
            }
        }

        // 合并本地结果到全局向量
#pragma omp critical
        {
            mergedIndices.insert(mergedIndices.end(),
                                 localMergedIndices.begin(),
                                 localMergedIndices.end());
        }
    }

    env->ReleaseDoubleArrayElements(left, leftElements, JNI_ABORT);
    env->ReleaseDoubleArrayElements(right, rightElements, JNI_ABORT);

    jintArray result = env->NewIntArray(mergedIndices.size());
    env->SetIntArrayRegion(result, 0, mergedIndices.size(), mergedIndices.data());

    return result;
}



// 布尔索引优化
extern "C" JNIEXPORT jintArray JNICALL
Java_cn_ac_oac_libs_andas_core_NativeData_where(
    JNIEnv* env,
    jobject /* this */,
    jbooleanArray mask
) {
    jsize length = env->GetArrayLength(mask);
    jboolean* maskElements = env->GetBooleanArrayElements(mask, nullptr);
    
    std::vector<int> indices;
    indices.reserve(length);
    
    // 并行查找true值的索引
    #pragma omp parallel
    {
        std::vector<int> local_indices;
        
        #pragma omp for nowait
        for (int i = 0; i < length; i++) {
            if (maskElements[i]) {
                local_indices.push_back(i);
            }
        }
        
        #pragma omp critical
        {
            indices.insert(indices.end(), 
                             local_indices.begin(), 
                             local_indices.end());
        }
    }
    
    env->ReleaseBooleanArrayElements(mask, maskElements, JNI_ABORT);
    
    jintArray result = env->NewIntArray(indices.size());
    env->SetIntArrayRegion(result, 0, indices.size(), indices.data());
    
    return result;
}

// 数据统计优化
extern "C" JNIEXPORT jdoubleArray JNICALL
Java_cn_ac_oac_libs_andas_core_NativeData_describe(
    JNIEnv* env,
    jobject /* this */,
    jdoubleArray array
) {
    jsize length = env->GetArrayLength(array);
    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);
    
    if (length == 0) {
        return env->NewDoubleArray(0);
    }
    
    // 并行计算统计量
    double sum = 0.0;
    double min_val = std::numeric_limits<double>::max();
    double max_val = std::numeric_limits<double>::lowest();
    int count = 0;
    
    #pragma omp parallel
    {
        double local_sum = 0.0;
        double local_min = std::numeric_limits<double>::max();
        double local_max = std::numeric_limits<double>::lowest();
        int local_count = 0;
        
        #pragma omp for nowait
        for (int i = 0; i < length; i++) {
            double val = elements[i];
            if (!std::isnan(val)) {
                local_sum += val;
                local_count++;
                if (val < local_min) local_min = val;
                if (val > local_max) local_max = val;
            }
        }
        
        #pragma omp critical
        {
            sum += local_sum;
            count += local_count;
            if (local_min < min_val) min_val = local_min;
            if (local_max > max_val) max_val = local_max;
        }
    }
    
    double mean = count > 0 ? sum / count : 0.0;
    
    // 并行计算方差
    double variance = 0.0;
    #pragma omp parallel for reduction(+:variance)
    for (int i = 0; i < length; i++) {
        double val = elements[i];
        if (!std::isnan(val)) {
            variance += (val - mean) * (val - mean);
        }
    }
    variance = count > 1 ? variance / (count - 1) : 0.0;
    double std = std::sqrt(variance);
    
    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);
    
    // 返回: [count, mean, std, min, max]
    jdoubleArray result = env->NewDoubleArray(5);
    jdouble resultElements[5] = {static_cast<double>(count), mean, std, min_val, max_val};
    env->SetDoubleArrayRegion(result, 0, 5, resultElements);
    
    return result;
}

// 数据采样优化
extern "C" JNIEXPORT jdoubleArray JNICALL
Java_cn_ac_oac_libs_andas_core_NativeData_sample(
    JNIEnv* env,
    jobject /* this */,
    jdoubleArray array,
    jint sampleSize
) {
    jsize length = env->GetArrayLength(array);
    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);
    
    if (sampleSize >= length) {
        // 返回副本
        jdoubleArray result = env->NewDoubleArray(length);
        env->SetDoubleArrayRegion(result, 0, length, elements);
        env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);
        return result;
    }
    
    // 简单的随机采样（线性同余生成器）
    std::vector<int> indices(length);
    #pragma omp parallel for
    for (int i = 0; i < length; i++) {
        indices[i] = i;
    }
    
    // 洗牌算法
    for (int i = length - 1; i > 0; i--) {
        int j = rand() % (i + 1);
        std::swap(indices[i], indices[j]);
    }
    
    jdoubleArray result = env->NewDoubleArray(sampleSize);
    for (int i = 0; i < sampleSize; i++) {
        double value = elements[indices[i]];
        env->SetDoubleArrayRegion(result, i, 1, &value);
    }
    
    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);
    
    return result;
}
