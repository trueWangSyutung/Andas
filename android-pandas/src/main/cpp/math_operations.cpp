#include <jni.h>
#include <android/log.h>
#include <cmath>
#include <vector>
#include <algorithm>
#include <functional>
#include <limits>

#define LOG_TAG "AndasMath"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// 高性能数学运算库
extern "C" JNIEXPORT jdoubleArray JNICALL
Java_cn_ac_oac_libs_andas_core_NativeMath_multiplyDoubleArray(
        JNIEnv* env,
        jobject /* this */,
        jdoubleArray array,
        jdouble multiplier
) {
    jsize length = env->GetArrayLength(array);
    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);

    jdoubleArray result = env->NewDoubleArray(length);
    jdouble* resultElements = env->GetDoubleArrayElements(result, nullptr);

    for (int i = 0; i < length; i++) {
        resultElements[i] = elements[i] * multiplier;
    }

    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);
    env->ReleaseDoubleArrayElements(result, resultElements, 0);

    return result;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_cn_ac_oac_libs_andas_core_NativeMath_sumDoubleArray(
        JNIEnv* env,
        jobject /* this */,
        jdoubleArray array
) {
    jsize length = env->GetArrayLength(array);
    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);

    double sum = 0.0;
    for (int i = 0; i < length; i++) {
        sum += elements[i];
    }

    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);
    return sum;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_cn_ac_oac_libs_andas_core_NativeMath_meanDoubleArray(
        JNIEnv* env,
        jobject /* this */,
        jdoubleArray array
) {
    jsize length = env->GetArrayLength(array);
    if (length == 0) return 0.0;

    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);

    double sum = 0.0;
    int count = 0;
    for (int i = 0; i < length; i++) {
        if (!std::isnan(elements[i])) {
            sum += elements[i];
            count++;
        }
    }

    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);
    return count > 0 ? sum / count : 0.0;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_cn_ac_oac_libs_andas_core_NativeMath_maxDoubleArray(
        JNIEnv* env,
        jobject /* this */,
        jdoubleArray array
) {
    jsize length = env->GetArrayLength(array);
    if (length == 0) return std::numeric_limits<double>::quiet_NaN();

    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);

    double max = std::numeric_limits<double>::lowest();
    bool foundValid = false;

    for (int i = 0; i < length; i++) {
        if (!std::isnan(elements[i])) {
            if (!foundValid || elements[i] > max) {
                max = elements[i];
                foundValid = true;
            }
        }
    }

    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);
    return foundValid ? max : std::numeric_limits<double>::quiet_NaN();
}

