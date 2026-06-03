package cn.hongt.monitor.server.common.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricUnitUtilTest {

    private static final double DELTA = 0.000001D;

    @Test
    void toKbPerSecond_shouldConvertAllRateUnitsToKbPerSecond() {
        assertEquals(1D, MetricUnitUtil.toKbPerSecond(1D, "KB/s"), DELTA);
        assertEquals(1024D, MetricUnitUtil.toKbPerSecond(1D, "MB/s"), DELTA);
        assertEquals(1024D * 1024D, MetricUnitUtil.toKbPerSecond(1D, "GB/s"), DELTA);
        assertEquals(1024D * 1024D * 1024D, MetricUnitUtil.toKbPerSecond(1D, "TB/s"), DELTA);
        assertEquals(1024D * 1024D * 1024D * 1024D, MetricUnitUtil.toKbPerSecond(1D, "PB/s"), DELTA);
    }

    @Test
    void fromKbPerSecond_shouldConvertKbPerSecondToAllDisplayUnits() {
        assertEquals(1D, MetricUnitUtil.fromKbPerSecond(1024D, "MB/s"), DELTA);
        assertEquals(1D, MetricUnitUtil.fromKbPerSecond(1024D * 1024D, "GB/s"), DELTA);
        assertEquals(1D, MetricUnitUtil.fromKbPerSecond(1024D * 1024D * 1024D, "TB/s"), DELTA);
        assertEquals(1D, MetricUnitUtil.fromKbPerSecond(1024D * 1024D * 1024D * 1024D, "PB/s"), DELTA);
    }

    @Test
    void selectDisplayUnit_shouldChooseUnitByPeakValue() {
        assertEquals("KB/s", MetricUnitUtil.selectDisplayUnit(Collections.singletonList(1023D)));
        assertEquals("MB/s", MetricUnitUtil.selectDisplayUnit(Collections.singletonList(1024D)));
        assertEquals("GB/s", MetricUnitUtil.selectDisplayUnit(Collections.singletonList(1024D * 1024D)));
        assertEquals("TB/s", MetricUnitUtil.selectDisplayUnit(Collections.singletonList(1024D * 1024D * 1024D)));
        assertEquals("PB/s", MetricUnitUtil.selectDisplayUnit(Collections.singletonList(1024D * 1024D * 1024D * 1024D)));
    }

    @Test
    void convertSeriesByDisplayUnit_shouldConvertWholeSeriesWithSameDisplayUnit() {
        List<Double> converted = MetricUnitUtil.convertSeriesByDisplayUnit(
            Arrays.asList(1024D, 2048D, 3072D),
            "MB/s"
        );

        assertEquals(1D, converted.get(0), DELTA);
        assertEquals(2D, converted.get(1), DELTA);
        assertEquals(3D, converted.get(2), DELTA);
    }

    @Test
    void defaultWarnUnit_shouldReturnMbPerSecondForIoAndNetwork() {
        assertEquals("MB/s", MetricUnitUtil.defaultWarnUnit("linux_IO_read"));
        assertEquals("MB/s", MetricUnitUtil.defaultWarnUnit("docker_network_up"));
        assertEquals("%", MetricUnitUtil.defaultWarnUnit("linux_cpu"));
        assertEquals("%", MetricUnitUtil.defaultWarnUnit(null));
    }
}
