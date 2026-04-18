package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.entity.Department;
import com.example.entity.OperationLog;
import com.example.entity.Permission;
import com.example.entity.Post;
import com.example.entity.Result;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.service.DepartmentService;
import com.example.service.OperationLogService;
import com.example.service.PermissionService;
import com.example.service.PostService;
import com.example.service.RoleService;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页仪表盘控制器。
 * 用最直观的统计和近期动态，把当前系统的组织、权限和审计能力集中展示出来。
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private PostService postService;

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 首页仪表盘数据。
     * 统计卡片、近 7 天日志趋势、权限变更摘要和最近动态都在这里一次性返回。
     */
    @GetMapping("/overview")
    public Result overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", buildSummary());
        data.put("recentLogs", buildRecentLogs());
        data.put("auditTrend", buildAuditTrend());
        data.put("authHighlights", buildAuthHighlights());
        return Result.success(data);
    }

    private Map<String, Object> buildSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("userCount", userService.lambdaQuery().eq(User::getDeleted, 0).count());
        summary.put("roleCount", roleService.lambdaQuery().eq(Role::getDeleted, 0).count());
        summary.put("permissionCount", permissionService.lambdaQuery().eq(Permission::getDeleted, 0).count());
        summary.put("departmentCount", departmentService.lambdaQuery().eq(Department::getDeleted, 0).count());
        summary.put("postCount", postService.lambdaQuery().eq(Post::getDeleted, 0).count());
        summary.put("logCount", operationLogService.count());
        return summary;
    }

    /**
     * 取最近几条动态，首页可以直接看出系统最近被改动了什么。
     */
    private List<Map<String, Object>> buildRecentLogs() {
        return operationLogService.lambdaQuery()
                .orderByDesc(OperationLog::getCreateTime)
                .last("LIMIT 8")
                .list()
                .stream()
                .map(log -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", log.getId());
                    item.put("username", log.getUsername());
                    item.put("operation", log.getOperation());
                    item.put("status", log.getStatus());
                    item.put("auditType", resolveAuditTypeLabel(log.getOperation()));
                    item.put("createTime", formatDateTime(log.getCreateTime()));
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * 统计近 7 天每天的日志量和权限变更量。
     * 首页不需要特别复杂的图表数据，按天聚合就足够支撑演示。
     */
    private List<Map<String, Object>> buildAuditTrend() {
        List<OperationLog> recentLogs = operationLogService.lambdaQuery()
                .ge(OperationLog::getCreateTime, LocalDateTime.now().minusDays(6).toLocalDate().atStartOfDay())
                .orderByAsc(OperationLog::getCreateTime)
                .list();

        return java.util.stream.IntStream.rangeClosed(0, 6)
                .mapToObj(offset -> {
                    /*
                     * 这里改成“今天 -> 6天前”的倒序输出。
                     * 首页趋势卡片更偏向运营视角，最近一天放在最前面时，管理员一眼就能先看到当前最新状态，
                     * 不需要先滚到列表底部再看今天的数据。
                     */
                    LocalDateTime dayStart = LocalDateTime.now().minusDays(offset).toLocalDate().atStartOfDay();
                    LocalDateTime dayEnd = dayStart.plusDays(1);

                    long total = recentLogs.stream()
                            .filter(log -> log.getCreateTime() != null
                                    && !log.getCreateTime().isBefore(dayStart)
                                    && log.getCreateTime().isBefore(dayEnd))
                            .count();
                    long authChange = recentLogs.stream()
                            .filter(log -> log.getCreateTime() != null
                                    && !log.getCreateTime().isBefore(dayStart)
                                    && log.getCreateTime().isBefore(dayEnd)
                                    && "权限变更".equals(resolveAuditTypeLabel(log.getOperation())))
                            .count();

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("date", dayStart.format(DateTimeFormatter.ofPattern("MM-dd")));
                    item.put("total", total);
                    item.put("authChange", authChange);
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * 把权限治理相关的亮点单独提炼出来，方便首页直接讲清这个项目的核心卖点。
     */
    private Map<String, Object> buildAuthHighlights() {
        Map<String, Object> highlights = new LinkedHashMap<>();

        long authChangeCount = operationLogService.count(new LambdaQueryWrapper<OperationLog>()
                .and(query -> query
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
                ));

        long enabledPostCount = postService.lambdaQuery()
                .eq(Post::getDeleted, 0)
                .eq(Post::getStatus, 1)
                .count();

        long departmentLeaderCount = departmentService.lambdaQuery()
                .eq(Department::getDeleted, 0)
                .isNotNull(Department::getLeaderUserId)
                .count();

        highlights.put("authChangeCount", authChangeCount);
        highlights.put("enabledPostCount", enabledPostCount);
        highlights.put("departmentLeaderCount", departmentLeaderCount);
        highlights.put("dataScopeTypes", List.of("全部数据", "本部门及下级", "仅本部门", "仅本人"));
        return highlights;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

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
}
