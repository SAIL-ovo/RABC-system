package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 基础配置。
 * 当前系统主要用 Redis 存储字符串型的认证辅助数据，例如 JWT 黑名单键和值，
 * 先提供 StringRedisTemplate 就能满足目前需求，也方便后续继续扩展验证码、限流计数等能力。
 */
@Configuration
public class RedisConfig {

    /**
     * 注册字符串模板。
     * 统一使用 StringRedisTemplate 可以避免各业务模块各自处理序列化细节，
     * 对当前权限系统这种以简单键值对为主的场景最直接、最稳定。
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
