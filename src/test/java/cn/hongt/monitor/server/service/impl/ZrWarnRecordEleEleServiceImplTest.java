package cn.hongt.monitor.server.service.impl;

import cn.hongt.monitor.server.common.consts.StationConst;
import cn.hongt.monitor.server.common.utils.Result;
import cn.hongt.monitor.server.dto.input.WarnRecMessage;
import cn.hongt.monitor.server.dto.input.ZrWarnRecordListInput;
import cn.hongt.monitor.server.entity.SysDockerDeployEleDO;
import cn.hongt.monitor.server.entity.SysWarnDeployDO;
import cn.hongt.monitor.server.entity.ZrDockerRecordEleDO;
import cn.hongt.monitor.server.entity.ZrWarnRecordEleDO;
import cn.hongt.monitor.server.enums.WarnRecordEnum;
import cn.hongt.monitor.server.mapper.SysWarnDeployMapper;
import cn.hongt.monitor.server.mapper.ZrDockerDeployMapper;
import cn.hongt.monitor.server.mapper.ZrWarnRecordEleMapper;
import cn.hongt.monitor.server.service.DockerMetricCollectorService;
import cn.hongt.monitor.server.service.DockerMetricsSnapshot;
import cn.hongt.monitor.server.service.ZrDockerRecordService;
import cn.hongt.monitor.server.service.ZrLinuxRecordService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZrWarnRecordEleServiceImplTest {

    @Mock
    private SysWarnDeployMapper sysWarnDeployMapper;
    @Mock
    private ZrWarnRecordEleMapper warnRecordEleMapper;
    @Mock
    private ZrLinuxRecordService zrLinuxRecordService;
    @Mock
    private ZrDockerRecordService zrDockerRecordService;
    @Mock
    private ZrDockerDeployMapper zrDockerDeployMapper;
    @Mock
    private DockerMetricCollectorService dockerMetricCollectorService;

    @TempDir
    Path tempDir;

    private ZrWarnRecordEleServiceImpl service;
    private String originalLinuxIp;
    private String originalContNames;

    @BeforeEach
    void setUp() {
        service = new ZrWarnRecordEleServiceImpl();
        initTableInfo(SysDockerDeployEleDO.class);
        initTableInfo(SysWarnDeployDO.class);
        initTableInfo(ZrWarnRecordEleDO.class);
        ReflectionTestUtils.setField(service, "sysWarnDeployMapper", sysWarnDeployMapper);
        ReflectionTestUtils.setField(service, "zrLinuxRecordService", zrLinuxRecordService);
        ReflectionTestUtils.setField(service, "dockerRecordService", zrDockerRecordService);
        ReflectionTestUtils.setField(service, "dockerDeployMapper", zrDockerDeployMapper);
        ReflectionTestUtils.setField(service, "dockerMetricCollectorService", dockerMetricCollectorService);
        ReflectionTestUtils.setField(service, "baseMapper", warnRecordEleMapper);
        originalLinuxIp = StationConst.linuxIP;
        originalContNames = StationConst.contNames;
        StationConst.linuxIP = "10.0.0.1";
        StationConst.contNames = "";
        StationConst.dockerDepMap.clear();
        StationConst.warnDepMap.clear();
    }

    @AfterEach
    void tearDown() {
        StationConst.linuxIP = originalLinuxIp;
        StationConst.contNames = originalContNames;
        StationConst.dockerDepMap.clear();
        StationConst.warnDepMap.clear();
    }

    private void initTableInfo(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) != null) {
            return;
        }
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(new MybatisConfiguration(), entityClass.getName());
        builderAssistant.setCurrentNamespace(entityClass.getName());
        TableInfoHelper.initTableInfo(builderAssistant, entityClass);
    }

    @Test
    void queryWarnSignList_shouldFilterFallbackConfigsByIp() {
        when(warnRecordEleMapper.selectList(any())).thenReturn(Collections.<ZrWarnRecordEleDO>emptyList());
        when(sysWarnDeployMapper.selectList(any())).thenReturn(Collections.singletonList(
            SysWarnDeployDO.builder().ip("10.0.0.1").warnType(WarnRecordEnum.linux_cpu.getCode()).status(0).build()
        ));

        ZrWarnRecordListInput input = ZrWarnRecordListInput.builder()
            .ip("10.0.0.1")
            .startTime("2026-04-08 00:00:00")
            .endTime("2026-04-08 01:00:00")
            .build();

        Map<String, Object> result = service.queryWarnSignList(input);

        assertNotNull(result);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<SysWarnDeployDO>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(sysWarnDeployMapper).selectList(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("ip"));
    }

    @Test
    void getOrCreateDockerWarnDeploy_shouldUseDockerSpecificWarnType() {
        when(sysWarnDeployMapper.selectList(any())).thenReturn(Collections.<SysWarnDeployDO>emptyList());
        when(sysWarnDeployMapper.insert(any(SysWarnDeployDO.class))).thenReturn(1);

        ZrDockerRecordEleDO dockerRecord = ZrDockerRecordEleDO.builder()
            .dockerName("app")
            .ip("10.0.0.1")
            .type(WarnRecordEnum.docker_cpu.getCode())
            .dataRate("90")
            .build();

        SysWarnDeployDO warnDeploy = service.getOrCreateDockerWarnDeploy(dockerRecord);

        assertNotNull(warnDeploy);
        assertEquals("app_" + WarnRecordEnum.docker_cpu.getCode(), warnDeploy.getWarnType());
        assertEquals("10.0.0.1", warnDeploy.getIp());
    }

    @Test
    void getOrCreateDockerWarnDeploy_shouldUseDefaultRateWarnUnit() {
        when(sysWarnDeployMapper.selectList(any())).thenReturn(Collections.<SysWarnDeployDO>emptyList());
        when(sysWarnDeployMapper.insert(any(SysWarnDeployDO.class))).thenReturn(1);

        ZrDockerRecordEleDO dockerRecord = ZrDockerRecordEleDO.builder()
            .dockerName("app")
            .ip("10.0.0.1")
            .type(WarnRecordEnum.docker_network_up.getCode())
            .dataRate("1024")
            .unit("KB/s")
            .build();

        SysWarnDeployDO warnDeploy = service.getOrCreateDockerWarnDeploy(dockerRecord);

        assertEquals("MB/s", warnDeploy.getUnit());
    }

    @Test
    void replaceWarnDepMap_shouldDiscardStaleEntries() {
        // 旧缓存项必须在快照替换后被整体丢弃，不能残留脏数据。
        StationConst.warnDepMap.put("stale", SysWarnDeployDO.builder().warnType("stale").build());

        StationConst.replaceWarnDepMap(Collections.singletonMap(
            WarnRecordEnum.linux_cpu.getCode(),
            SysWarnDeployDO.builder().warnType(WarnRecordEnum.linux_cpu.getCode()).build()
        ));

        assertFalse(StationConst.warnDepMap.containsKey("stale"));
        assertEquals(WarnRecordEnum.linux_cpu.getCode(), StationConst.warnDepMap.get(WarnRecordEnum.linux_cpu.getCode()).getWarnType());
    }

    @Test
    void replaceNetWorkList_shouldPublishStableSnapshot() {
        // 发布新快照后，旧列表引用不应继续污染运行态缓存。
        StationConst.replaceNetWorkList(Collections.singletonList("eth0"));
        StationConst.replaceNetWorkList(Collections.singletonList("eth1"));

        assertEquals(1, StationConst.netWorkList.size());
        assertEquals("eth1", StationConst.netWorkList.get(0));
    }

    @Test
    void getOrCreateDockerWarnDeploy_shouldInsertOnlyOnceWhenInvokedConcurrently() throws Exception {
        // 两个线程同时首次加载同一配置时，只允许一次真实插库。
        when(sysWarnDeployMapper.selectList(any())).thenReturn(Collections.<SysWarnDeployDO>emptyList());
        when(sysWarnDeployMapper.insert(any(SysWarnDeployDO.class))).thenReturn(1);

        ZrDockerRecordEleDO dockerRecord = ZrDockerRecordEleDO.builder()
            .dockerName("app")
            .ip("10.0.0.1")
            .type(WarnRecordEnum.docker_cpu.getCode())
            .dataRate("90")
            .build();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<SysWarnDeployDO> first = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(2, TimeUnit.SECONDS));
                return service.getOrCreateDockerWarnDeploy(dockerRecord);
            });
            Future<SysWarnDeployDO> second = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(2, TimeUnit.SECONDS));
                return service.getOrCreateDockerWarnDeploy(dockerRecord);
            });

            assertTrue(ready.await(2, TimeUnit.SECONDS));
            start.countDown();

            assertEquals("app_" + WarnRecordEnum.docker_cpu.getCode(), first.get(2, TimeUnit.SECONDS).getWarnType());
            assertEquals("app_" + WarnRecordEnum.docker_cpu.getCode(), second.get(2, TimeUnit.SECONDS).getWarnType());
        } finally {
            executor.shutdownNow();
        }

        verify(sysWarnDeployMapper, times(1)).insert(any(SysWarnDeployDO.class));
    }

    @Test
    void getOrLoadLinuxIp_shouldOnlyInvokeLoaderOnceWhenConcurrent() throws Exception {
        StationConst.linuxIP = "";
        AtomicInteger loadCount = new AtomicInteger();

        String first = runConcurrentStationLoad(() -> StationConst.getOrLoadLinuxIp(() -> {
            loadCount.incrementAndGet();
            return "10.0.0.2";
        }));

        String second = runConcurrentStationLoad(() -> StationConst.getOrLoadLinuxIp(() -> {
            loadCount.incrementAndGet();
            return "10.0.0.3";
        }));

        assertEquals("10.0.0.2", first);
        assertEquals("10.0.0.2", second);
        assertEquals(1, loadCount.get());
    }

    @Test
    void getOrLoadContNames_shouldPublishNormalizedContainerSnapshot() throws Exception {
        StationConst.contNames = "";
        AtomicInteger loadCount = new AtomicInteger();

        String first = runConcurrentStationLoad(() -> StationConst.getOrLoadContNames(() -> {
            loadCount.incrementAndGet();
            return " app-1,app-2 ";
        }));

        String second = runConcurrentStationLoad(() -> StationConst.getOrLoadContNames(() -> {
            loadCount.incrementAndGet();
            return "app-3";
        }));

        assertEquals("app-1,app-2", first);
        assertEquals("app-1,app-2", second);
        assertEquals(1, loadCount.get());
    }


    @Test
    void procMemory_shouldReturnZeroWhenMemTotalMissing() throws Exception {
        Path memInfoFile = tempDir.resolve("meminfo");
        Files.write(memInfoFile, Arrays.asList(
            "MemFree: 1024 kB",
            "Buffers: 512 kB",
            "Cached: 256 kB"
        ), StandardCharsets.UTF_8);

        String memoryUsage = ReflectionTestUtils.invokeMethod(service, "procMemory", memInfoFile.toString());

        assertEquals("0.00", memoryUsage);
    }

    @Test
    void procDiskIO_shouldPreserveInterruptFlagWhenSamplingInterrupted() throws Exception {
        Path diskStatsFile = tempDir.resolve("diskstats");
        Files.write(diskStatsFile, Collections.singletonList("8 0 sda 100 0 200 0 300 0 400 0 0 0 0 0 0 0 0 0 0"), StandardCharsets.UTF_8);

        try {
            Thread.currentThread().interrupt();
            @SuppressWarnings("unchecked")
            Map<String, String> ioMap = ReflectionTestUtils.invokeMethod(service, "procDiskIO", diskStatsFile.toString());

            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals("0.00 KB/s", ioMap.get("read").trim());
            assertEquals("0.00 KB/s", ioMap.get("write").trim());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void insertWarnLinuxRecord_shouldCompareRateThresholdsUsingWarnUnit() {
        StationConst.warnDepMap.put(
            WarnRecordEnum.linux_IO_read.getCode(),
            SysWarnDeployDO.builder()
                .warnType(WarnRecordEnum.linux_IO_read.getCode())
                .status(0)
                .minThresholdNum(1)
                .middleThresholdNum(2)
                .maxThresholdNum(3)
                .unit("MB/s")
                .build()
        );
        when(warnRecordEleMapper.selectList(any())).thenReturn(Collections.<ZrWarnRecordEleDO>emptyList());
        when(warnRecordEleMapper.insert(any(ZrWarnRecordEleDO.class))).thenReturn(1);

        ReflectionTestUtils.invokeMethod(
            service,
            "insertWarnLinuxRecord",
            "10.0.0.1",
            Collections.singletonList(
                cn.hongt.monitor.server.entity.ZrLinuxRecordEleDO.builder()
                    .ip("10.0.0.1")
                    .type(WarnRecordEnum.linux_IO_read.getCode())
                    .dataRate("2048")
                    .unit("KB/s")
                    .build()
            )
        );

        ArgumentCaptor<ZrWarnRecordEleDO> insertCaptor = ArgumentCaptor.forClass(ZrWarnRecordEleDO.class);
        verify(warnRecordEleMapper).insert(insertCaptor.capture());
        assertEquals(1, insertCaptor.getValue().getGrade());
    }

    @Test
    void insertWarnDockerRecord_shouldCreateWarnConfigAndRecordForIoAndNetwork() {
        when(sysWarnDeployMapper.selectList(any())).thenReturn(Collections.<SysWarnDeployDO>emptyList());
        when(sysWarnDeployMapper.insert(any(SysWarnDeployDO.class))).thenReturn(1);
        when(warnRecordEleMapper.selectList(any())).thenReturn(Collections.<ZrWarnRecordEleDO>emptyList());
        when(warnRecordEleMapper.insert(any(ZrWarnRecordEleDO.class))).thenReturn(1);

        ReflectionTestUtils.invokeMethod(
            service,
            "insertWarnDockerRecord",
            "10.0.0.1",
            Arrays.asList(
                ZrDockerRecordEleDO.builder()
                    .ip("10.0.0.1")
                    .dockerName("app")
                    .type(WarnRecordEnum.docker_IO_read.getCode())
                    .dataRate("71680")
                    .unit("KB/s")
                    .build(),
                ZrDockerRecordEleDO.builder()
                    .ip("10.0.0.1")
                    .dockerName("app")
                    .type(WarnRecordEnum.docker_network_up.getCode())
                    .dataRate("71680")
                    .unit("KB/s")
                    .build()
            )
        );

        ArgumentCaptor<SysWarnDeployDO> warnDeployCaptor = ArgumentCaptor.forClass(SysWarnDeployDO.class);
        verify(sysWarnDeployMapper, times(2)).insert(warnDeployCaptor.capture());
        assertEquals("app_" + WarnRecordEnum.docker_IO_read.getCode(), warnDeployCaptor.getAllValues().get(0).getWarnType());
        assertEquals("MB/s", warnDeployCaptor.getAllValues().get(0).getUnit());
        assertEquals("app_" + WarnRecordEnum.docker_network_up.getCode(), warnDeployCaptor.getAllValues().get(1).getWarnType());
        assertEquals("MB/s", warnDeployCaptor.getAllValues().get(1).getUnit());

        ArgumentCaptor<ZrWarnRecordEleDO> warnRecordCaptor = ArgumentCaptor.forClass(ZrWarnRecordEleDO.class);
        verify(warnRecordEleMapper, times(2)).insert(warnRecordCaptor.capture());
        assertEquals(1, warnRecordCaptor.getAllValues().get(0).getGrade());
        assertEquals(1, warnRecordCaptor.getAllValues().get(1).getGrade());
    }

    @Test
    void warnDockerIOBySch_shouldCreateWarnRecordForDockerIoRate() {
        when(dockerMetricCollectorService.listRunningContainerNames()).thenReturn(Collections.singletonList("app"));
        when(dockerMetricCollectorService.getSnapshotByContainerName("app")).thenReturn(
            DockerMetricsSnapshot.builder()
                .containerName("app")
                .readKbPerSecond(71680D)
                .writeKbPerSecond(0D)
                .available(true)
                .build()
        );
        when(zrDockerDeployMapper.selectList(any())).thenReturn(Collections.<SysDockerDeployEleDO>emptyList());
        when(zrDockerDeployMapper.insert(any(SysDockerDeployEleDO.class))).thenReturn(1);
        when(sysWarnDeployMapper.selectList(any())).thenReturn(Collections.<SysWarnDeployDO>emptyList());
        when(sysWarnDeployMapper.insert(any(SysWarnDeployDO.class))).thenReturn(1);
        when(warnRecordEleMapper.selectList(any())).thenReturn(Collections.<ZrWarnRecordEleDO>emptyList());
        when(warnRecordEleMapper.insert(any(ZrWarnRecordEleDO.class))).thenReturn(1);

        service.warnDockerIOBySch();

        ArgumentCaptor<ZrWarnRecordEleDO> warnRecordCaptor = ArgumentCaptor.forClass(ZrWarnRecordEleDO.class);
        verify(warnRecordEleMapper).insert(warnRecordCaptor.capture());
        assertEquals("app_" + WarnRecordEnum.docker_IO_read.getCode(), warnRecordCaptor.getValue().getWarnType());
        assertEquals(1, warnRecordCaptor.getValue().getGrade());
    }

    @Test
    void warnDockerNetWorkBySch_shouldCreateWarnRecordForDockerNetworkRate() {
        when(dockerMetricCollectorService.listRunningContainerNames()).thenReturn(Collections.singletonList("app"));
        when(dockerMetricCollectorService.getSnapshotByContainerName("app")).thenReturn(
            DockerMetricsSnapshot.builder()
                .containerName("app")
                .networkUpKbPerSecond(71680D)
                .networkDownKbPerSecond(0D)
                .available(true)
                .build()
        );
        when(zrDockerDeployMapper.selectList(any())).thenReturn(Collections.<SysDockerDeployEleDO>emptyList());
        when(zrDockerDeployMapper.insert(any(SysDockerDeployEleDO.class))).thenReturn(1);
        when(sysWarnDeployMapper.selectList(any())).thenReturn(Collections.<SysWarnDeployDO>emptyList());
        when(sysWarnDeployMapper.insert(any(SysWarnDeployDO.class))).thenReturn(1);
        when(warnRecordEleMapper.selectList(any())).thenReturn(Collections.<ZrWarnRecordEleDO>emptyList());
        when(warnRecordEleMapper.insert(any(ZrWarnRecordEleDO.class))).thenReturn(1);

        service.warnDockerNetWorkBySch();

        ArgumentCaptor<ZrWarnRecordEleDO> warnRecordCaptor = ArgumentCaptor.forClass(ZrWarnRecordEleDO.class);
        verify(warnRecordEleMapper).insert(warnRecordCaptor.capture());
        assertEquals("app_" + WarnRecordEnum.docker_network_up.getCode(), warnRecordCaptor.getValue().getWarnType());
        assertEquals(1, warnRecordCaptor.getValue().getGrade());
    }

    @Test
    void continued_shouldReturnOneMinuteForExactSixtySeconds() {
        Date start = new Date(1_000L);
        Date end = new Date(61_000L);

        String continuedTime = ReflectionTestUtils.invokeMethod(service, "continued", start, end);

        assertEquals("1分", continuedTime);
    }

    @Test
    void continued_shouldNotRoundWholeHourUpToExtraMinute() {
        Date start = new Date(1_000L);
        Date end = new Date(3_601_000L);

        String continuedTime = ReflectionTestUtils.invokeMethod(service, "continued", start, end);

        assertEquals("1小时0分", continuedTime);
    }

    @Test
    void queryWarnNumber_shouldUseCountQuery() {
        when(warnRecordEleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3);

        Result<Integer> result = service.queryWarnNumber("10.0.0.1");

        assertEquals("200", result.getCode());
        assertEquals(3, result.getData());
        verify(warnRecordEleMapper).selectCount(any(LambdaQueryWrapper.class));
        verify(warnRecordEleMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void queryTimingUpdate_shouldProcessWarnRecordsInPages() {
        Date now = new Date();
        Date before = new Date(now.getTime() - 60_000L);
        Page<ZrWarnRecordEleDO> firstPage = new Page<>(1, 200, false);
        firstPage.setRecords(Collections.singletonList(ZrWarnRecordEleDO.builder().id("warn-1").updateTime(before).build()));
        Page<ZrWarnRecordEleDO> secondPage = new Page<>(2, 200, false);
        secondPage.setRecords(Collections.emptyList());
        when(warnRecordEleMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(firstPage, secondPage);
        when(warnRecordEleMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

        Result<String> result = service.queryTimingUpdate();

        assertEquals("200", result.getCode());
        assertEquals("数据更新成功", result.getData());
        verify(warnRecordEleMapper, times(2)).selectPage(any(Page.class), any(QueryWrapper.class));
        verify(warnRecordEleMapper).update(isNull(), any(UpdateWrapper.class));
        verify(warnRecordEleMapper, never()).selectList(any(QueryWrapper.class));
    }
    private String runConcurrentStationLoad(Callable<String> callable) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(2, TimeUnit.SECONDS));
                return callable.call();
            });
            Future<String> second = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(2, TimeUnit.SECONDS));
                return callable.call();
            });

            assertTrue(ready.await(2, TimeUnit.SECONDS));
            start.countDown();
            assertEquals(first.get(2, TimeUnit.SECONDS), second.get(2, TimeUnit.SECONDS));
            return first.get(2, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    // ==================== 边界测试 ====================

    @Test
    void resolveWarnGrade_shouldReturnNullForNullWarnDeploy() {
        Integer result = ReflectionTestUtils.invokeMethod(service, "resolveWarnGrade", "50", "percent", null);
        assertEquals(null, result);
    }

    @Test
    void resolveWarnGrade_shouldReturnNullForBlankDataRate() {
        SysWarnDeployDO warnDeploy = SysWarnDeployDO.builder()
                .minThresholdNum(10)
                .middleThresholdNum(50)
                .maxThresholdNum(90)
                .unit("percent")
                .build();
        Integer result = ReflectionTestUtils.invokeMethod(service, "resolveWarnGrade", "", "percent", warnDeploy);
        assertEquals(null, result);
    }

    @Test
    void resolveWarnGrade_shouldReturnNullForNonNumericDataRate() {
        SysWarnDeployDO warnDeploy = SysWarnDeployDO.builder()
                .minThresholdNum(10)
                .middleThresholdNum(50)
                .maxThresholdNum(90)
                .unit("percent")
                .build();
        Integer result = ReflectionTestUtils.invokeMethod(service, "resolveWarnGrade", "abc", "percent", warnDeploy);
        assertEquals(null, result);
    }

    @Test
    void resolveWarnGrade_shouldReturnNullForValueBelowMinThreshold() {
        SysWarnDeployDO warnDeploy = SysWarnDeployDO.builder()
                .minThresholdNum(10)
                .middleThresholdNum(50)
                .maxThresholdNum(90)
                .unit("percent")
                .build();
        Integer result = ReflectionTestUtils.invokeMethod(service, "resolveWarnGrade", "5", "percent", warnDeploy);
        assertEquals(null, result);
    }

    @Test
    void resolveWarnGrade_shouldReturn0ForValueAtMinThreshold() {
        SysWarnDeployDO warnDeploy = SysWarnDeployDO.builder()
                .minThresholdNum(10)
                .middleThresholdNum(50)
                .maxThresholdNum(90)
                .unit("percent")
                .build();
        Integer result = ReflectionTestUtils.invokeMethod(service, "resolveWarnGrade", "10", "percent", warnDeploy);
        assertEquals(0, result);
    }

    @Test
    void resolveWarnGrade_shouldReturn1ForValueAtMiddleThreshold() {
        SysWarnDeployDO warnDeploy = SysWarnDeployDO.builder()
                .minThresholdNum(10)
                .middleThresholdNum(50)
                .maxThresholdNum(90)
                .unit("percent")
                .build();
        Integer result = ReflectionTestUtils.invokeMethod(service, "resolveWarnGrade", "50", "percent", warnDeploy);
        assertEquals(1, result);
    }

    @Test
    void resolveWarnGrade_shouldReturn2ForValueAtMaxThreshold() {
        SysWarnDeployDO warnDeploy = SysWarnDeployDO.builder()
                .minThresholdNum(10)
                .middleThresholdNum(50)
                .maxThresholdNum(90)
                .unit("percent")
                .build();
        Integer result = ReflectionTestUtils.invokeMethod(service, "resolveWarnGrade", "90", "percent", warnDeploy);
        assertEquals(2, result);
    }

    @Test
    void resolveWarnGrade_shouldReturn2ForValueAboveMaxThreshold() {
        SysWarnDeployDO warnDeploy = SysWarnDeployDO.builder()
                .minThresholdNum(10)
                .middleThresholdNum(50)
                .maxThresholdNum(90)
                .unit("percent")
                .build();
        Integer result = ReflectionTestUtils.invokeMethod(service, "resolveWarnGrade", "95", "percent", warnDeploy);
        assertEquals(2, result);
    }

    @Test
    void isDockerMonitorDisabled_shouldReturnFalseForNullCacheEntry() {
        StationConst.dockerDepMap.clear();
        Boolean result = ReflectionTestUtils.invokeMethod(service, "isDockerMonitorDisabled", "nonexistent_key");
        assertEquals(false, result);
    }

    @Test
    void isDockerMonitorDisabled_shouldReturnFalseWhenShowIs0() {
        StationConst.dockerDepMap.put("test_key", SysDockerDeployEleDO.builder().isShow(0).build());
        Boolean result = ReflectionTestUtils.invokeMethod(service, "isDockerMonitorDisabled", "test_key");
        assertEquals(false, result);
    }

    @Test
    void isDockerMonitorDisabled_shouldReturnTrueWhenShowIs1() {
        StationConst.dockerDepMap.put("test_key", SysDockerDeployEleDO.builder().isShow(1).build());
        Boolean result = ReflectionTestUtils.invokeMethod(service, "isDockerMonitorDisabled", "test_key");
        assertEquals(true, result);
    }

    @Test
    void formatRate_shouldFormatZeroCorrectly() {
        String result = ZrWarnRecordEleServiceImpl.formatRate(0);
        assertEquals("0.00 KB/s" + System.lineSeparator(), result);
    }

    @Test
    void formatRate_shouldFormatKbCorrectly() {
        String result = ZrWarnRecordEleServiceImpl.formatRate(512);
        assertEquals("512.00 KB/s" + System.lineSeparator(), result);
    }

    @Test
    void formatRate_shouldFormatMbCorrectly() {
        String result = ZrWarnRecordEleServiceImpl.formatRate(1024);
        assertEquals("1.00 MB/s" + System.lineSeparator(), result);
    }

    @Test
    void formatRate_shouldFormatGbCorrectly() {
        String result = ZrWarnRecordEleServiceImpl.formatRate(1024 * 1024);
        assertEquals("1.00 GB/s" + System.lineSeparator(), result);
    }
}










