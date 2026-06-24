package cn.hongt.monitor.server.service.impl;

import cn.hongt.monitor.server.common.consts.StationConst;
import cn.hongt.monitor.server.service.DockerClientFactory;
import cn.hongt.monitor.server.service.DockerMetricCollectorService;
import cn.hongt.monitor.server.service.DockerMetricsSnapshot;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.model.BlkioStatEntry;
import com.github.dockerjava.api.model.BlkioStatsConfig;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.CpuStatsConfig;
import com.github.dockerjava.api.model.CpuUsageConfig;
import com.github.dockerjava.api.model.MemoryStatsConfig;
import com.github.dockerjava.api.model.StatisticNetworksConfig;
import com.github.dockerjava.api.model.Statistics;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 基于 docker-java stats 流的本机 Docker 指标采集实现。
 */
@Slf4j
@Service
public class DockerMetricCollectorServiceImpl implements DockerMetricCollectorService {

    private static final double BYTES_PER_KILOBYTE = 1024D;
    private static final long SNAPSHOT_STALE_MILLIS = 180000L;

    private final DockerClientFactory dockerClientFactory;
    private final Executor dockerMonitorExecutor;
    private final ConcurrentMap<String, DockerMetricsSnapshot> snapshotsByContainerId = new ConcurrentHashMap<String, DockerMetricsSnapshot>();
    private final ConcurrentMap<String, String> containerIdToName = new ConcurrentHashMap<String, String>();
    private final ConcurrentMap<String, String> containerNameToId = new ConcurrentHashMap<String, String>();
    private final ConcurrentMap<String, StatsStreamCallback> subscriptions = new ConcurrentHashMap<String, StatsStreamCallback>();
    private final ConcurrentMap<String, PreviousSample> previousSamples = new ConcurrentHashMap<String, PreviousSample>();
    private final ConcurrentMap<String, Long> lastReceiveTimes = new ConcurrentHashMap<String, Long>();
    private final Set<String> startingContainerIds = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final Set<String> restartingContainerIds = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final ConcurrentMap<String, Long> lastRestartAttempts = new ConcurrentHashMap<String, Long>();
    private final Object refreshLock = new Object();
    private volatile boolean shuttingDown;
    private volatile boolean refreshCompleted;
    private static final long MIN_RESTART_INTERVAL_MILLIS = 120000L;

    public DockerMetricCollectorServiceImpl(DockerClientFactory dockerClientFactory,
                                            @Qualifier("dockerMonitorExecutor") Executor dockerMonitorExecutor) {
        this.dockerClientFactory = dockerClientFactory;
        this.dockerMonitorExecutor = dockerMonitorExecutor;
    }

    @PostConstruct
    public void initialize() {
        refreshContainerSubscriptions();
    }

