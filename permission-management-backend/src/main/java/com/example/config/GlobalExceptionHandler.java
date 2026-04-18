package com.example.config;

import com.example.entity.Result;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 统一处理用户名或密码错误。
     */
    @ExceptionHandler(BadCredentialsException.class)
    public Result handleBadCredentials(BadCredentialsException exception) {
        return new Result(401, "用户名或密码错误", null);
    }

    /**
     * 统一处理账号被禁用。
     */
    @ExceptionHandler(DisabledException.class)
    public Result handleDisabled(DisabledException exception) {
        return new Result(403, "账号已被禁用", null);
    }

    /**
     * 兜底异常处理，避免将堆栈直接暴露给前端。
     */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception exception) {
        String message = exception.getMessage();
        return Result.error(message == null || message.isBlank() ? "服务器内部错误" : message);
    }
}
