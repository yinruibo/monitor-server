package cn.hongt.monitor.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerMonitorExecutorConfigTest {

    @Test
    void config_shouldProvideDefaultAsyncTaskExecutor() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DockerMonitorExecutorConfig.class);
        try {
            assertTrue(context.containsBean("taskExecutor"));
            Executor dockerMonitorExecutor = context.getBean("dockerMonitorExecutor", Executor.class);
            ThreadPoolTaskExecutor taskExecutor = context.getBean("taskExecutor", ThreadPoolTaskExecutor.class);
            assertNotSame(dockerMonitorExecutor, taskExecutor);
        } finally {
            context.close();
        }
    }
}
