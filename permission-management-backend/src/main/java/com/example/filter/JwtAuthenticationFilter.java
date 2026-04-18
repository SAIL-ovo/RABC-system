package com.example.filter;

import com.example.entity.User;
import com.example.service.UserService;
import com.example.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

// 推荐：通过构造方法注入依赖（避免 setter 注入时机问题）
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    // 直接通过 final 修饰，确保依赖不为 null
    private final JwtUtils jwtUtils;
    private final UserService userService;

    // 关键新增：注入 Spring 容器中配置好的 ObjectMapper（识别 @JsonFormat 注解）
    @Autowired
    private ObjectMapper objectMapper;

    // 核心：构造方法传入所有依赖 + AuthenticationManager
    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, JwtUtils jwtUtils, UserService userService) {
        super(authenticationManager); // 必须传给父类
        setFilterProcessesUrl("/api/auth/login"); // 指定登录接口路径
        this.jwtUtils = jwtUtils; // 构造方法注入，避免 null
        this.userService = userService; // 构造方法注入，避免 null
    }

    // 处理登录请求（解析用户名密码并认证）
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            // 1. 解析请求体中的用户信息（指定 UTF-8 避免乱码）
            // 这里也改用注入的 ObjectMapper，保持一致性
            User user = objectMapper.readValue(request.getInputStream(), User.class);

            // 2. 验证参数非空
            if (user.getUsername() == null || user.getPassword() == null) {
                throw new RuntimeException("用户名或密码不能为空");
            }

            // 3. 创建认证 Token 并调用 AuthenticationManager 认证
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    user.getUsername(),
                    user.getPassword()
            );

            return getAuthenticationManager().authenticate(authToken);
        } catch (IOException e) {
            throw new RuntimeException("解析登录请求失败：" + e.getMessage());
        }
    }

    // 登录成功：生成 JWT 并返回
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        // 1. 设置跨域 + 编码响应头（关键！）
        setResponseHeaders(response);

        // 2. 获取认证后的用户信息
        org.springframework.security.core.userdetails.User authUser =
                (org.springframework.security.core.userdetails.User) authResult.getPrincipal();
        User dbUser = userService.findByUsername(authUser.getUsername());

        // 3. 生成 JWT Token
        String token = jwtUtils.generateToken(authUser.getUsername(), dbUser.getId());

        // 4. 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "登录成功");
        result.put("token", token);
        result.put("user", dbUser);

        // 5. 关键修改：用注入的 ObjectMapper（识别 @JsonFormat），替代 new ObjectMapper()
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    // 登录失败：返回错误信息
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        // 1. 设置跨域 + 编码响应头
        setResponseHeaders(response);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 状态码

        // 2. 构建错误结果
        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", "用户名或密码错误");

        // 3. 关键修改：用注入的 ObjectMapper
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    // 通用：设置跨域 + 编码响应头
    private void setResponseHeaders(HttpServletResponse response) {
        // 跨域配置（前后端分离必需）
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        // 编码 + 内容类型
        response.setContentType("application/json;charset=" + StandardCharsets.UTF_8.name());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    }
}