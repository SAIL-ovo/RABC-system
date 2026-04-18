package com.example.filter;

import com.example.entity.User;
import com.example.service.TokenBlacklistService;
import com.example.service.UserService;
import com.example.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 授权过滤器：验证请求头中的 Token，为合法请求设置 Spring Security 认证信息。
 * 这一版额外接入了 Redis 黑名单能力，用于实现“退出登录后 token 立即失效”。
 */
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthorizationFilter.class);

    private JwtUtils jwtUtils;
    private UserService userService;
    private TokenBlacklistService tokenBlacklistService;

    public JwtAuthorizationFilter() {
    }

    /**
     * 使用构造方法集中注入依赖。
     * 黑名单服务也放进来，是为了让过滤器能够在校验 JWT 合法性的同时，
     * 继续判断它是否已经被用户主动注销。
     */
    public JwtAuthorizationFilter(JwtUtils jwtUtils, UserService userService, TokenBlacklistService tokenBlacklistService) {
        this.jwtUtils = jwtUtils;
        this.userService = userService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public void setJwtUtils(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setTokenBlacklistService(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if ("/api/auth/login".equals(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        token = token.substring(7).trim();

        try {
            if (jwtUtils == null || userService == null) {
                logger.warn("JwtUtils或UserService未注入，跳过Token校验");
                chain.doFilter(request, response);
                return;
            }

            if (!jwtUtils.validateToken(token)) {
                logger.warn("Token无效或已过期: {}", token);
                chain.doFilter(request, response);
                return;
            }

            /*
             * 这里额外增加 Redis 黑名单校验。
             * 纯 JWT 只会校验签名和过期时间，无法感知“用户已经主动退出”这一业务事实，
             * 所以需要借助 Redis 记录已失效 token，命中后直接拒绝继续建立认证上下文。
             */
            if (tokenBlacklistService != null && tokenBlacklistService.isBlacklisted(token)) {
                logger.warn("Token 已加入黑名单，拒绝继续访问: {}", token);
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            String username = jwtUtils.getUsernameFromToken(token);
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (username == null || userId == null) {
                logger.warn("Token中未解析到有效用户信息");
                chain.doFilter(request, response);
                return;
            }

            User currentUser = userService.getById(userId);
            if (currentUser == null || currentUser.getStatus() == null || currentUser.getStatus() != 1) {
                logger.warn("用户已被禁用或不存在，拒绝使用旧Token访问: userId={}", userId);
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            List<String> roles = userService.getUserRoles(userId);
            List<String> permissions = userService.getUserPermissions(userId);
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            if (roles != null) {
                authorities.addAll(roles.stream()
                        .distinct()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList()));
            }
            if (permissions != null) {
                authorities.addAll(permissions.stream()
                        .distinct()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()));
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception exception) {
            logger.error("Token校验过程中发生异常", exception);
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/api/auth/login".equals(request.getRequestURI());
    }
}
