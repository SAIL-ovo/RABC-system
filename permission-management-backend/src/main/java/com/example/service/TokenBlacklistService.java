package com.example.service;

/**
 * Token 黑名单服务接口。
 * 负责把主动退出登录后的 JWT 记入 Redis，并在后续请求时判断该 token 是否已经失效。
 */
public interface TokenBlacklistService {

    /**
     * 将 token 写入黑名单。
     * 实现层会根据 token 自身剩余有效期自动设置 Redis 过期时间，
     * 这样黑名单数据会随着 JWT 过期一起自动清理。
     */
    void blacklistToken(String token);

    /**
     * 判断 token 是否命中黑名单。
     * 只要命中黑名单，就说明这是一个已主动作废的 token，不允许再继续访问受保护资源。
     */
    boolean isBlacklisted(String token);
}
