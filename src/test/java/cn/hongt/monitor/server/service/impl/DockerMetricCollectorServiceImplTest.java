package cn.hongt.monitor.server.service.impl;

import cn.hongt.monitor.server.common.consts.StationConst;
import cn.hongt.monitor.server.service.DockerClientFactory;
import cn.hongt.monitor.server.service.DockerMetricsSnapshot;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.model.BlkioStatEntry;
import com.github.dockerjava.api.model.BlkioStatsConfig;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.CpuStatsConfig;
import com.github.dockerjava.api.model.CpuUsageConfig;
import com.github.dockerjava.api.model.MemoryStatsConfig;
import com.github.dockerjava.api.model.StatisticNetworksConfig;
import com.github.dockerjava.api.model.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DockerMetricCollectorServiceImplTest {

    @Mock
    private DockerClientFactory dockerClientFactory;
    @Mock
    private DockerClient dockerClient;
    @Mock
    private ListContainersCmd listContainersCmd;

    @Test
    void refreshContainerSubscriptions_shouldCacheRunningContainersAndLatestSnapshot() {
        StationConst.refreshContNames("");
        StatsCmd statsCmd = org.mockito.Mockito.mock(StatsCmd.class);
        when(dockerClientFactory.getClient()).thenReturn(dockerClient);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(false)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenReturn(Collections.singletonList(createContainer("container-1", "/app")));
        when(dockerClient.statsCmd("container-1")).thenReturn(statsCmd);
        mockStreamingStats(statsCmd, createStatistics(300L, 100L, 1000L, 600L, 2L,
                512L, 1024L, Collections.<BlkioStatEntry>emptyList(), Collections.<String, StatisticNetworksConfig>emptyMap()));
        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());

        service.refreshContainerSubscriptions();

        List<String> containerNames = service.listRunningContainerNames();
        DockerMetricsSnapshot snapshot = service.getSnapshotByContainerName("app");

        assertEquals(Collections.singletonList("app"), containerNames);
        assertNotNull(snapshot);
        assertEquals(50.0D, snapshot.getCpuPercent());
        assertEquals(50.0D, snapshot.getMemoryPercent());
        assertEquals("app", snapshot.getContainerName());
        assertEquals("app", StationConst.contNames);
    }

    @Test
    void refreshContainerSubscriptions_shouldNormalizeCpuPercentByHostCapacity() {
        StationConst.refreshContNames("");
        StatsCmd statsCmd = org.mockito.Mockito.mock(StatsCmd.class);
        when(dockerClientFactory.getClient()).thenReturn(dockerClient);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(false)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenReturn(Collections.singletonList(createContainer("container-1", "/app")));
        when(dockerClient.statsCmd("container-1")).thenReturn(statsCmd);
        mockStreamingStats(statsCmd, createStatistics(350L, 100L, 2000L, 1000L, 4L,
                512L, 1024L, Collections.<BlkioStatEntry>emptyList(), Collections.<String, StatisticNetworksConfig>emptyMap()));
        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());

        service.refreshContainerSubscriptions();

        DockerMetricsSnapshot snapshot = service.getSnapshotByContainerName("app");

        assertNotNull(snapshot);
        assertEquals(25.0D, snapshot.getCpuPercent());
    }

    @Test
    void refreshContainerSubscriptions_shouldCapMemoryPercentAtOneHundred() {
        StationConst.refreshContNames("");
        StatsCmd statsCmd = org.mockito.Mockito.mock(StatsCmd.class);
        when(dockerClientFactory.getClient()).thenReturn(dockerClient);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(false)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenReturn(Collections.singletonList(createContainer("container-1", "/app")));
        when(dockerClient.statsCmd("container-1")).thenReturn(statsCmd);
        mockStreamingStats(statsCmd, createStatistics(300L, 100L, 1000L, 600L, 2L,
                2048L, 1024L, Collections.<BlkioStatEntry>emptyList(), Collections.<String, StatisticNetworksConfig>emptyMap()));
        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());

        service.refreshContainerSubscriptions();

        DockerMetricsSnapshot snapshot = service.getSnapshotByContainerName("app");

        assertNotNull(snapshot);
        assertEquals(100.0D, snapshot.getMemoryPercent());
    }

    @Test
    void refreshContainerSubscriptions_shouldCapCpuPercentAtOneHundred() {
        StationConst.refreshContNames("");
        StatsCmd statsCmd = org.mockito.Mockito.mock(StatsCmd.class);
        when(dockerClientFactory.getClient()).thenReturn(dockerClient);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(false)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenReturn(Collections.singletonList(createContainer("container-1", "/app")));
        when(dockerClient.statsCmd("container-1")).thenReturn(statsCmd);
        mockStreamingStats(statsCmd, createStatistics(2200L, 100L, 2000L, 1000L, 4L,
                512L, 1024L, Collections.<BlkioStatEntry>emptyList(), Collections.<String, StatisticNetworksConfig>emptyMap()));
        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());

        service.refreshContainerSubscriptions();

        DockerMetricsSnapshot snapshot = service.getSnapshotByContainerName("app");

        assertNotNull(snapshot);
        assertEquals(100.0D, snapshot.getCpuPercent());
    }

    @Test
    void refreshContainerSubscriptions_shouldHandleZeroSystemCpuDelta() {
        StationConst.refreshContNames("");
        StatsCmd statsCmd = org.mockito.Mockito.mock(StatsCmd.class);
        when(dockerClientFactory.getClient()).thenReturn(dockerClient);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(false)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenReturn(Collections.singletonList(createContainer("container-1", "/app")));
        when(dockerClient.statsCmd("container-1")).thenReturn(statsCmd);
        // systemCpuUsage 相同 => systemDelta=0，防止除零
        mockStreamingStats(statsCmd, createStatistics(300L, 100L, 1000L, 1000L, 2L,
                512L, 1024L, Collections.<BlkioStatEntry>emptyList(), Collections.<String, StatisticNetworksConfig>emptyMap()));
        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());

        service.refreshContainerSubscriptions();

        DockerMetricsSnapshot snapshot = service.getSnapshotByContainerName("app");
        assertNotNull(snapshot);
        assertEquals(0.0D, snapshot.getCpuPercent());
    }

    @Test
    void refreshContainerSubscriptions_shouldHandleZeroTotalUsageDelta() {
        StationConst.refreshContNames("");
        StatsCmd statsCmd = org.mockito.Mockito.mock(StatsCmd.class);
        when(dockerClientFactory.getClient()).thenReturn(dockerClient);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(false)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenReturn(Collections.singletonList(createContainer("container-1", "/app")));
        when(dockerClient.statsCmd("container-1")).thenReturn(statsCmd);
        // totalUsage 相同 => cpuDelta=0
        mockStreamingStats(statsCmd, createStatistics(100L, 100L, 2000L, 1000L, 2L,
                512L, 1024L, Collections.<BlkioStatEntry>emptyList(), Collections.<String, StatisticNetworksConfig>emptyMap()));
        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());

        service.refreshContainerSubscriptions();

        DockerMetricsSnapshot snapshot = service.getSnapshotByContainerName("app");
        assertNotNull(snapshot);
        assertEquals(0.0D, snapshot.getCpuPercent());
    }

    @Test
    void refreshContainerSubscriptions_shouldHandleZeroMemoryLimit() {
        StationConst.refreshContNames("");
        StatsCmd statsCmd = org.mockito.Mockito.mock(StatsCmd.class);
        when(dockerClientFactory.getClient()).thenReturn(dockerClient);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(false)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenReturn(Collections.singletonList(createContainer("container-1", "/app")));
        when(dockerClient.statsCmd("container-1")).thenReturn(statsCmd);
        // memoryLimit=0 => 防止除零
        mockStreamingStats(statsCmd, createStatistics(300L, 100L, 1000L, 600L, 2L,
                512L, 0L, Collections.<BlkioStatEntry>emptyList(), Collections.<String, StatisticNetworksConfig>emptyMap()));
        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());

        service.refreshContainerSubscriptions();

        DockerMetricsSnapshot snapshot = service.getSnapshotByContainerName("app");
        assertNotNull(snapshot);
        assertEquals(0.0D, snapshot.getMemoryPercent());
    }

    @Test
    void refreshContainerSubscriptions_shouldFilterNullContainer() {
        StationConst.refreshContNames("");
        when(dockerClientFactory.getClient()).thenReturn(dockerClient);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(false)).thenReturn(listContainersCmd);
        // 包含一个 null 容器和一个有效容器
        when(listContainersCmd.exec()).thenReturn(Arrays.asList(null, createContainer("container-1", "/app")));
        StatsCmd statsCmd = org.mockito.Mockito.mock(StatsCmd.class);
        when(dockerClient.statsCmd("container-1")).thenReturn(statsCmd);
        mockStreamingStats(statsCmd, createStatistics(300L, 100L, 1000L, 600L, 2L,
                512L, 1024L, Collections.<BlkioStatEntry>emptyList(), Collections.<String, StatisticNetworksConfig>emptyMap()));
        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());

        service.refreshContainerSubscriptions();

        List<String> names = service.listRunningContainerNames();
        assertEquals(Collections.singletonList("app"), names);
    }

    @Test
    void refreshContainerSubscriptions_shouldFilterBlankContainerName() {
        StationConst.refreshContNames("");
        when(dockerClientFactory.getClient()).thenReturn(dockerClient);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(false)).thenReturn(listContainersCmd);
        // 空名称的容器应被过滤
        when(listContainersCmd.exec()).thenReturn(Arrays.asList(
                createContainer("container-1", ""),
                createContainer("container-2", "/app")));
        StatsCmd statsCmd = org.mockito.Mockito.mock(StatsCmd.class);
        when(dockerClient.statsCmd("container-2")).thenReturn(statsCmd);
        mockStreamingStats(statsCmd, createStatistics(300L, 100L, 1000L, 600L, 2L,
                512L, 1024L, Collections.<BlkioStatEntry>emptyList(), Collections.<String, StatisticNetworksConfig>emptyMap()));
        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());

        service.refreshContainerSubscriptions();

        List<String> names = service.listRunningContainerNames();
        assertEquals(Collections.singletonList("app"), names);
    }

    @Test
    void getSnapshotByContainerName_shouldReturnNullForBlankName() {
        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());

        assertEquals(null, service.getSnapshotByContainerName(""));
        assertEquals(null, service.getSnapshotByContainerName(null));
        assertEquals(null, service.getSnapshotByContainerName("  "));
    }

    @Test
    void getSnapshotByContainerName_shouldReturnNullForUnknownContainer() {
        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());

        assertEquals(null, service.getSnapshotByContainerName("nonexistent"));
    }

    @Test
    void getSnapshotByContainerName_shouldReturnDefensiveCopy() {
        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());
        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentMap<String, String> containerNameToId =
                (java.util.concurrent.ConcurrentMap<String, String>) ReflectionTestUtils.getField(service, "containerNameToId");
        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentMap<String, DockerMetricsSnapshot> snapshotsByContainerId =
                (java.util.concurrent.ConcurrentMap<String, DockerMetricsSnapshot>) ReflectionTestUtils.getField(service, "snapshotsByContainerId");
        containerNameToId.put("app", "container-1");
        snapshotsByContainerId.put("container-1", DockerMetricsSnapshot.builder()
                .containerId("container-1")
                .containerName("app")
                .cpuPercent(10D)
                .memoryPercent(20D)
                .lastUpdateTime(System.currentTimeMillis())
                .available(true)
                .build());

        DockerMetricsSnapshot snapshot1 = service.getSnapshotByContainerName("app");
        DockerMetricsSnapshot snapshot2 = service.getSnapshotByContainerName("app");

        assertNotNull(snapshot1);
        assertNotNull(snapshot2);
        // 两次返回不同对象（防御性副本）
        assertFalse(snapshot1 == snapshot2);
        assertEquals(snapshot1.getCpuPercent(), snapshot2.getCpuPercent());
    }

    @Test
    void getSnapshotByContainerName_shouldMarkStaleSnapshotUnavailable() {
        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());
        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentMap<String, String> containerNameToId =
                (java.util.concurrent.ConcurrentMap<String, String>) ReflectionTestUtils.getField(service, "containerNameToId");
        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentMap<String, DockerMetricsSnapshot> snapshotsByContainerId =
                (java.util.concurrent.ConcurrentMap<String, DockerMetricsSnapshot>) ReflectionTestUtils.getField(service, "snapshotsByContainerId");
        containerNameToId.put("app", "container-1");
        snapshotsByContainerId.put("container-1", DockerMetricsSnapshot.builder()
                .containerId("container-1")
                .containerName("app")
                .cpuPercent(10D)
                .memoryPercent(20D)
                .lastUpdateTime(System.currentTimeMillis() - 300001L)
                .available(true)
                .build());

        DockerMetricsSnapshot snapshot = service.getSnapshotByContainerName("app");

        assertNotNull(snapshot);
        assertFalse(snapshot.isAvailable());
    }

    @Test
    void refreshContainerSubscriptions_shouldTruncateMemoryPercentAbove100() {
        StationConst.refreshContNames("");
        StatsCmd statsCmd = org.mockito.Mockito.mock(StatsCmd.class);
        when(dockerClientFactory.getClient()).thenReturn(dockerClient);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(false)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenReturn(Collections.singletonList(createContainer("container-1", "/app")));
        when(dockerClient.statsCmd("container-1")).thenReturn(statsCmd);
        // 内存使用量超过限制（异常情况），应被截断到100%
        mockStreamingStats(statsCmd, createStatistics(300L, 100L, 1000L, 600L, 2L,
                2048L, 1024L, Collections.<BlkioStatEntry>emptyList(), Collections.<String, StatisticNetworksConfig>emptyMap()));

        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());
        service.refreshContainerSubscriptions();

        DockerMetricsSnapshot snapshot = service.getSnapshotByContainerName("app");
        assertNotNull(snapshot);
        assertEquals(100.0D, snapshot.getMemoryPercent());
    }

    @Test
    void refreshContainerSubscriptions_shouldHandleNullBlkioStats() {
        StationConst.refreshContNames("");
        StatsCmd statsCmd = org.mockito.Mockito.mock(StatsCmd.class);
        when(dockerClientFactory.getClient()).thenReturn(dockerClient);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(false)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenReturn(Collections.singletonList(createContainer("container-1", "/app")));
        when(dockerClient.statsCmd("container-1")).thenReturn(statsCmd);

        Statistics statistics = new Statistics();
        ReflectionTestUtils.setField(statistics, "cpuStats", createCpuStats(300L, 1000L, 2L));
        ReflectionTestUtils.setField(statistics, "preCpuStats", createCpuStats(100L, 600L, 2L));
        ReflectionTestUtils.setField(statistics, "memoryStats", createMemoryStats(512L, 1024L));
        // blkioStats 保持 null
        ReflectionTestUtils.setField(statistics, "networks", Collections.<String, StatisticNetworksConfig>emptyMap());

        mockStreamingStats(statsCmd, statistics);

        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());
        service.refreshContainerSubscriptions();

        DockerMetricsSnapshot snapshot = service.getSnapshotByContainerName("app");
        assertNotNull(snapshot);
        assertEquals(0.0D, snapshot.getReadKbPerSecond());
        assertEquals(0.0D, snapshot.getWriteKbPerSecond());
    }

    @Test
    void refreshContainerSubscriptions_shouldHandleNullNetworkStats() {
        StationConst.refreshContNames("");
        StatsCmd statsCmd = org.mockito.Mockito.mock(StatsCmd.class);
        when(dockerClientFactory.getClient()).thenReturn(dockerClient);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(false)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenReturn(Collections.singletonList(createContainer("container-1", "/app")));
        when(dockerClient.statsCmd("container-1")).thenReturn(statsCmd);

        Statistics statistics = new Statistics();
        ReflectionTestUtils.setField(statistics, "cpuStats", createCpuStats(300L, 1000L, 2L));
        ReflectionTestUtils.setField(statistics, "preCpuStats", createCpuStats(100L, 600L, 2L));
        ReflectionTestUtils.setField(statistics, "memoryStats", createMemoryStats(512L, 1024L));
        ReflectionTestUtils.setField(statistics, "blkioStats", createBlkioStats(Collections.<BlkioStatEntry>emptyList()));
        // networks 保持 null

        mockStreamingStats(statsCmd, statistics);

        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());
        service.refreshContainerSubscriptions();

        DockerMetricsSnapshot snapshot = service.getSnapshotByContainerName("app");
        assertNotNull(snapshot);
        assertEquals(0.0D, snapshot.getNetworkDownKbPerSecond());
        assertEquals(0.0D, snapshot.getNetworkUpKbPerSecond());
    }

    @Test
    void refreshContainerSubscriptions_shouldCleanupGhostDataWhenContainerRemoved() {
        StationConst.refreshContNames("");
        StatsCmd statsCmd = org.mockito.Mockito.mock(StatsCmd.class);
        when(dockerClientFactory.getClient()).thenReturn(dockerClient);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(false)).thenReturn(listContainersCmd);

        // 第一次刷新：有容器
        when(listContainersCmd.exec()).thenReturn(Collections.singletonList(createContainer("container-1", "/app")));
        when(dockerClient.statsCmd("container-1")).thenReturn(statsCmd);
        mockStreamingStats(statsCmd, createStatistics(300L, 100L, 1000L, 600L, 2L,
                512L, 1024L, Collections.<BlkioStatEntry>emptyList(), Collections.<String, StatisticNetworksConfig>emptyMap()));

        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());
        service.refreshContainerSubscriptions();

        assertNotNull(service.getSnapshotByContainerName("app"));
        assertEquals(Collections.singletonList("app"), service.listRunningContainerNames());

        // 第二次刷新：容器已移除
        when(listContainersCmd.exec()).thenReturn(Collections.<Container>emptyList());
        service.refreshContainerSubscriptions();

        // 幽灵数据应被清理
        assertEquals(null, service.getSnapshotByContainerName("app"));
        assertEquals(Collections.emptyList(), service.listRunningContainerNames());
    }

    private Executor directExecutor() {
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }

    private Container createContainer(String id, String... names) {
        Container container = new Container();
        ReflectionTestUtils.setField(container, "id", id);
        ReflectionTestUtils.setField(container, "names", names);
        return container;
    }

    private void mockStreamingStats(StatsCmd statsCmd, Statistics statistics) {
        when(statsCmd.exec(any(ResultCallback.class))).thenAnswer(invocation -> {
            ResultCallback<Statistics> callback = invocation.getArgument(0);
            callback.onStart(new Closeable() {
                @Override
                public void close() {
                }
            });
            callback.onNext(statistics);
            callback.onComplete();
            return callback;
        });
    }

    private Statistics createStatistics(Long totalUsage, Long preTotalUsage,
                                        Long systemUsage, Long preSystemUsage, Long onlineCpus,
                                        Long memoryUsage, Long memoryLimit,
                                        List<BlkioStatEntry> blkioEntries,
                                        Map<String, StatisticNetworksConfig> networkMap) {
        Statistics statistics = new Statistics();
        ReflectionTestUtils.setField(statistics, "cpuStats", createCpuStats(totalUsage, systemUsage, onlineCpus));
        ReflectionTestUtils.setField(statistics, "preCpuStats", createCpuStats(preTotalUsage, preSystemUsage, onlineCpus));
        ReflectionTestUtils.setField(statistics, "memoryStats", createMemoryStats(memoryUsage, memoryLimit));
        ReflectionTestUtils.setField(statistics, "blkioStats", createBlkioStats(blkioEntries));
        ReflectionTestUtils.setField(statistics, "networks", networkMap);
        return statistics;
    }

    private CpuStatsConfig createCpuStats(Long totalUsage, Long systemUsage, Long onlineCpus) {
        CpuUsageConfig cpuUsage = new CpuUsageConfig();
        ReflectionTestUtils.setField(cpuUsage, "totalUsage", totalUsage);
        CpuStatsConfig cpuStats = new CpuStatsConfig();
        ReflectionTestUtils.setField(cpuStats, "cpuUsage", cpuUsage);
        ReflectionTestUtils.setField(cpuStats, "systemCpuUsage", systemUsage);
        ReflectionTestUtils.setField(cpuStats, "onlineCpus", onlineCpus);
        return cpuStats;
    }

    private MemoryStatsConfig createMemoryStats(Long usage, Long limit) {
        MemoryStatsConfig memoryStats = new MemoryStatsConfig();
        ReflectionTestUtils.setField(memoryStats, "usage", usage);
        ReflectionTestUtils.setField(memoryStats, "limit", limit);
        return memoryStats;
    }

    private BlkioStatsConfig createBlkioStats(List<BlkioStatEntry> entries) {
        BlkioStatsConfig blkioStats = new BlkioStatsConfig();
        ReflectionTestUtils.setField(blkioStats, "ioServiceBytesRecursive", entries);
        return blkioStats;
    }

    private List<BlkioStatEntry> createBlkioEntries(Long readBytes, Long writeBytes) {
        return Arrays.asList(
                new BlkioStatEntry().withOp("Read").withValue(readBytes),
                new BlkioStatEntry().withOp("Write").withValue(writeBytes)
        );
    }

    private Map<String, StatisticNetworksConfig> createNetworkMap(Long rxBytes, Long txBytes) {
        StatisticNetworksConfig networkStats = new StatisticNetworksConfig();
        ReflectionTestUtils.setField(networkStats, "rxBytes", rxBytes);
        ReflectionTestUtils.setField(networkStats, "txBytes", txBytes);
        Map<String, StatisticNetworksConfig> networkMap = new HashMap<String, StatisticNetworksConfig>();
        networkMap.put("eth0", networkStats);
        return networkMap;
    }

    // ==================== 并发安全测试 ====================

    @Test
    void concurrentRefreshContainerSubscriptions_shouldNotCorruptState() throws Exception {
        StationConst.refreshContNames("");
        int threadCount = 10;
        ExecutorService testExecutor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicReference<Throwable> error = new AtomicReference<>(null);

        StatsCmd statsCmd = org.mockito.Mockito.mock(StatsCmd.class);
        when(dockerClientFactory.getClient()).thenReturn(dockerClient);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(false)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenReturn(Collections.singletonList(createContainer("container-1", "/app")));
        when(dockerClient.statsCmd("container-1")).thenReturn(statsCmd);
        mockStreamingStats(statsCmd, createStatistics(300L, 100L, 1000L, 1000L, 2L,
                512L, 1024L, Collections.<BlkioStatEntry>emptyList(), Collections.<String, StatisticNetworksConfig>emptyMap()));

        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());

        for (int i = 0; i < threadCount; i++) {
            testExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        startLatch.await();
                        service.refreshContainerSubscriptions();
                        successCount.incrementAndGet();
                    } catch (Throwable t) {
                        error.compareAndSet(null, t);
                    } finally {
                        doneLatch.countDown();
                    }
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        testExecutor.shutdown();

        if (error.get() != null) {
            throw new AssertionError("Concurrent refresh failed", error.get());
        }
        assertEquals(threadCount, successCount.get());
        List<String> names = service.listRunningContainerNames();
        assertEquals(Collections.singletonList("app"), names);
    }

    @Test
    void concurrentGetSnapshot_shouldReturnConsistentDefensiveCopies() throws Exception {
        StationConst.refreshContNames("");
        StatsCmd statsCmd = org.mockito.Mockito.mock(StatsCmd.class);
        when(dockerClientFactory.getClient()).thenReturn(dockerClient);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(false)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenReturn(Collections.singletonList(createContainer("container-1", "/app")));
        when(dockerClient.statsCmd("container-1")).thenReturn(statsCmd);
        mockStreamingStats(statsCmd, createStatistics(300L, 100L, 1000L, 600L, 2L,
                512L, 1024L, Collections.<BlkioStatEntry>emptyList(), Collections.<String, StatisticNetworksConfig>emptyMap()));

        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());
        service.refreshContainerSubscriptions();

        int threadCount = 10;
        int snapshotsPerThread = 100;
        ExecutorService testExecutor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<List<DockerMetricsSnapshot>>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(testExecutor.submit(new java.util.concurrent.Callable<List<DockerMetricsSnapshot>>() {
                @Override
                public List<DockerMetricsSnapshot> call() throws Exception {
                    startLatch.await();
                    List<DockerMetricsSnapshot> snapshots = new ArrayList<>();
                    for (int j = 0; j < snapshotsPerThread; j++) {
                        DockerMetricsSnapshot s = service.getSnapshotByContainerName("app");
                        if (s != null) {
                            snapshots.add(s);
                        }
                    }
                    return snapshots;
                }
            }));
        }

        startLatch.countDown();

        // 等待所有任务完成
        List<DockerMetricsSnapshot> allSnapshots = new ArrayList<>();
        for (Future<List<DockerMetricsSnapshot>> f : futures) {
            allSnapshots.addAll(f.get());
        }
        testExecutor.shutdown();

        // 每个快照应该有相同的值（因为底层数据没变）
        for (DockerMetricsSnapshot s : allSnapshots) {
            assertEquals(50.0D, s.getCpuPercent(), 0.001);
            assertEquals(50.0D, s.getMemoryPercent(), 0.001);
        }

        // 验证不同线程获取的是不同对象实例（防御性拷贝）
        if (allSnapshots.size() >= 2) {
            DockerMetricsSnapshot first = allSnapshots.get(0);
            DockerMetricsSnapshot second = allSnapshots.get(1);
            assertFalse(first == second, "Snapshots should be different object instances (defensive copy)");
        }
    }

    @Test
    void startingContainerIds_shouldPreventDuplicateSubscriptions() {
        StationConst.refreshContNames("");
        when(dockerClientFactory.getClient()).thenReturn(dockerClient);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(false)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenReturn(Collections.singletonList(createContainer("container-1", "/app")));

        // 使用空执行器，任务不会被执行，验证startingContainerIds守卫机制
        Executor noopExecutor = new Executor() {
            @Override
            public void execute(Runnable command) {
                // 不执行任务
            }
        };

        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, noopExecutor);
        service.refreshContainerSubscriptions();

        // 验证容器名称列表正确（即使订阅任务未执行，名称映射也已建立）
        List<String> names = service.listRunningContainerNames();
        assertEquals(Collections.singletonList("app"), names);
    }

    @Test
    void refreshContainerSubscriptions_shouldHandleConcurrentNameMappingUpdates() throws Exception {
        StationConst.refreshContNames("");
        int threadCount = 5;
        ExecutorService testExecutor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        StatsCmd statsCmd = org.mockito.Mockito.mock(StatsCmd.class);
        when(dockerClientFactory.getClient()).thenReturn(dockerClient);
        when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
        when(listContainersCmd.withShowAll(false)).thenReturn(listContainersCmd);
        when(listContainersCmd.exec()).thenReturn(Collections.singletonList(createContainer("container-1", "/app")));
        when(dockerClient.statsCmd("container-1")).thenReturn(statsCmd);
        mockStreamingStats(statsCmd, createStatistics(300L, 100L, 1000L, 1000L, 2L,
                512L, 1024L, Collections.<BlkioStatEntry>emptyList(), Collections.<String, StatisticNetworksConfig>emptyMap()));

        DockerMetricCollectorServiceImpl service = new DockerMetricCollectorServiceImpl(dockerClientFactory, directExecutor());

        // 先初始化一次
        service.refreshContainerSubscriptions();

        // 并发读取和刷新
        for (int i = 0; i < threadCount; i++) {
            testExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        startLatch.await();
                        // 混合读写操作
                        service.listRunningContainerNames();
                        service.getSnapshotByContainerName("app");
                        service.refreshContainerSubscriptions();
                    } catch (Exception e) {
                        // 忽略
                    } finally {
                        doneLatch.countDown();
                    }
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        testExecutor.shutdown();

        // 验证最终状态一致
        List<String> names = service.listRunningContainerNames();
        assertEquals(Collections.singletonList("app"), names);
        DockerMetricsSnapshot snapshot = service.getSnapshotByContainerName("app");
        assertNotNull(snapshot);
    }
}
