#include <jni.h>
#include <cmath>
#include <vector>
#include <limits>

extern "C" JNIEXPORT jdouble JNICALL
Java_cn_ac_oac_libs_andas_scipy_NativeSciPy_erf(JNIEnv*, jobject, jdouble x) {
    return std::erf(x);
}

extern "C" JNIEXPORT jdouble JNICALL
Java_cn_ac_oac_libs_andas_scipy_NativeSciPy_gamma(JNIEnv*, jobject, jdouble x) {
    return std::tgamma(x);
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_cn_ac_oac_libs_andas_scipy_NativeSciPy_convolve(
        JNIEnv* env, jobject, jdoubleArray a, jdoubleArray b) {
    const jsize n = env->GetArrayLength(a);
    const jsize m = env->GetArrayLength(b);
    if (n == 0 || m == 0) return nullptr;
    jdouble* first = env->GetDoubleArrayElements(a, nullptr);
    jdouble* second = env->GetDoubleArrayElements(b, nullptr);
    jdoubleArray output = env->NewDoubleArray(n + m - 1);
    jdouble* result = env->GetDoubleArrayElements(output, nullptr);
    for (jsize i = 0; i < n + m - 1; ++i) result[i] = 0.0;
    for (jsize i = 0; i < n; ++i) {
        for (jsize j = 0; j < m; ++j) result[i + j] += first[i] * second[j];
    }
    env->ReleaseDoubleArrayElements(a, first, JNI_ABORT);
    env->ReleaseDoubleArrayElements(b, second, JNI_ABORT);
    env->ReleaseDoubleArrayElements(output, result, 0);
    return output;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_cn_ac_oac_libs_andas_scipy_NativeSciPy_histogram(
        JNIEnv* env, jobject, jdoubleArray data, jdouble minValue, jdouble maxValue, jint bins) {
    const jsize length = env->GetArrayLength(data);
    jdouble* values = env->GetDoubleArrayElements(data, nullptr);
    jintArray output = env->NewIntArray(bins);
    jint* counts = env->GetIntArrayElements(output, nullptr);
    for (jint i = 0; i < bins; ++i) counts[i] = 0;
    const double width = (maxValue - minValue) / bins;
    for (jsize i = 0; i < length; ++i) {
        int index = width == 0.0 ? 0 : static_cast<int>((values[i] - minValue) / width);
        if (index < 0) index = 0;
        if (index >= bins) index = bins - 1;
        counts[index]++;
    }
    env->ReleaseDoubleArrayElements(data, values, JNI_ABORT);
    env->ReleaseIntArrayElements(output, counts, 0);
    return output;
}