extern "C" JNIEXPORT jdouble JNICALL
Java_cn_ac_oac_libs_andas_core_NativeMath_minDoubleArray(
        JNIEnv* env,
        jobject /* this */,
        jdoubleArray array
) {
    jsize length = env->GetArrayLength(array);
    if (length == 0) return std::numeric_limits<double>::quiet_NaN();

    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);

    double min = std::numeric_limits<double>::max();
    bool foundValid = false;

    for (int i = 0; i < length; i++) {
        if (!std::isnan(elements[i])) {
            if (!foundValid || elements[i] < min) {
                min = elements[i];
                foundValid = true;
            }
        }
    }

    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);
    return foundValid ? min : std::numeric_limits<double>::quiet_NaN();
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_cn_ac_oac_libs_andas_core_NativeMath_vectorizedAdd(
        JNIEnv* env,
        jobject /* this */,
        jdoubleArray a,
        jdoubleArray b
) {
    jsize length = env->GetArrayLength(a);
    if (length != env->GetArrayLength(b)) {
        return nullptr;
    }

    jdouble* elementsA = env->GetDoubleArrayElements(a, nullptr);
    jdouble* elementsB = env->GetDoubleArrayElements(b, nullptr);

    jdoubleArray result = env->NewDoubleArray(length);
    jdouble* resultElements = env->GetDoubleArrayElements(result, nullptr);

    // 向量化加法
    for (int i = 0; i < length; i++) {
        resultElements[i] = elementsA[i] + elementsB[i];
    }

    env->ReleaseDoubleArrayElements(a, elementsA, JNI_ABORT);
    env->ReleaseDoubleArrayElements(b, elementsB, JNI_ABORT);
    env->ReleaseDoubleArrayElements(result, resultElements, 0);

    return result;
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_cn_ac_oac_libs_andas_core_NativeMath_vectorizedMultiply(
        JNIEnv* env,
        jobject /* this */,
        jdoubleArray a,
        jdoubleArray b
) {
    jsize length = env->GetArrayLength(a);
    if (length != env->GetArrayLength(b)) {
        return nullptr;
    }

    jdouble* elementsA = env->GetDoubleArrayElements(a, nullptr);
    jdouble* elementsB = env->GetDoubleArrayElements(b, nullptr);

    jdoubleArray result = env->NewDoubleArray(length);
    jdouble* resultElements = env->GetDoubleArrayElements(result, nullptr);

    // 向量化乘法
    for (int i = 0; i < length; i++) {
        resultElements[i] = elementsA[i] * elementsB[i];
    }

    env->ReleaseDoubleArrayElements(a, elementsA, JNI_ABORT);
    env->ReleaseDoubleArrayElements(b, elementsB, JNI_ABORT);
    env->ReleaseDoubleArrayElements(result, resultElements, 0);

    return result;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_cn_ac_oac_libs_andas_core_NativeMath_dotProduct(
        JNIEnv* env,
        jobject /* this */,
        jdoubleArray a,
        jdoubleArray b
) {
    jsize length = env->GetArrayLength(a);
    if (length != env->GetArrayLength(b)) {
        return 0.0;
    }

    jdouble* elementsA = env->GetDoubleArrayElements(a, nullptr);
    jdouble* elementsB = env->GetDoubleArrayElements(b, nullptr);

    double dot = 0.0;
    for (int i = 0; i < length; i++) {
        dot += elementsA[i] * elementsB[i];
    }

    env->ReleaseDoubleArrayElements(a, elementsA, JNI_ABORT);
    env->ReleaseDoubleArrayElements(b, elementsB, JNI_ABORT);

    return dot;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_cn_ac_oac_libs_andas_core_NativeMath_norm(
        JNIEnv* env,
        jobject /* this */,
        jdoubleArray array
) {
    jsize length = env->GetArrayLength(array);
    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);

    double sumSq = 0.0;
    for (int i = 0; i < length; i++) {
        sumSq += elements[i] * elements[i];
    }

    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);
    return std::sqrt(sumSq);
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_cn_ac_oac_libs_andas_core_NativeMath_normalize(
        JNIEnv* env,
        jobject /* this */,
        jdoubleArray array
) {
    jsize length = env->GetArrayLength(array);
    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);

    // 计算均值和标准差
    double sum = 0.0;
    double sumSq = 0.0;

    for (int i = 0; i < length; i++) {
        sum += elements[i];
        sumSq += elements[i] * elements[i];
    }

    double mean = sum / length;
    double variance = (sumSq / length) - (mean * mean);
    double std = std::sqrt(std::max(variance, 0.0));

    // 归一化
    jdoubleArray result = env->NewDoubleArray(length);
    jdouble* resultElements = env->GetDoubleArrayElements(result, nullptr);

    for (int i = 0; i < length; i++) {
        resultElements[i] = std > 0 ? (elements[i] - mean) / std : 0.0;
    }

    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);
    env->ReleaseDoubleArrayElements(result, resultElements, 0);

    return result;
}

// 统计函数
extern "C" JNIEXPORT jdouble JNICALL
Java_cn_ac_oac_libs_andas_core_NativeMath_variance(
        JNIEnv* env,
        jobject /* this */,
        jdoubleArray array
) {
    jsize length = env->GetArrayLength(array);
    if (length <= 1) return 0.0;

    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);

    double sum = 0.0;
    double sumSq = 0.0;

    for (int i = 0; i < length; i++) {
        sum += elements[i];
        sumSq += elements[i] * elements[i];
    }

    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);

    double mean = sum / length;
    return (sumSq / length) - (mean * mean);
}

extern "C" JNIEXPORT jdouble JNICALL
Java_cn_ac_oac_libs_andas_core_NativeMath_std(
        JNIEnv* env,
        jobject /* this */,
        jdoubleArray array
) {
    double var = Java_cn_ac_oac_libs_andas_core_NativeMath_variance(env, nullptr, array);
    return std::sqrt(std::max(var, 0.0));
}

// 排序和索引
extern "C" JNIEXPORT jintArray JNICALL
Java_cn_ac_oac_libs_andas_core_NativeMath_argsort(
        JNIEnv* env,
        jobject /* this */,
        jdoubleArray array
) {
    jsize length = env->GetArrayLength(array);
    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);

    std::vector<int> indices(length);
    for (int i = 0; i < length; i++) {
        indices[i] = i;
    }

    // 使用标准排序算法
    std::sort(indices.begin(), indices.end(),
              [&](int a, int b) { return elements[a] < elements[b]; });

    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);

    jintArray result = env->NewIntArray(length);
    env->SetIntArrayRegion(result, 0, length, indices.data());

    return result;
}

// 布尔运算
extern "C" JNIEXPORT jbooleanArray JNICALL
Java_cn_ac_oac_libs_andas_core_NativeMath_greaterThan(
        JNIEnv* env,
        jobject /* this */,
        jdoubleArray array,
        jdouble threshold
) {
    jsize length = env->GetArrayLength(array);
    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);

    jbooleanArray result = env->NewBooleanArray(length);
    jboolean* resultElements = env->GetBooleanArrayElements(result, nullptr);

    for (int i = 0; i < length; i++) {
        resultElements[i] = elements[i] > threshold;
    }

    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);
    env->ReleaseBooleanArrayElements(result, resultElements, 0);

    return result;
}
