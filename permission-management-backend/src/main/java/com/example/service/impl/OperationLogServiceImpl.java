package com.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.OperationLog;
import com.example.entity.User;
import com.example.mapper.OperationLogMapper;
import com.example.service.OperationLogService;
import com.example.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志服务实现。
 * 负责把控制器中的业务动作统一落库，方便后续审计和问题追踪。
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    private static final Logger logger = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    /**
     * 记录一次后台操作日志。
     * 如果当前没有登录上下文，也会尽量把请求信息补齐后落库。
     */
    @Override
    public void record(String operation, String method, Object params, boolean success, String errorMessage) {
        try {
            OperationLog log = new OperationLog();
            log.setOperation(operation);
            log.setMethod(method);
            log.setParams(serializeParams(params));
            log.setIp(resolveRequestIp());
            log.setStatus(success ? 1 : 0);
            log.setErrorMessage(truncate(errorMessage, 1000));
            log.setCreateTime(LocalDateTime.now());

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getName() != null && !"anonymousUser".equals(authentication.getName())) {
                log.setUsername(authentication.getName());
                User operator = userService.findByUsername(authentication.getName());
                if (operator != null) {
                    log.setUserId(operator.getId());
                }
            }

            save(log);
        } catch (Exception exception) {
            // 日志记录失败不能反向影响主业务，否则会出现“业务成功但因为审计缺表导致整体失败”的问题。
            logger.error("记录操作日志失败，不影响主业务继续执行", exception);
        }
    }

    /**
     * 把请求参数序列化为 JSON。
     * 这里会顺手把 password 字段打码，避免把敏感信息写进日志。
     */
    private String serializeParams(Object params) {
        if (params == null) {
            return null;
        }

        try {
            String json = objectMapper.writeValueAsString(params);
            return truncate(maskSensitiveFields(json), 4000);
        } catch (JsonProcessingException ignored) {
            return truncate(maskSensitiveFields(String.valueOf(params)), 4000);
        }
    }

    /**
     * 从当前请求上下文中提取客户端 IP。
     * 优先读取常见代理头，拿不到时再回退到 remoteAddr。
     */
    private String resolveRequestIp() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }

        HttpServletRequest request = servletAttributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return truncate(forwardedFor.split(",")[0].trim(), 50);
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return truncate(realIp.trim(), 50);
        }

        return truncate(request.getRemoteAddr(), 50);
    }

    /**
     * 对敏感字段做简单脱敏。
     * 当前先覆盖 password，后续如果增加更多敏感键可以继续扩展。
     */
    private String maskSensitiveFields(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        return content.replaceAll("(?i)(\"password\"\\s*:\\s*\")[^\"]*(\")", "$1******$2");
    }

    /**
     * 控制字符串长度，避免日志字段超长导致入库失败。
     */
    private String truncate(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength);
    }
}
