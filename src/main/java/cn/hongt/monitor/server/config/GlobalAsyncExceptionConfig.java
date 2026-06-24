package cn.hongt.monitor.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import java.lang.reflect.Method;

@Slf4j
@Configuration
public class GlobalAsyncExceptionConfig implements AsyncConfigurer {

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }

    /**
     * 自定义异步异常处理器
     */
    static class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            // 发生异常时，统一打印：异常详情、抛出异常的方法名、当时的入参
            log.error("【全局异步异常拦截】捕获到未处理的异步异常!");
            log.error("方法名称: {}", method.getName());
            log.error("方法参数: {}", params);
            log.error("异常堆栈: ", ex);
        }
    }
}
