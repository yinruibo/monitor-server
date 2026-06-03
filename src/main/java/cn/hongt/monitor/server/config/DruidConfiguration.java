package cn.hongt.monitor.server.config;

import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DruidConfiguration {

    @Bean
    public ServletRegistrationBean<StatViewServlet> statViewServlet() {
        ServletRegistrationBean<StatViewServlet> registrationBean = new ServletRegistrationBean(new StatViewServlet());
        // 配置初始化参数 -- 默认免登录
//        registrationBean.addInitParameter("loginUsername", "admin");
//        registrationBean.addInitParameter("loginPassword", "123456");
        // IP白名单 -- 默认均可访问
//        registrationBean.addInitParameter("allow", "192.168.*");
        // IP黑名单(共同存在时，deny优先于allow) -- 默认均可访问
//        registrationBean.addInitParameter("deny", "192.168.1.100");
        //是否能够重置数据 禁用HTML页面上的“Reset All”功能
        registrationBean.addInitParameter("resetEnable", "true");
        // 需要监控的 url路径
        registrationBean.addUrlMappings("/druid/*");
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean filterRegistrationBean() {

        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(new WebStatFilter());
        // 需要监控的 url
        filterRegistrationBean.addUrlPatterns("/*");
        // 排除一些静态资源，以提高效率
        filterRegistrationBean.addInitParameter("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
        return filterRegistrationBean;
    }
}

