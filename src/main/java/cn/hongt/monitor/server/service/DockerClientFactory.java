package cn.hongt.monitor.server.service;

import com.github.dockerjava.api.DockerClient;

/**
 * DockerClient 工厂，统一收口本机 Docker 连接获取和生命周期管理。
 */
public interface DockerClientFactory {

    DockerClient getClient();

    /**
     * 重置共享客户端，下次 getClient() 时将重新创建连接；
     * 用于 Docker daemon 重启等连接失效场景。
     */
    void resetClient();

    void closeClient();
}
