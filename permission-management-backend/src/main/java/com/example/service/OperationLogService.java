package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.OperationLog;

/**
 * 操作日志服务接口。
 * 对外统一暴露日志记录入口，避免控制器自己拼装审计字段。
 */
public interface OperationLogService extends IService<OperationLog> {

    /**
     * 记录一次后台操作日志。
     * 会自动补齐当前登录人、请求 IP、操作时间等上下文信息。
     */
    void record(String operation, String method, Object params, boolean success, String errorMessage);
}
