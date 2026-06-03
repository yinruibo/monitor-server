package cn.hongt.monitor.server.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Docker 容器最新监控快照，供定时告警直接读取。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DockerMetricsSnapshot {

    private String containerId;
    private String containerName;
    private double cpuPercent;
    private double memoryPercent;
    private double readKbPerSecond;
    private double writeKbPerSecond;
    private double networkUpKbPerSecond;
    private double networkDownKbPerSecond;
    private long lastUpdateTime;
    private boolean available;
}