    @PreDestroy
    public void shutdown() {
        // 先设置关闭标志，阻止 @Scheduled checkSubscriptionHealth 提交新的订阅任务
        shuttingDown = true;
        stopAllSubscriptions();
        // 关闭 dockerMonitorExecutor，防止排队任务在 DockerClient 关闭后运行
        if (dockerMonitorExecutor instanceof java.util.concurrent.ExecutorService) {
            java.util.concurrent.ExecutorService executorService = (java.util.concurrent.ExecutorService) dockerMonitorExecutor;
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        dockerClientFactory.closeClient();
    }

    @Override
    public List<String> listRunningContainerNames() {
        if (containerIdToName.isEmpty() && !refreshCompleted) {
            refreshContainerSubscriptions();
        }
        List<String> containerNames = new ArrayList<String>(new LinkedHashSet<String>(containerIdToName.values()));
        Collections.sort(containerNames);
        return containerNames;
    }

    @Override
    public DockerMetricsSnapshot getSnapshotByContainerName(String containerName) {
        if (StringUtils.isBlank(containerName)) {
            return null;
        }
        String containerId = containerNameToId.get(containerName);
        if (StringUtils.isBlank(containerId)) {
            return null;
        }
        DockerMetricsSnapshot snapshot = snapshotsByContainerId.get(containerId);
        return resolveFreshSnapshot(snapshot);
    }

    @Override
    public void refreshContainerSubscriptions() {
        synchronized (refreshLock) {
            List<Container> runningContainers = loadRunningContainers();
            // loadRunningContainers 返回 null 表示 Docker API 查询失败（非"无容器"）；
            // 此时跳过本轮刷新，保留现有订阅和快照，避免瞬时错误清空全部监控状态
            if (runningContainers == null) {
                return;
            }
            Set<String> latestContainerIds = new LinkedHashSet<String>();
            List<String> latestContainerNames = new ArrayList<String>();
            for (Container container : runningContainers) {
                if (container == null || StringUtils.isBlank(container.getId())) {
                    continue;
                }
                String containerName = resolveContainerName(container);
                if (StringUtils.isBlank(containerName)) {
                    continue;
                }
                String containerId = container.getId();
                String previousName = containerIdToName.put(containerId, containerName);
                // 先建立新名称→容器ID 映射，再移除旧名称映射；
                // 避免并发读取者在 remove 后、put 前的窗口期内找不到任何映射
                containerNameToId.put(containerName, containerId);
                if (StringUtils.isNotBlank(previousName) && !StringUtils.equals(previousName, containerName)) {
                    containerNameToId.remove(previousName, containerId);
                }
                latestContainerIds.add(containerId);
                latestContainerNames.add(containerName);
                startSubscription(containerId, containerName);
            }
            for (String containerId : new ArrayList<String>(containerIdToName.keySet())) {
                if (!latestContainerIds.contains(containerId)) {
                    stopSubscription(containerId);
                }
            }
            StationConst.refreshContNames(String.join(",", latestContainerNames.stream().sorted().collect(Collectors.toList())));
            refreshCompleted = true;
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 60000L, initialDelay = 60000L)
    public void checkSubscriptionHealth() {
        if (shuttingDown) {
            return;
        }
        // 清理启动守卫集合中已不再被追踪的容器ID（容器已停止但启动任务异常中断等情况）
        for (String id : new ArrayList<String>(startingContainerIds)) {
            if (!containerIdToName.containsKey(id)) {
                startingContainerIds.remove(id);
            }
        }
        long now = System.currentTimeMillis();
        for (Map.Entry<String, String> entry : new ArrayList<Map.Entry<String, String>>(containerIdToName.entrySet())) {
            String containerId = entry.getKey();
            String containerName = entry.getValue();
            if (!subscriptions.containsKey(containerId)) {
                // 对缺失订阅的容器做退避检查：若上次重启距现在不足 MIN_RESTART_INTERVAL_MILLIS，
                // 跳过本次重启，避免对已停止的容器反复创建→立即失败→再创建的无限循环
                Long lastAttempt = lastRestartAttempts.get(containerId);
                if (lastAttempt != null && now - lastAttempt < MIN_RESTART_INTERVAL_MILLIS) {
                    continue;
                }
                lastRestartAttempts.put(containerId, now);
                startSubscription(containerId, containerName);
                continue;
            }
            Long lastReceiveTime = lastReceiveTimes.get(containerId);
            if (lastReceiveTime == null || now - lastReceiveTime > SNAPSHOT_STALE_MILLIS) {
                // 防止多个线程同时触发同一容器的重启，避免 closeSubscriptionOnly + startSubscription 重复执行
                if (!restartingContainerIds.add(containerId)) {
                    continue;
                }
                try {
                    log.warn("Docker 容器 {} 的 stats 流超过 {} ms 未更新，准备重建订阅", containerName, SNAPSHOT_STALE_MILLIS);
                    lastRestartAttempts.put(containerId, now);
                    restartSubscription(containerId, containerName);
                } finally {
                    restartingContainerIds.remove(containerId);
                }
            }
        }
    }

    private void startSubscription(final String containerId, final String containerName) {
        if (shuttingDown || StringUtils.isBlank(containerId) || StringUtils.isBlank(containerName)) {
            return;
        }
        if (subscriptions.containsKey(containerId) || !startingContainerIds.add(containerId)) {
            return;
        }
        // execute() 可能抛出 RejectedExecutionException（线程池饱和或已关闭），
        // 必须在外层 catch 中清理 startingContainerIds，否则该容器 ID 永久污染，
        // 后续所有 startSubscription 调用都会被静默跳过
        try {
            dockerMonitorExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (subscriptions.containsKey(containerId)) {
                            return;
                        }
                        StatsStreamCallback callback = new StatsStreamCallback(containerId, containerName);
                        dockerClientFactory.getClient().statsCmd(containerId).exec(callback);
                        subscriptions.put(containerId, callback);
                        lastReceiveTimes.put(containerId, System.currentTimeMillis());
                        log.info("已启动 Docker 容器 {} 的 stats 流订阅", containerName);
                    } catch (RuntimeException e) {
                        log.error("启动 Docker 容器 {} 的 stats 流订阅失败", containerName, e);
                    } finally {
                        startingContainerIds.remove(containerId);
                    }
                }
            });
        } catch (RuntimeException e) {
            startingContainerIds.remove(containerId);
            log.error("提交 Docker 容器 {} 的 stats 流订阅任务失败", containerName, e);
        }
    }

    private void restartSubscription(String containerId, String containerName) {
        closeSubscriptionOnly(containerId);
        startSubscription(containerId, containerName);
    }

    private void stopSubscription(String containerId) {
        String containerName = containerIdToName.remove(containerId);
        if (StringUtils.isNotBlank(containerName)) {
            containerNameToId.remove(containerName, containerId);
        }
        snapshotsByContainerId.remove(containerId);
        previousSamples.remove(containerId);
        lastReceiveTimes.remove(containerId);
        lastRestartAttempts.remove(containerId);
        restartingContainerIds.remove(containerId);
        closeSubscriptionOnly(containerId);
    }

    private void stopAllSubscriptions() {
        for (String containerId : new ArrayList<String>(subscriptions.keySet())) {
            stopSubscription(containerId);
        }
        subscriptions.clear();
        previousSamples.clear();
        lastReceiveTimes.clear();
        lastRestartAttempts.clear();
        restartingContainerIds.clear();
        snapshotsByContainerId.clear();
        containerIdToName.clear();
        containerNameToId.clear();
        startingContainerIds.clear();
    }

    private void closeSubscriptionOnly(String containerId) {
        StatsStreamCallback callback = subscriptions.get(containerId);
        if (callback != null) {
            closeQuietly(callback);
            subscriptions.remove(containerId, callback);
        }
    }

    private List<Container> loadRunningContainers() {
        try {
            List<Container> containers = dockerClientFactory.getClient().listContainersCmd().withShowAll(false).exec();
            return containers == null ? Collections.<Container>emptyList() : containers;
        } catch (RuntimeException e) {
            log.error("读取 Docker 容器列表失败", e);
            // 返回 null 表示查询失败，与"无运行中容器"（empty list）区分开；
            // 避免因 Docker API 瞬时错误而误清空全部订阅状态
            // 同时触发客户端重置，防止 Docker daemon 重启导致单例连接永久失效
            dockerClientFactory.resetClient();
            return null;
        }
    }

    private DockerMetricsSnapshot resolveFreshSnapshot(DockerMetricsSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        if (System.currentTimeMillis() - snapshot.getLastUpdateTime() <= SNAPSHOT_STALE_MILLIS) {
            // 始终返回防御性副本，防止调用者通过 setter 污染 ConcurrentHashMap 内部缓存
            return snapshot.toBuilder().build();
        }
        return snapshot.toBuilder().available(false).build();
    }

    private void updateSnapshot(String containerId, String containerName, Statistics statistics) {
        // 二次检查：容器已被停止清理，跳过写入防止幽灵数据（与 onNext 的活跃回调检查构成双重防护）
        if (!containerIdToName.containsKey(containerId)) {
            return;
        }
        long now = System.currentTimeMillis();
        PreviousSample currentSample = new PreviousSample(
                readBlkioBytes(statistics, "Read"),
                readBlkioBytes(statistics, "Write"),
                readRxBytes(statistics),
                readTxBytes(statistics),
                now
        );
        PreviousSample previousSample = previousSamples.put(containerId, currentSample);
        double seconds = previousSample == null ? 0D : (now - previousSample.timestamp) / 1000D;
        double readKbPerSecond = previousSample == null ? 0D
                : calculateRateInKb(currentSample.readBytes - previousSample.readBytes, seconds);
        double writeKbPerSecond = previousSample == null ? 0D
                : calculateRateInKb(currentSample.writeBytes - previousSample.writeBytes, seconds);
        double networkDownKbPerSecond = previousSample == null ? 0D
                : calculateRateInKb(currentSample.rxBytes - previousSample.rxBytes, seconds);
        double networkUpKbPerSecond = previousSample == null ? 0D
                : calculateRateInKb(currentSample.txBytes - previousSample.txBytes, seconds);

        DockerMetricsSnapshot snapshot = DockerMetricsSnapshot.builder()
                .containerId(containerId)
                .containerName(containerName)
                .cpuPercent(calculateCpuPercent(statistics))
                .memoryPercent(calculateMemoryPercent(statistics))
                .readKbPerSecond(readKbPerSecond)
                .writeKbPerSecond(writeKbPerSecond)
                .networkDownKbPerSecond(networkDownKbPerSecond)
                .networkUpKbPerSecond(networkUpKbPerSecond)
                .lastUpdateTime(now)
                .available(true)
                .build();
        snapshotsByContainerId.put(containerId, snapshot);
        lastReceiveTimes.put(containerId, now);
    }

    private String resolveContainerName(Container container) {
        if (container == null || container.getNames() == null) {
            return null;
        }
        for (String name : container.getNames()) {
            if (StringUtils.isBlank(name)) {
                continue;
            }
            return StringUtils.removeStart(name.trim(), "/");
        }
        return null;
    }

    private double calculateCpuPercent(Statistics statistics) {
        CpuStatsConfig cpuStats = statistics.getCpuStats();
        CpuStatsConfig preCpuStats = statistics.getPreCpuStats();
        if (cpuStats == null || preCpuStats == null) {
            return 0D;
        }
        CpuUsageConfig cpuUsage = cpuStats.getCpuUsage();
        CpuUsageConfig preCpuUsage = preCpuStats.getCpuUsage();
        if (cpuUsage == null || preCpuUsage == null) {
            return 0D;
        }
        long cpuDelta = safeLong(cpuUsage.getTotalUsage()) - safeLong(preCpuUsage.getTotalUsage());
        long systemDelta = safeLong(cpuStats.getSystemCpuUsage()) - safeLong(preCpuStats.getSystemCpuUsage());
        if (cpuDelta <= 0L || systemDelta <= 0L) {
            return 0D;
        }
        return normalizePercent(cpuDelta * 100D / systemDelta);
    }

    private double calculateMemoryPercent(Statistics statistics) {
        MemoryStatsConfig memoryStats = statistics.getMemoryStats();
        if (memoryStats == null) {
            return 0D;
        }
        long usage = safeLong(memoryStats.getUsage());
        long limit = safeLong(memoryStats.getLimit());
        if (usage <= 0L || limit <= 0L) {
            return 0D;
        }
        return normalizePercent(usage * 100D / limit);
    }

    private double normalizePercent(double percent) {
        if (percent <= 0D) {
            return 0D;
        }
        return Math.min(percent, 100D);
    }

    private long readBlkioBytes(Statistics statistics, String operation) {
        BlkioStatsConfig blkioStats = statistics.getBlkioStats();
        if (blkioStats == null || blkioStats.getIoServiceBytesRecursive() == null) {
            return 0L;
        }
        long totalBytes = 0L;
        for (BlkioStatEntry entry : blkioStats.getIoServiceBytesRecursive()) {
            if (entry == null || !StringUtils.equalsIgnoreCase(entry.getOp(), operation)) {
                continue;
            }
            totalBytes += safeLong(entry.getValue());
        }
        return totalBytes;
    }

    private long readRxBytes(Statistics statistics) {
        long totalRxBytes = 0L;
        for (StatisticNetworksConfig networkStats : resolveNetworkStats(statistics).values()) {
            totalRxBytes += safeLong(networkStats.getRxBytes());
        }
        return totalRxBytes;
    }

    private long readTxBytes(Statistics statistics) {
        long totalTxBytes = 0L;
        for (StatisticNetworksConfig networkStats : resolveNetworkStats(statistics).values()) {
            totalTxBytes += safeLong(networkStats.getTxBytes());
        }
        return totalTxBytes;
    }

    private Map<String, StatisticNetworksConfig> resolveNetworkStats(Statistics statistics) {
        Map<String, StatisticNetworksConfig> networks = statistics.getNetworks();
        if (networks != null && !networks.isEmpty()) {
            return networks;
        }
        Map<String, StatisticNetworksConfig> network = statistics.getNetwork();
        return network != null ? network : Collections.<String, StatisticNetworksConfig>emptyMap();
    }

    private double calculateRateInKb(long deltaBytes, double seconds) {
        if (deltaBytes <= 0L || seconds <= 0D) {
            return 0D;
        }
        return deltaBytes / BYTES_PER_KILOBYTE / seconds;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value.longValue();
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            log.warn("关闭 Docker stats 回调失败", e);
        }
    }

    private class StatsStreamCallback extends ResultCallbackTemplate<StatsStreamCallback, Statistics> {
        private final String containerId;
        private final String initialContainerName;

        private StatsStreamCallback(String containerId, String initialContainerName) {
            this.containerId = containerId;
            this.initialContainerName = initialContainerName;
        }

        @Override
        public void onNext(Statistics object) {
            // 仅当此回调已被其他活跃订阅替换时才跳过处理；
            // 回调尚未注册（null）或仍是当前活跃回调（this）时正常处理，
            // 防止已停止容器的幽灵数据复活到快照缓存
            StatsStreamCallback active = subscriptions.get(containerId);
            if (active != null && active != this) {
                return;
            }
            String containerName = containerIdToName.get(containerId);
            updateSnapshot(containerId, StringUtils.defaultIfBlank(containerName, initialContainerName), object);
        }

        @Override
        public void onError(Throwable throwable) {
            // 仅当此回调仍在订阅映射中时才记录错误并主动清理；
            // 若已被 closeSubscriptionOnly 移除，说明已正常关闭，跳过日志减少噪音
            if (subscriptions.remove(containerId, this)) {
                log.error("Docker 容器 {} 的 stats 流异常终止", initialContainerName, throwable);
            }
            // 父类 ResultCallbackTemplate.onError 内部会调用 close() 释放底层 HTTP 流；
            // 此处重写后必须显式关闭，否则连接池（maxConnections=100）会被耗尽；
            // close() 内部有幂等保护，即使与 closeSubscriptionOnly 并发关闭也不会出错
            closeQuietly(this);
        }

        @Override
        public void onComplete() {
            if (subscriptions.remove(containerId, this)) {
                log.warn("Docker 容器 {} 的 stats 流已结束", initialContainerName);
            }
            closeQuietly(this);
        }
    }

    private static class PreviousSample {
        private final long readBytes;
        private final long writeBytes;
        private final long rxBytes;
        private final long txBytes;
        private final long timestamp;

        private PreviousSample(long readBytes, long writeBytes, long rxBytes, long txBytes, long timestamp) {
            this.readBytes = readBytes;
            this.writeBytes = writeBytes;
            this.rxBytes = rxBytes;
            this.txBytes = txBytes;
            this.timestamp = timestamp;
        }
    }
}
