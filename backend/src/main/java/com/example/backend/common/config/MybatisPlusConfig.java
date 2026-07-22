package com.example.backend.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MyBatis-Plus 公共配置。
 *
 * <p>写法与 demo 保持一致：配置 MySQL 分页拦截器，并开启 Spring 事务管理。
 * 之后待确认列表、统计明细列表都可以直接使用 MyBatis-Plus 的 Page 对象分页。</p>
 */
@Configuration
@EnableTransactionManagement
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 分页拦截器。
     *
     * <p>数据库是 MySQL，所以明确指定 {@link DbType#MYSQL}。没有该配置时，
     * Page 对象不会自动在 SQL 后追加 LIMIT 分页语句。</p>
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
