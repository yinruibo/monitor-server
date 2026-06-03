package cn.hongt.monitor.server.common.consts;

import cn.hongt.monitor.server.entity.SysDockerDeployEleDO;
import cn.hongt.monitor.server.entity.SysLinuxDeployDO;
import cn.hongt.monitor.server.entity.SysWarnDeployDO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author yrb
 * @date 2021/7/1
 * @Description: 站点常量类
 */
@Slf4j
@Data
@Component
public class StationConst {

    private static final Object LINUX_IP_LOCK = new Object();
    private static final Object CONT_NAMES_LOCK = new Object();

    // 常用数据集合分组条数限制
    public static final Integer PG_LENGTH = 500;
    // 统一定义缺省值数据
    public static final String DEFULT = "999999";


    // redis 数据保存天数（用 long 防止溢出）
    public static final long DATA_TIME_TEN = 1000L * 60 * 60 * 24 * 10;
    // 定时删除任务 获取数据的条数
    public static final String LIMIT_DATA_SERVER = "limit 7 offset 0";
    public static final long DATA_TIME_FIFTEEN = 1000L * 60 * 60 * 24 * 15;

    // 服务器内存
    public static volatile String linuxIP = "";

    // 存储 docker容器名称列表
    public static volatile String contNames = "";
    // 存储 Docker配置表中所有信息
    public static volatile Map<String,SysDockerDeployEleDO> dockerDepMap = new ConcurrentHashMap<>();

    // 存储 告警监控配置表中 所有监控 的所有信息
    public static volatile Map<String,SysWarnDeployDO> warnDepMap = new ConcurrentHashMap<>();


    // 存储 物理网卡名称列表
    public static volatile List<String> netWorkList = Collections.emptyList();
    // 存储 linux 硬件配置表中所有信息
    public static volatile Map<String,SysLinuxDeployDO> linuxDepMap = new ConcurrentHashMap<>();

    public static final String SystemCode = "UTF-8";

    public static String getOrLoadLinuxIp(Supplier<String> loader) {
        String current = linuxIP;
        if (!isBlank(current)) {
            return current;
        }
        synchronized (LINUX_IP_LOCK) {
            if (isBlank(linuxIP)) {
                linuxIP = normalizeText(loader == null ? null : loader.get());
            }
            return linuxIP;
        }
    }

    public static String refreshLinuxIp(String value) {
        synchronized (LINUX_IP_LOCK) {
            linuxIP = normalizeText(value);
            return linuxIP;
        }
    }

    public static String getOrLoadContNames(Supplier<String> loader) {
        String current = contNames;
        if (!isBlank(current)) {
            return current;
        }
        synchronized (CONT_NAMES_LOCK) {
            if (isBlank(contNames)) {
                contNames = normalizeText(loader == null ? null : loader.get());
            }
            return contNames;
        }
    }

    public static String refreshContNames(String value) {
        synchronized (CONT_NAMES_LOCK) {
            contNames = normalizeText(value);
            return contNames;
        }
    }

    // 控制器刷新缓存时使用整张快照替换，避免 clear + put 暴露半更新状态。
    public static void replaceDockerDepMap(Map<String, SysDockerDeployEleDO> snapshot) {
        dockerDepMap = snapshot == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(snapshot);
    }

    // 告警配置和监控线程并发访问，这里统一走原子替换。
    public static void replaceWarnDepMap(Map<String, SysWarnDeployDO> snapshot) {
        warnDepMap = snapshot == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(snapshot);
    }

    // Linux 配置刷新时同样使用快照替换，避免读取线程撞上清空窗口。
    public static void replaceLinuxDepMap(Map<String, SysLinuxDeployDO> snapshot) {
        linuxDepMap = snapshot == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(snapshot);
    }

    // 网卡列表按不可变快照发布，读线程只遍历稳定视图。
    public static void replaceNetWorkList(List<String> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            netWorkList = Collections.emptyList();
            return;
        }
        netWorkList = Collections.unmodifiableList(new ArrayList<>(snapshot));
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
