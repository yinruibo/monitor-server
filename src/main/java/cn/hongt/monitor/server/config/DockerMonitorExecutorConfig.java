package cn.hongt.monitor.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class DockerMonitorExecutorConfig {

    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);        //  核心线程数
        executor.setMaxPoolSize(8);         //  最大线程数
        executor.setQueueCapacity(1000);    //  线程队列容量
        executor.setKeepAliveSeconds(30);   //  线程空闲时间 30秒
        executor.setThreadNamePrefix("async-task-");
        // CallerRunsPolicy：线程池饱和时由调用者线程（@Scheduled 线程）同步执行，
        // 避免 AbortPolicy 抛异常导致监控数据静默丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(name = "dockerMonitorExecutor")
    public Executor dockerMonitorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("docker-monitor-");
        // CallerRunsPolicy：线程池饱和时由调用者线程（@Scheduled 线程）同步执行，
        // 避免 AbortPolicy 抛异常导致监控数据静默丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(name = "queryMonitorExecutor")
    public Executor queryMonitorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("query-monitor-");
        // CallerRunsPolicy：线程池饱和时由调用者线程（@Scheduled 线程）同步执行，
        // 避免 AbortPolicy 抛异常导致监控数据静默丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(name = "commandExecutor")
    public Executor commandExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("command-");
        // CallerRunsPolicy：线程池饱和时由调用者线程（@Scheduled 线程）同步执行，
        // 避免 AbortPolicy 抛异常导致监控数据静默丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅关闭：等待正在执行的流读取任务完成，避免 StreamReader 线程被中断导致子进程输出丢失
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }
}
