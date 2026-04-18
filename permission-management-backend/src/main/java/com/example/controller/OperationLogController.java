package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.OperationLog;
import com.example.entity.Result;
import com.example.service.OperationLogService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 操作日志控制器。
 * 提供后台操作日志的分页查询和导出能力，方便管理员查看审计记录。
 */
@RestController
@RequestMapping("/api/operation-log")
public class OperationLogController {

    private static final String AUDIT_TYPE_AUTH_CHANGE = "AUTH_CHANGE";
    private static final String AUDIT_TYPE_USER_CHANGE = "USER_CHANGE";
    private static final String AUDIT_TYPE_ORG_CHANGE = "ORG_CHANGE";
    private static final String AUDIT_TYPE_SECURITY = "SECURITY";

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 分页查询操作日志列表。
     * 支持按操作人、操作名称、执行状态、审计分类和时间范围做筛选。
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('user:manage', 'log:view')")
    public Result list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String auditType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        LambdaQueryWrapper<OperationLog> wrapper = buildLogQueryWrapper(username, operation, keyword, auditType, status, startDate, endDate);
        Page<OperationLog> pageParam = new Page<>(page, size);
        return Result.success(operationLogService.page(pageParam, wrapper));
    }

    /**
     * 按当前筛选条件导出日志。
     * 导出能力直接复用列表筛选规则，避免页面看到的数据和导出文件内容不一致。
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('user:manage', 'log:view')")
    public void export(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String auditType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response
    ) throws IOException {
        LambdaQueryWrapper<OperationLog> wrapper = buildLogQueryWrapper(username, operation, keyword, auditType, status, startDate, endDate);
        List<OperationLog> logs = operationLogService.list(wrapper.last("LIMIT 2000"));

        String fileName = "operation-log-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append('\uFEFF');
        csvBuilder.append("操作时间,操作人,操作名称,审计分类,接口,IP,执行结果,错误信息,请求参数\n");
        for (OperationLog log : logs) {
            csvBuilder.append(csvEscape(formatDateTime(log.getCreateTime()))).append(',')
                    .append(csvEscape(log.getUsername())).append(',')
                    .append(csvEscape(log.getOperation())).append(',')
                    .append(csvEscape(resolveAuditTypeLabel(log.getOperation()))).append(',')
                    .append(csvEscape(log.getMethod())).append(',')
                    .append(csvEscape(log.getIp())).append(',')
                    .append(csvEscape(log.getStatus() != null && log.getStatus() == 1 ? "成功" : "失败")).append(',')
                    .append(csvEscape(log.getErrorMessage())).append(',')
                    .append(csvEscape(log.getParams()))
                    .append('\n');
        }

        response.getWriter().write(csvBuilder.toString());
        response.getWriter().flush();
    }

    /**
     * 统一构建日志筛选条件。
     * 列表和导出都走同一套规则，后续新增筛选项时只需要维护这一处。
     */
    private LambdaQueryWrapper<OperationLog> buildLogQueryWrapper(
            String username,
            String operation,
            String keyword,
            String auditType,
            Integer status,
            String startDate,
            String endDate
    ) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            wrapper.like(OperationLog::getUsername, username.trim());
        }
        if (operation != null && !operation.isBlank()) {
            wrapper.like(OperationLog::getOperation, operation.trim());
        }

        /*
         * 审计排查时，很多时候只记得某个关键词，而不是完整的操作名。
         * 这里把关键字搜索同时覆盖到操作名、接口、请求参数和错误信息四个维度，
         * 让“按线索反查日志”这件事更顺手。
         */
        if (keyword != null && !keyword.isBlank()) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(query -> query
                    .like(OperationLog::getOperation, trimmedKeyword)
                    .or()
                    .like(OperationLog::getMethod, trimmedKeyword)
                    .or()
                    .like(OperationLog::getParams, trimmedKeyword)
                    .or()
                    .like(OperationLog::getErrorMessage, trimmedKeyword)
            );
        }

        applyAuditTypeFilter(wrapper, auditType);

        if (status != null) {
            wrapper.eq(OperationLog::getStatus, status);
        }
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(OperationLog::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isBlank()) {
            LocalDateTime endDateTime = LocalDate.parse(endDate).plusDays(1).atStartOfDay();
            wrapper.lt(OperationLog::getCreateTime, endDateTime);
        }
        wrapper.orderByDesc(OperationLog::getCreateTime);
        return wrapper;
    }

    /**
     * 应用专项审计分类筛选。
     * 这里先按操作名称归类，优点是对现有表结构零侵入，也方便快速形成“权限变更专项日志视图”。
     */
    private void applyAuditTypeFilter(LambdaQueryWrapper<OperationLog> wrapper, String auditType) {
        if (auditType == null || auditType.isBlank()) {
            return;
        }

        if (AUDIT_TYPE_AUTH_CHANGE.equals(auditType)) {
            wrapper.and(query -> query
                    .like(OperationLog::getOperation, "分配用户角色")
                    .or()
                    .like(OperationLog::getOperation, "分配角色权限")
                    .or()
                    .like(OperationLog::getOperation, "新增角色")
                    .or()
                    .like(OperationLog::getOperation, "编辑角色")
                    .or()
                    .like(OperationLog::getOperation, "删除角色")
                    .or()
                    .like(OperationLog::getOperation, "新增权限")
                    .or()
                    .like(OperationLog::getOperation, "编辑权限")
                    .or()
                    .like(OperationLog::getOperation, "删除权限")
            );
            return;
        }

        if (AUDIT_TYPE_USER_CHANGE.equals(auditType)) {
            wrapper.and(query -> query
                    .like(OperationLog::getOperation, "新增用户")
                    .or()
                    .like(OperationLog::getOperation, "编辑用户")
                    .or()
                    .like(OperationLog::getOperation, "删除用户")
                    .or()
                    .like(OperationLog::getOperation, "更新用户状态")
            );
            return;
        }

        if (AUDIT_TYPE_ORG_CHANGE.equals(auditType)) {
            wrapper.and(query -> query
                    .like(OperationLog::getOperation, "新增部门")
                    .or()
                    .like(OperationLog::getOperation, "编辑部门")
                    .or()
                    .like(OperationLog::getOperation, "删除部门")
                    .or()
                    .like(OperationLog::getOperation, "新增岗位")
                    .or()
                    .like(OperationLog::getOperation, "编辑岗位")
                    .or()
                    .like(OperationLog::getOperation, "删除岗位")
            );
            return;
        }

        if (AUDIT_TYPE_SECURITY.equals(auditType)) {
            wrapper.and(query -> query
                    .like(OperationLog::getOperation, "重置用户密码")
                    .or()
                    .like(OperationLog::getOperation, "修改个人密码")
                    .or()
                    .like(OperationLog::getOperation, "登录")
                    .or()
                    .like(OperationLog::getOperation, "退出登录")
            );
        }
    }

    /**
     * 把操作名称映射成更适合展示的审计分类标签。
     * 导出文件和前端表格都可以共用这套语义，避免同一条日志在不同页面被解释成两套口径。
     */
    private String resolveAuditTypeLabel(String operation) {
        if (operation == null || operation.isBlank()) {
            return "其他操作";
        }
        if (operation.contains("分配用户角色")
                || operation.contains("分配角色权限")
                || operation.contains("角色")
                || operation.contains("权限")) {
            return "权限变更";
        }
        if (operation.contains("部门") || operation.contains("岗位")) {
            return "组织变更";
        }
        if (operation.contains("密码") || operation.contains("登录") || operation.contains("退出")) {
            return "安全操作";
        }
        if (operation.contains("用户")) {
            return "用户变更";
        }
        return "其他操作";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * CSV 输出时统一做引号转义。
     * 这样即使请求参数里包含逗号、换行或引号，导出文件也能被 Excel 正常打开。
     */
    private String csvEscape(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
