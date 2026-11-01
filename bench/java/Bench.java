import cn.ac.oac.libs.andas.entity.DataFrame;
import cn.ac.oac.libs.andas.entity.DataFrameIO;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryType;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 独立的 JVM 基准测试入口（不依赖 JUnit / Android 运行时）。
 *
 * 用法:
 *   java -Xmx256m -cp <classes>:<kotlin-stdlib> Bench read  <csv>
 *   java -Xmx1g   -cp <classes>:<kotlin-stdlib> Bench <op>  <csv>
 *   java -Xmx1g   -cp <classes>:<kotlin-stdlib> Bench merge <left.csv> <right.csv>
 *
 * 输出一行 JSON，由 bench/run_benchmarks.py 汇总。
 */
public final class Bench {

    private static final List<String> NULL_VALUES =
            Arrays.asList("", "null", "NULL", "NA", "N/A");

    private static volatile boolean sampling = false;
    private static Thread samplerThread = null;
    private static final AtomicLong SAMPLED_PEAK = new AtomicLong(0L);

    private static void startPeakSampling() {
        SAMPLED_PEAK.set(0L);
        sampling = true;
        samplerThread = new Thread(() -> {
            while (sampling) {
                try {
                    long used = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
                    SAMPLED_PEAK.accumulateAndGet(used, Math::max);
                    Thread.sleep(50L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Throwable ignored) {
                    // 堆耗尽时采样线程自行退出，避免污染被测进程的输出
                    return;
                }
            }
        }, "heap-peak-sampler");
        samplerThread.setDaemon(true);
        samplerThread.start();
    }

    private static long stopPeakSampling() {
        sampling = false;
        if (samplerThread != null) {
            try {
                samplerThread.join(500L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            samplerThread = null;
        }
        long peak = SAMPLED_PEAK.get();
        long used = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        return Math.max(peak, used) / (1024L * 1024L);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("usage: Bench <mode> <csv> [right.csv]");
            System.exit(2);
        }
        String mode = args[0];
        String impl = System.getProperty("impl", "unknown");
        String path = args[1];

        String status = "ok";
        String error = null;
        DataFrame df = null;
        long readMs = 0L;
        long opMs = 0L;
        int rows = 0;
        long peakMb = 0L;
        String result = "";

        try {
            // 预热 JIT / 类加载：小数据集读取 + 对应操作
            DataFrame warm;
            DataFrame warmRight = null;
            if ("merge".equals(mode)) {
                warm = read(args[1]);
                warmRight = read(args[2]);
            } else {
                warm = read(pathForWarmup(path));
            }
            if (!"read".equals(mode)) {
                runOperation(mode, warm, warmRight);
            }
            warm = null;
            warmRight = null;

            startPeakSampling();
            long t0 = System.nanoTime();
            try {
                df = read(path);
            } catch (OutOfMemoryError oom) {
                status = "oom";
                error = "OutOfMemoryError";
            }
            readMs = (System.nanoTime() - t0) / 1_000_000L;
            peakMb = stopPeakSampling();

            if (df != null) {
                rows = df.shape().getFirst();
                if (!"read".equals(mode)) {
                    DataFrame right = null;
                    if ("merge".equals(mode)) {
                        right = read(args[2]);
                    }
                    long o0 = System.nanoTime();
                    result = String.valueOf(runOperation(mode, df, right));
                    opMs = (System.nanoTime() - o0) / 1_000_000L;
                } else {
                    long longValue = ((Number) df.at(0, "long_col_4")).longValue();
                    int intValue = ((Number) df.at(0, "int_col_7")).intValue();
                    double doubleValue = ((Number) df.at(0, "double_col_10")).doubleValue();
                    result = "s0=" + df.at(0, "string_col_1")
                            + ";l0=" + longValue
                            + ";i0=" + intValue
                            + ";d0=" + doubleValue
                            + ";last=" + df.at(rows - 1, "string_col_1");
                }
            }
        } catch (OutOfMemoryError oom) {
            status = "oom";
            error = "OutOfMemoryError";
        } catch (Throwable t) {
            status = "error";
            error = t.getClass().getSimpleName() + ": " + t.getMessage();
        }

        long usedMb = usedMb();
        System.out.println(toJson(impl, mode, rows, readMs, opMs, peakMb, usedMb, status, error, result));
    }

    private static String pathForWarmup(String path) {
        // 同目录下固定的 10k 小文件；找不到时退回到目标文件
        File target = new File(path).getAbsoluteFile();
        File warm = new File(target.getParentFile(), "bench_10000_c15.csv");
        return warm.exists() ? warm.getAbsolutePath() : path;
    }

    private static DataFrame read(String path) {
        return DataFrameIO.INSTANCE.readCSV(
                new File(path), ",", true, true, "UTF-8", 0, NULL_VALUES, true);
    }

    private static Object runOperation(String mode, DataFrame df, DataFrame right) {
        switch (mode) {
            case "sum":
                return df.sum("double_col_10");
            case "mean":
                return df.mean("double_col_10");
            case "std":
                return df.std("double_col_10");
            case "describe": {
                java.util.Map<String, Double> d = df.describe("double_col_10");
                return d.get("count") + ";" + d.get("mean") + ";" + d.get("std");
            }
            case "sort": {
                DataFrame sorted = df.sortValues("double_col_10", false);
                return sorted.shape().getFirst() + ";" + sorted.at(0, "double_col_10");
            }
            case "filter": {
                DataFrame filtered = df.filterGreaterThan("double_col_10", 90.0);
                return filtered.shape().getFirst();
            }
            case "groupby": {
                DataFrame grouped = df.groupBySum("string_col_3", "double_col_10");
                return grouped.shape().getFirst();
            }
            case "merge": {
                DataFrame merged = df.merge(right, "id", "inner");
                return merged.shape().getFirst();
            }
            case "read":
                return "";
            default:
                throw new IllegalArgumentException("unknown mode: " + mode);
        }
    }

    private static long usedMb() {
        System.gc();
        try {
            Thread.sleep(120L);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
    }

    private static String toJson(
            String impl, String mode, int rows, long readMs, long opMs,
            long peakMb, long usedMb, String status, String error, String result) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{')
                .append("\"impl\":\"").append(impl).append("\",")
                .append("\"mode\":\"").append(mode).append("\",")
                .append("\"rows\":").append(rows).append(',')
                .append("\"read_ms\":").append(readMs).append(',')
                .append("\"op_ms\":").append(opMs).append(',')
                .append("\"peak_mb\":").append(peakMb).append(',')
                .append("\"used_mb\":").append(usedMb).append(',')
                .append("\"status\":\"").append(status).append("\",")
                .append("\"error\":");
        if (error == null) {
            sb.append("null");
        } else {
            sb.append('"').append(escape(error)).append('"');
        }
        sb.append(",\"result\":\"").append(escape(result)).append("\"}");
        return sb.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
