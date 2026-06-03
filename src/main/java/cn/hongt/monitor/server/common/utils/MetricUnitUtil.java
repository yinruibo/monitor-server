package cn.hongt.monitor.server.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 监控指标单位换算工具。
 */
public final class MetricUnitUtil {

    private static final double UNIT_STEP = 1024D;

    private MetricUnitUtil() {
    }

    /**
     * 将速率值统一换算为 KB/s。
     */
    public static double toKbPerSecond(double value, String unit) {
        String normalizedUnit = normalizeUnit(unit);
        if ("%".equals(normalizedUnit) || "KB/S".equals(normalizedUnit) || normalizedUnit.isEmpty()) {
            return value;
        }
        if ("MB/S".equals(normalizedUnit)) {
            return value * UNIT_STEP;
        }
        if ("GB/S".equals(normalizedUnit)) {
            return value * UNIT_STEP * UNIT_STEP;
        }
        if ("TB/S".equals(normalizedUnit)) {
            return value * UNIT_STEP * UNIT_STEP * UNIT_STEP;
        }
        if ("PB/S".equals(normalizedUnit)) {
            return value * UNIT_STEP * UNIT_STEP * UNIT_STEP * UNIT_STEP;
        }
        return value;
    }

    /**
     * 将 KB/s 统一换算为指定展示单位。
     */
    public static double fromKbPerSecond(double value, String unit) {
        String normalizedUnit = normalizeUnit(unit);
        if ("%".equals(normalizedUnit) || "KB/S".equals(normalizedUnit) || normalizedUnit.isEmpty()) {
            return value;
        }
        if ("MB/S".equals(normalizedUnit)) {
            return value / UNIT_STEP;
        }
        if ("GB/S".equals(normalizedUnit)) {
            return value / UNIT_STEP / UNIT_STEP;
        }
        if ("TB/S".equals(normalizedUnit)) {
            return value / UNIT_STEP / UNIT_STEP / UNIT_STEP;
        }
        if ("PB/S".equals(normalizedUnit)) {
            return value / UNIT_STEP / UNIT_STEP / UNIT_STEP / UNIT_STEP;
        }
        return value;
    }

    /**
     * 根据区间峰值选择统一展示单位。
     */
    public static String selectDisplayUnit(List<Double> valuesInKb) {
        if (valuesInKb == null || valuesInKb.isEmpty()) {
            return "KB/s";
        }
        double peakValue = valuesInKb.stream()
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(0D);
        if (peakValue >= UNIT_STEP * UNIT_STEP * UNIT_STEP * UNIT_STEP) {
            return "PB/s";
        }
        if (peakValue >= UNIT_STEP * UNIT_STEP * UNIT_STEP) {
            return "TB/s";
        }
        if (peakValue >= UNIT_STEP * UNIT_STEP) {
            return "GB/s";
        }
        if (peakValue >= UNIT_STEP) {
            return "MB/s";
        }
        return "KB/s";
    }

    /**
     * 按统一展示单位批量转换整段序列。
     */
    public static List<Double> convertSeriesByDisplayUnit(List<Double> valuesInKb, String displayUnit) {
        if (valuesInKb == null || valuesInKb.isEmpty()) {
            return Collections.emptyList();
        }
        return valuesInKb.stream()
            .filter(Objects::nonNull)
            .map(value -> round(fromKbPerSecond(value, displayUnit)))
            .collect(Collectors.toList());
    }

    /**
     * 默认告警单位规则。
     */
    public static String defaultWarnUnit(String warnType) {
        if (warnType == null) {
            return "%";
        }
        return warnType.contains("_IO_") || warnType.contains("_network_") ? "MB/s" : "%";
    }

    /**
     * 判断是否为速率单位。
     */
    public static boolean isRateUnit(String unit) {
        String normalizedUnit = normalizeUnit(unit);
        return "KB/S".equals(normalizedUnit)
            || "MB/S".equals(normalizedUnit)
            || "GB/S".equals(normalizedUnit)
            || "TB/S".equals(normalizedUnit)
            || "PB/S".equals(normalizedUnit);
    }

    /**
     * 统一保留两位小数，避免展示时位数乱飘。
     */
    public static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static String normalizeUnit(String unit) {
        return unit == null ? "" : unit.trim().toUpperCase(Locale.ROOT);
    }
}
