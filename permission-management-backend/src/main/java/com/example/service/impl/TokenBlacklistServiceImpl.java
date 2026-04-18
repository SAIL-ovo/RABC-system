package com.example.service.impl;

import com.example.service.TokenBlacklistService;
import com.example.utils.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

/**
 * Token 黑名单服务实现。
 * 这里使用 Redis 维护“已主动退出”的 JWT 集合，从而弥补纯无状态 JWT 无法主动失效的问题。
 */
@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "auth:token:blacklist:";
    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistServiceImpl.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 把 token 加入黑名单。
     * 这里不会设置固定 TTL，而是精确使用 token 剩余有效时间，
     * 让 Redis 里的黑名单键与 JWT 生命周期保持一致。
     */
    @Override
    public void blacklistToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        try {
            Date expirationDate = jwtUtils.getExpirationDateFromToken(token);
            long ttlMillis = expirationDate.getTime() - System.currentTimeMillis();
            if (ttlMillis <= 0) {
                return;
            }

            stringRedisTemplate.opsForValue().set(buildBlacklistKey(token), "1", Duration.ofMillis(ttlMillis));
        } catch (Exception exception) {
            /*
             * Redis 在当前项目里属于认证增强能力，而不是登录主链路的绝对前提。
             * 所以这里采用“失败降级”策略：如果 Redis 暂时不可用，只记录告警，不反向影响退出接口本身。
             * 这样至少不会出现因为 Redis 故障，连正常的前后端退出流程都被拖垮的情况。
             */
            logger.warn("写入 token 黑名单失败，本次退出将退化为仅清理前端本地状态", exception);
        }
    }

    /**
     * 检查 token 是否已经被主动拉黑。
     * 只要 Redis 中还存在对应键，就说明该 token 即使签名合法、仍在有效期内，也不允许继续使用。
     */
    @Override
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(buildBlacklistKey(token)));
        } catch (Exception exception) {
            /*
             * 这里必须明确做失败降级。
             * 如果 Redis 查询失败时直接把异常抛给过滤器，过滤器就拿不到认证结果，最终会把所有已登录请求都打成 401。
             * 对当前系统来说，更合理的策略是：Redis 不可用时暂时跳过黑名单校验，但保留 JWT 的签名、过期时间和用户状态校验。
             */
            logger.warn("查询 token 黑名单失败，已降级为忽略黑名单校验", exception);
            return false;
        }
    }

    /**
     * 统一生成黑名单键名。
     * 这样后续无论是排查问题、做监控还是扩展认证相关数据，都能很容易识别这一类键。
     */
    private String buildBlacklistKey(String token) {
        return BLACKLIST_PREFIX + token;
    }
}
