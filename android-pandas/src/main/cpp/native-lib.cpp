#include <jni.h>
#include <android/log.h>
#include <cmath>
#include <vector>
#include <algorithm>
#include <chrono>

#define LOG_TAG "AndasNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 性能测试工具
extern "C" JNIEXPORT jlong JNICALL
Java_cn_ac_oac_libs_andas_core_NativeMath_00024Benchmark_measureOperationTime(
    JNIEnv* env,
    jobject /* this */,
    jint operationType,
    jint dataSize
) {
    auto start = std::chrono::high_resolution_clock::now();
    
    // 根据操作类型执行不同的测试
    switch (operationType) {
        case 1: { // 数组创建和初始化
            std::vector<double> data(dataSize);
            for (int i = 0; i < dataSize; i++) {
                data[i] = i * 2.0;
            }
            break;
        }
        case 2: { // 数学运算
            std::vector<double> data(dataSize);
            for (int i = 0; i < dataSize; i++) {
                data[i] = i * 2.0;
            }
            for (int i = 0; i < dataSize; i++) {
                data[i] = data[i] * 1.5;
            }
            break;
        }
        case 3: { // 统计计算
            std::vector<double> data(dataSize);
            double sum = 0.0;
            for (int i = 0; i < dataSize; i++) {
                data[i] = i;
                sum += data[i];
            }
            double mean = sum / dataSize;
            break;
        }
        case 4: { // 过滤操作
            std::vector<int> indices;
            for (int i = 0; i < dataSize; i++) {
                if (i > dataSize / 2) {
                    indices.push_back(i);
                }
            }
            break;
        }
    }
    
    auto end = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::microseconds>(end - start);
    
    return duration.count();
}

// 线程安全的批量处理
extern "C" JNIEXPORT jdoubleArray JNICALL
Java_cn_ac_oac_libs_andas_core_NativeBatch_processBatch(
    JNIEnv* env,
    jobject /* this */,
    jdoubleArray array,
    jint batchSize
) {
    jsize length = env->GetArrayLength(array);
    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);
    
    jdoubleArray result = env->NewDoubleArray(length);
    jdouble* resultElements = env->GetDoubleArrayElements(result, nullptr);
    
    // 批量计算 - 模拟复杂的数据处理操作
    for (int i = 0; i < length; i++) {
        // 模拟复杂计算：sin(x) + cos(x) * 2.0
        resultElements[i] = std::sin(elements[i]) + std::cos(elements[i]) * 2.0;
    }
    
    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);
    env->ReleaseDoubleArrayElements(result, resultElements, 0);
    
    return result;
}
