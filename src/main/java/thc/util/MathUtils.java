package thc.util;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.List;

public class MathUtils {
    public static double calStdDev(List<Double> data) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int i = 1; i < data.size(); i++) {
            if (data.get(i) == 0 || data.get(i-1) == 0) continue;
            var change = calculatePercentageChange(data.get(i-1), data.get(i));
            stats.addValue(change);
        }
        return stats.getStandardDeviation();
    }

    public static double calculatePercentageChange(double originalValue, double newValue) {
        if (originalValue == 0) {
            throw new IllegalArgumentException("Original value cannot be zero.");
        }
        return ((newValue - originalValue) / originalValue) * 100;
    }
}
