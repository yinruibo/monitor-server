package cn.hongt.monitor.server;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@EnableAsync
@EnableScheduling
@SpringBootApplication
@MapperScan(basePackages = "cn.hongt.monitor.server.mapper")//扫描 mybatis repository 包路径
@ComponentScan(value = {"cn.hongt.monitor.server.*"})
public class MonitorServerApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(MonitorServerApplication.class);
    }

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext ctx = SpringApplication.run(MonitorServerApplication.class, args);
        InetAddress addr = InetAddress.getLocalHost();
        String ip = addr.getHostAddress();
        String port = ctx.getEnvironment().getProperty("server.port");
        log.info(String.format("项目接口路径：http://%s:%s/doc.html", ip, port));
        log.info(String.format("德鲁伊连接池Url路径：http://%s:%s/druid/index.html", ip, port));
    }
}
