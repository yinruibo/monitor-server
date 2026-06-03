package cn.hongt.monitor.server.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.parser.ISqlParser;
import com.baomidou.mybatisplus.extension.parsers.DynamicTableNameParser;
import com.baomidou.mybatisplus.extension.parsers.ITableNameHandler;
import com.baomidou.mybatisplus.extension.plugins.OptimisticLockerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author huangyanwen
 * @date 2021/7/1
 * @Description: mybatis-plus配置类
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * 分页插件,注册到ioc容器中
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
//        return new PaginationInterceptor();
        PaginationInterceptor page = new PaginationInterceptor();
        //设置方言类型
//        page.setDialectType("mysql");

        //动态表名
        List<ISqlParser> sqlParserList = CollUtil.newArrayList();
        DynamicTableNameParser dynamicTableNameParser = new DynamicTableNameParser();
        Map<String, ITableNameHandler> tableNameHandlerMap = new HashMap<>(16);
        //拿到当前时间得年月
        String year = DateUtil.format(DateUtil.date(), "yyyyMM");
        /**
         * 按照时间逻辑分表
         * @param metaObject 元对象
         * @param sql        当前执行 SQL
         * @param tableName  表名
         * lambda表示式的三个参数
         * @return 动态表名  为当前表名_当前年月
         */
        tableNameHandlerMap.put("test", (m, s, tn) -> tn + "_" + year);
        //将表名的映射集合加回到PaginationInterceptor对象
        dynamicTableNameParser.setTableNameHandlerMap(tableNameHandlerMap);
        sqlParserList.add(dynamicTableNameParser);
        page.setSqlParserList(sqlParserList);

        return page;
    }

    //注册乐观锁插件
    @Bean
    public OptimisticLockerInterceptor optimisticLockerInterceptor(){
        return new OptimisticLockerInterceptor();
    }
}
