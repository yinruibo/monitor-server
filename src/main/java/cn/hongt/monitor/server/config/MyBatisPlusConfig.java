package cn.hongt.monitor.server.config;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yrb
 * @date 2021/7/1
 * @Description: mybatis-plus配置类
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 注册 MybatisPlusInterceptor，包含分页、乐观锁、动态表名等拦截器
     * 注意：拦截器顺序会影响执行顺序，DynamicTableNameInnerInterceptor 应在分页之前
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 动态表名（每次SQL执行时动态计算，不要在外面固定）
        Map<String, TableNameHandler> tableNameHandlerMap = new HashMap<>(4);
        tableNameHandlerMap.put("test", (sql, tableName) ->
                tableName + "_" + DateUtil.format(DateUtil.date(), "yyyyMM")
        );
        interceptor.addInnerInterceptor(new DynamicTableNameInnerInterceptor(tableNameHandlerMap));

        // 2. 分页（PostgreSQL）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));

        // 3. 乐观锁
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return interceptor;
    }
}
