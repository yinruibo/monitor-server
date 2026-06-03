package cn.hongt.monitor.server.common.consts;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author yrb
 * @date 2021/7/1
 * @Description: 基础常量
 *
 * 注意：static 字段通过 setter 注入，加 volatile 保证多线程可见性。
 * 如配合 Nacos @RefreshScope 动态刷新，刷新时其他线程能及时读到新值。
 */
@Slf4j
@Data
@Component
@RefreshScope
public class Const {

    // volatile 保证：Spring 容器写入后，@Async / 定时任务线程能立即可见
    public static volatile String warnRecordPath;
    public static volatile String cpumoryIOUrl;
    public static volatile String networkipUrl;
    public static volatile String diskPaths;
    public static volatile String networkCardsUrl;

    @Value("${datacenter.monitor.warnRecord.path}")
    public void setwarnRecordPath(String warnRecordPath) {
        Const.warnRecordPath = warnRecordPath;
    }

    @Value("${datacenter.monitor.disk.path}")
    public void setdiskPath(String diskPaths) {
        Const.diskPaths = diskPaths;
    }

    @Value("${datacenter.monitor.cpumory.path}")
    public void setCpumoryUrl(String cpumoryIOUrl) {
        Const.cpumoryIOUrl = cpumoryIOUrl;
    }

    @Value("${datacenter.monitor.network.ip}")
    public void setNetworkipUrl(String networkipUrl) {
        Const.networkipUrl = networkipUrl;
    }

    @Value("${datacenter.monitor.network.cards}")
    public void setNetworkCardsUrl(String networkCardsUrl) {
        Const.networkCardsUrl = networkCardsUrl;
    }
}
