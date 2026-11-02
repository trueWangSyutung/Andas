import cn.ac.oac.libs.andas.numpy.FFT;
import cn.ac.oac.libs.andas.numpy.Linalg;
import cn.ac.oac.libs.andas.numpy.NDArray;
import cn.ac.oac.libs.andas.scipy.MultipleLinearRegression;
import cn.ac.oac.libs.andas.scipy.Signal;
import cn.ac.oac.libs.andas.scipy.Stats;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** JVM (Kotlin fallback) NDArray/SciPy benchmark; CSV to stdout. */
public final class NumpyBench {
    private static double sink;
    private static final int SEED = 20261102;

    interface Operation { void run(); }

    private static double medianMs(Operation operation, int warmup, int repeats) {
        for (int i = 0; i < warmup; i++) operation.run();
        double[] samples = new double[repeats];
        for (int i = 0; i < repeats; i++) {
            long start = System.nanoTime();
            operation.run();
            samples[i] = (System.nanoTime() - start) / 1_000_000.0;
        }
        java.util.Arrays.sort(samples);
        return samples[repeats / 2];
    }

    private static double[] randomArray(int size, Random random) {
        double[] values = new double[size];
        for (int i = 0; i < size; i++) values[i] = random.nextDouble();
        return values;
    }

    public static void main(String[] args) {
        Random random = new Random(SEED);
        double[] leftData = randomArray(1_000_000, random);
        double[] rightData = randomArray(1_000_000, random);
        double[] sumData = randomArray(1_000_000, random);
        NDArray left = NDArray.Companion.fromArray(leftData, leftData.length);
        NDArray right = NDArray.Companion.fromArray(rightData, rightData.length);
        NDArray sumArray = NDArray.Companion.fromArray(sumData, sumData.length);

        double[][] matrixData = new double[300][300];
        for (int r = 0; r < 300; r++) for (int c = 0; c < 300; c++) matrixData[r][c] = random.nextDouble();
        NDArray matrix = NDArray.Companion.from2D(matrixData);

        double[] fftData = randomArray(1024, random);
        NDArray fftArray = NDArray.Companion.fromArray(fftData, fftData.length);

        double[] regressionX = new double[20_000 * 5];
        double[] regressionY = new double[20_000];
        double[] weights = {1.5, -2.0, 0.5, 3.0, -1.0};
        for (int i = 0; i < 20_000; i++) {
            double value = 2.0;
            for (int j = 0; j < 5; j++) {
                double v = random.nextDouble();
                regressionX[i * 5 + j] = v;
                value += weights[j] * v;
            }
            regressionY[i] = value;
        }
        NDArray features = NDArray.Companion.fromArray(regressionX, 20_000, 5);
        NDArray target = NDArray.Companion.fromArray(regressionY, 20_000);

        double[] convolutionData = randomArray(100_000, random);
        double[] kernelData = randomArray(16, random);
        NDArray convolutionArray = NDArray.Companion.fromArray(convolutionData, convolutionData.length);
        NDArray kernel = NDArray.Companion.fromArray(kernelData, kernelData.length);

        Map<String, Double> results = new LinkedHashMap<>();
        results.put("add_1m", medianMs(() -> sink += left.plus(right).sum(), 2, 5));
        results.put("sum_1m", medianMs(() -> sink += sumArray.sum(), 2, 5));
        results.put("matmul_300", medianMs(() -> sink += matrix.matmul(matrix.transpose()).sum(), 2, 5));
        results.put("fft_1024", medianMs(() -> sink += FFT.INSTANCE.fft(fftArray).magnitude().sum(), 2, 5));
        results.put("quantile_1m", medianMs(() -> sink += Stats.INSTANCE.quantile(sumArray, 0.5), 2, 5));
        results.put("regression_20k_x5", medianMs(() -> sink += MultipleLinearRegression.INSTANCE.fit(features, target, java.util.Collections.emptyList()).getR2(), 1, 3));
        results.put("convolve_100k_x16", medianMs(() -> sink += Signal.INSTANCE.convolve(convolutionArray, kernel).sum(), 1, 3));
        results.put("normal_cdf_1m", medianMs(() -> {
            double total = 0.0;
            for (double value : sumData) total += Stats.INSTANCE.normalCdf(value, 0.0, 1.0);
            sink += total;
        }, 1, 3));

        System.out.println("op,jvm_ms");
        for (Map.Entry<String, Double> entry : results.entrySet()) {
            System.out.printf(Locale.US, "%s,%.4f%n", entry.getKey(), entry.getValue());
        }
        System.err.println("sink=" + sink);
    }
}
