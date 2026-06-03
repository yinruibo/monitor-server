package cn.hongt.monitor.server.service;

import java.util.List;
import java.util.Map;

/**
 * Docker 指标采集服务，后台维护实时快照，业务层只读缓存。
 */
public interface DockerMetricCollectorService {

    List<String> listRunningContainerNames();

    // 获取 Docker 容器监控快照 -- 快照每1秒更新一次，只存最新数据
    DockerMetricsSnapshot getSnapshotByContainerName(String containerName);

    void refreshContainerSubscriptions();
}
