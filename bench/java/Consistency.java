import cn.ac.oac.libs.andas.entity.DataFrame;
import cn.ac.oac.libs.andas.entity.DataFrameIO;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Cross-language consistency harness: compute the same metrics as pandas with Andas on the JVM.
 *
 * Usage: java -cp <classes>:<stdlib> Consistency <csv> <left.csv> <right.csv>
 */
public final class Consistency {

    private static final List<String> NULLS = Arrays.asList("", "null", "NULL", "NA", "N/A");

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("usage: Consistency <csv> <left.csv> <right.csv>");
            System.exit(2);
        }
        try {
            DataFrame df = read(args[0]);
            DataFrame left = read(args[1]);
            DataFrame right = read(args[2]);

            int rows = df.shape().getFirst();
            int cols = df.shape().getSecond();

            DataFrame sorted = df.sortValues("double_col_10", false);
            DataFrame filtered = df.filterGreaterThan("double_col_10", 90.0);
            DataFrame grouped = df.groupBySum("string_col_3", "double_col_10");
            DataFrame merged = left.merge(right, "id", "inner");
            DataFrame normalized = df.normalize("double_col_10");
            DataFrame corr = df.corr();
            Map<String, Double> describe = df.describe("double_col_10");

            double sortFirst50 = 0.0;
            double sortLast50 = 0.0;
            for (int i = 0; i < 50; i++) {
                sortFirst50 += ((Number) sorted.at(i, "double_col_10")).doubleValue();
                sortLast50 += ((Number) sorted.at(rows - 1 - i, "double_col_10")).doubleValue();
            }

            Map<String, Double> groupSums = new TreeMap<>();
            for (int i = 0; i < grouped.shape().getFirst(); i++) {
                groupSums.put(
                        String.valueOf(grouped.at(i, "string_col_3")),
                        ((Number) grouped.at(i, "double_col_10")).doubleValue());
            }

            int corrRow = corr.columns().indexOf("double_col_10");
            double correlation = ((Number) corr.at(corrRow, "double_col_11")).doubleValue();

            StringBuilder metrics = new StringBuilder();
            metrics.append('{');
            number(metrics, "rows", rows);
            number(metrics, "cols", cols);
            number(metrics, "sum_double_col_10", df.sum("double_col_10"));
            number(metrics, "mean_double_col_10", df.mean("double_col_10"));
            number(metrics, "std_double_col_10", df.std("double_col_10"));
            number(metrics, "min_double_col_10", df.min("double_col_10"));
            number(metrics, "max_double_col_10", df.max("double_col_10"));
            number(metrics, "mean_long_col_4", df.mean("long_col_4"));
            number(metrics, "describe_count", describe.get("count"));
            number(metrics, "describe_mean", describe.get("mean"));
            number(metrics, "describe_std", describe.get("std"));
            number(metrics, "describe_min", describe.get("min"));
            number(metrics, "describe_max", describe.get("max"));
            number(metrics, "sort_first", ((Number) sorted.at(0, "double_col_10")).doubleValue());
            number(metrics, "sort_last", ((Number) sorted.at(rows - 1, "double_col_10")).doubleValue());
            number(metrics, "sort_first50_sum", sortFirst50);
            number(metrics, "sort_last50_sum", sortLast50);
            number(metrics, "filter_count", filtered.shape().getFirst());
            number(metrics, "filter_sum", filtered.sum("double_col_10"));
            number(metrics, "groupby_count", grouped.shape().getFirst());
            metrics.append("\"groupby_sum\":{");
            boolean first = true;
            for (Map.Entry<String, Double> e : groupSums.entrySet()) {
                if (!first) metrics.append(',');
                first = false;
                metrics.append('"').append(escape(e.getKey())).append("\":");
                metrics.append(e.getValue());
            }
            metrics.append("},");
            number(metrics, "merge_rows", merged.shape().getFirst());
            number(metrics, "merge_amount_sum", merged.sum("amount"));
            number(metrics, "corr_double_10_11", correlation);
            number(metrics, "normalize_first", ((Number) normalized.at(0, "double_col_10")).doubleValue());
            number(metrics, "normalize_last", ((Number) normalized.at(rows - 1, "double_col_10")).doubleValue());
            // Remove the trailing comma from the last number.
            if (metrics.charAt(metrics.length() - 1) == ',') {
                metrics.setLength(metrics.length() - 1);
            }
            metrics.append('}');

            StringBuilder out = new StringBuilder();
            out.append("{\"generator\":\"andas-jvm\",\"metrics\":").append(metrics).append('}');
            System.out.println(out);
        } catch (Throwable t) {
            System.err.println(t);
            System.exit(1);
        }
    }

    private static DataFrame read(String path) {
        return DataFrameIO.INSTANCE.readCSV(
                new File(path), ",", true, true, "UTF-8", 0, NULLS, true);
    }

    private static void number(StringBuilder out, String key, double value) {
        out.append('"').append(key).append("\":").append(value).append(',');
    }

    private static void number(StringBuilder out, String key, int value) {
        out.append('"').append(key).append("\":").append(value).append(',');
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
