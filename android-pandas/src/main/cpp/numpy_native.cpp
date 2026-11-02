#include <jni.h>
#include <cmath>
#include <vector>
#include <algorithm>

namespace {

std::vector<double> fft_radix2(const std::vector<double>& input) {
    const size_t n = input.size();
    std::vector<double> real(n);
    std::vector<double> imag(n, 0.0);
    for (size_t i = 0; i < n; ++i) real[i] = input[i];

    // bit reversal
    for (size_t i = 1, j = 0; i < n; ++i) {
        size_t bit = n >> 1;
        for (; j & bit; bit >>= 1) j ^= bit;
        j ^= bit;
        if (i < j) std::swap(real[i], real[j]);
    }

    for (size_t length = 2; length <= n; length <<= 1) {
        const double kPi = 3.14159265358979323846;
        const double angle = -2.0 * kPi / static_cast<double>(length);
        const double wlenReal = std::cos(angle);
        const double wlenImag = std::sin(angle);
        for (size_t i = 0; i < n; i += length) {
            double wReal = 1.0;
            double wImag = 0.0;
            for (size_t j = 0; j < length / 2; ++j) {
                const double uReal = real[i + j];
                const double uImag = imag[i + j];
                const double vReal = real[i + j + length / 2] * wReal - imag[i + j + length / 2] * wImag;
                const double vImag = real[i + j + length / 2] * wImag + imag[i + j + length / 2] * wReal;
                real[i + j] = uReal + vReal;
                imag[i + j] = uImag + vImag;
                real[i + j + length / 2] = uReal - vReal;
                imag[i + j + length / 2] = uImag - vImag;
                const double nextReal = wReal * wlenReal - wImag * wlenImag;
                wImag = wReal * wlenImag + wImag * wlenReal;
                wReal = nextReal;
            }
        }
    }

    std::vector<double> result(2 * n);
    for (size_t i = 0; i < n; ++i) {
        result[2 * i] = real[i];
        result[2 * i + 1] = imag[i];
    }
    return result;
}

} // namespace

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_cn_ac_oac_libs_andas_numpy_NativeNDArray_add(
        JNIEnv* env, jobject, jdoubleArray a, jdoubleArray b) {
    const jsize length = env->GetArrayLength(a);
    if (length != env->GetArrayLength(b)) return nullptr;
    jdouble* first = env->GetDoubleArrayElements(a, nullptr);
    jdouble* second = env->GetDoubleArrayElements(b, nullptr);
    jdoubleArray output = env->NewDoubleArray(length);
    jdouble* result = env->GetDoubleArrayElements(output, nullptr);
    for (jsize i = 0; i < length; ++i) result[i] = first[i] + second[i];
    env->ReleaseDoubleArrayElements(a, first, JNI_ABORT);
    env->ReleaseDoubleArrayElements(b, second, JNI_ABORT);
    env->ReleaseDoubleArrayElements(output, result, 0);
    return output;
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_cn_ac_oac_libs_andas_numpy_NativeNDArray_multiply(
        JNIEnv* env, jobject, jdoubleArray a, jdoubleArray b) {
    const jsize length = env->GetArrayLength(a);
    if (length != env->GetArrayLength(b)) return nullptr;
    jdouble* first = env->GetDoubleArrayElements(a, nullptr);
    jdouble* second = env->GetDoubleArrayElements(b, nullptr);
    jdoubleArray output = env->NewDoubleArray(length);
    jdouble* result = env->GetDoubleArrayElements(output, nullptr);
    for (jsize i = 0; i < length; ++i) result[i] = first[i] * second[i];
    env->ReleaseDoubleArrayElements(a, first, JNI_ABORT);
    env->ReleaseDoubleArrayElements(b, second, JNI_ABORT);
    env->ReleaseDoubleArrayElements(output, result, 0);
    return output;
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_cn_ac_oac_libs_andas_numpy_NativeNDArray_scale(
        JNIEnv* env, jobject, jdoubleArray a, jdouble factor) {
    const jsize length = env->GetArrayLength(a);
    jdouble* values = env->GetDoubleArrayElements(a, nullptr);
    jdoubleArray output = env->NewDoubleArray(length);
    jdouble* result = env->GetDoubleArrayElements(output, nullptr);
    for (jsize i = 0; i < length; ++i) result[i] = values[i] * factor;
    env->ReleaseDoubleArrayElements(a, values, JNI_ABORT);
    env->ReleaseDoubleArrayElements(output, result, 0);
    return output;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_cn_ac_oac_libs_andas_numpy_NativeNDArray_sum(
        JNIEnv* env, jobject, jdoubleArray a) {
    const jsize length = env->GetArrayLength(a);
    jdouble* values = env->GetDoubleArrayElements(a, nullptr);
    double total = 0.0;
    for (jsize i = 0; i < length; ++i) total += values[i];
    env->ReleaseDoubleArrayElements(a, values, JNI_ABORT);
    return total;
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_cn_ac_oac_libs_andas_numpy_NativeNDArray_matmul(
        JNIEnv* env, jobject, jdoubleArray a, jint m, jint k, jint n, jdoubleArray b) {
    if (env->GetArrayLength(a) != m * k || env->GetArrayLength(b) != k * n) return nullptr;
    jdouble* left = env->GetDoubleArrayElements(a, nullptr);
    jdouble* right = env->GetDoubleArrayElements(b, nullptr);
    jdoubleArray output = env->NewDoubleArray(m * n);
    jdouble* result = env->GetDoubleArrayElements(output, nullptr);
    for (jint i = 0; i < m; ++i) {
        for (jint j = 0; j < n; ++j) {
            double total = 0.0;
            for (jint p = 0; p < k; ++p) total += left[i * k + p] * right[p * n + j];
            result[i * n + j] = total;
        }
    }
    env->ReleaseDoubleArrayElements(a, left, JNI_ABORT);
    env->ReleaseDoubleArrayElements(b, right, JNI_ABORT);
    env->ReleaseDoubleArrayElements(output, result, 0);
    return output;
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_cn_ac_oac_libs_andas_numpy_NativeNDArray_fft(
        JNIEnv* env, jobject, jdoubleArray input) {
    const jsize length = env->GetArrayLength(input);
    if (length == 0 || (length & (length - 1)) != 0) return nullptr;
    jdouble* values = env->GetDoubleArrayElements(input, nullptr);
    std::vector<double> data(values, values + length);
    env->ReleaseDoubleArrayElements(input, values, JNI_ABORT);
    const std::vector<double> transformed = fft_radix2(data);
    jdoubleArray output = env->NewDoubleArray(static_cast<jsize>(transformed.size()));
    env->SetDoubleArrayRegion(output, 0, static_cast<jsize>(transformed.size()), transformed.data());
    return output;
}
