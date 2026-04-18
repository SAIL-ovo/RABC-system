package com.example.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 基础配置。
 * 当前主要补充分页拦截器，保证分页查询时 records 和 total 都能被正确回填。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 分页拦截器。
     * 没有这一层时，分页列表虽然能查到数据，但 total 总数不会被正确统计。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
