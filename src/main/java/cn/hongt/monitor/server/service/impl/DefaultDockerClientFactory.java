package cn.hongt.monitor.server.service.impl;

import cn.hongt.monitor.server.service.DockerClientFactory;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;

/**
 * 默认的 DockerClient 工厂，复用共享连接池，避免每次采集重复建连。
 */
@Slf4j
@Service
public class DefaultDockerClientFactory implements DockerClientFactory {

    private final Object clientLock = new Object();
    private volatile DockerClient dockerClient;
    private volatile DockerHttpClient dockerHttpClient;

    @Override
    public DockerClient getClient() {
        DockerClient current = dockerClient;
        if (current != null) {
            return current;
        }
        synchronized (clientLock) {
            if (dockerClient == null) {
                DefaultDockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
                dockerHttpClient = new ApacheDockerHttpClient.Builder()
                        .dockerHost(dockerClientConfig.getDockerHost())
                        .sslConfig(dockerClientConfig.getSSLConfig())
                        .maxConnections(100)
                        .connectionTimeout(Duration.ofSeconds(5))
                        .responseTimeout(Duration.ofSeconds(30))
                        .build();
                dockerClient = DockerClientBuilder.getInstance(dockerClientConfig)
                        .withDockerHttpClient(dockerHttpClient)
                        .build();
            }
            return dockerClient;
        }
    }

    @Override
    public void resetClient() {
        // 关闭旧连接并置空引用，迫使下次 getClient() 重新创建；
        // 用于 Docker daemon 重启后恢复连接，避免单例永久失效
        synchronized (clientLock) {
            closeQuietly(dockerClient, "重置 DockerClient 时关闭旧连接失败");
            closeQuietly(dockerHttpClient, "重置 DockerHttpClient 时关闭旧连接失败");
            dockerClient = null;
            dockerHttpClient = null;
        }
    }

    @Override
    public void closeClient() {
        synchronized (clientLock) {
            closeQuietly(dockerClient, "关闭共享 DockerClient 失败");
            closeQuietly(dockerHttpClient, "关闭共享 DockerHttpClient 失败");
            dockerClient = null;
            dockerHttpClient = null;
        }
    }

    private void closeQuietly(Closeable resource, String errorMessage) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (IOException e) {
            log.warn(errorMessage, e);
        }
    }
}
