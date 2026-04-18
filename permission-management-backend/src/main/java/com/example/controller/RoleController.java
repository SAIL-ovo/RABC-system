package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.Result;
import com.example.entity.Role;
import com.example.entity.RolePermission;
import com.example.entity.UserRole;
import com.example.mapper.RoleMapper;
import com.example.mapper.RolePermissionMapper;
import com.example.mapper.UserRoleMapper;
import com.example.service.OperationLogService;
import com.example.service.PermissionService;
import com.example.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private PermissionService permissionService;

    /**
     * 分页查询角色列表。
     * 支持按角色名称和角色编码搜索。
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('role:manage', 'role:view')")
    public Result list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code
    ) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) {
            wrapper.like(Role::getName, name);
        }
        if (code != null && !code.isBlank()) {
            wrapper.like(Role::getCode, code);
        }
        wrapper.eq(Role::getDeleted, 0);

        Page<Role> pageParam = new Page<>(page, size);
        Page<Role> pageResult = roleService.page(pageParam, wrapper);
        fillRolePermissionCounts(pageResult.getRecords());
        return Result.success(pageResult);
    }

    /**
     * 新增角色。
     * 创建前会校验名称、编码必填，以及编码不能重复。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    public Result save(@RequestBody Role role) {
        if (role == null) {
            return logResult("新增角色", "POST /api/role", role, Result.error("角色数据不能为空"));
        }
        if (role.getName() == null || role.getName().trim().isEmpty()) {
            return logResult("新增角色", "POST /api/role", role, Result.error("角色名称不能为空"));
        }
        if (role.getCode() == null || role.getCode().trim().isEmpty()) {
            return logResult("新增角色", "POST /api/role", role, Result.error("角色编码不能为空"));
        }

        boolean codeExists = roleService.lambdaQuery()
                .eq(Role::getCode, role.getCode().trim())
                .eq(Role::getDeleted, 0)
                .count() > 0;
        if (codeExists) {
            return logResult("新增角色", "POST /api/role", role, Result.error("角色编码已存在"));
        }

        Result dataScopeValidation = validateDataScope(role.getDataScope());
        if (dataScopeValidation != null) {
            return logResult("新增角色", "POST /api/role", role, dataScopeValidation);
        }

        role.setName(role.getName().trim());
        role.setCode(role.getCode().trim());
        if (role.getDescription() != null) {
            role.setDescription(role.getDescription().trim());
        }
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        if (role.getDataScope() == null || role.getDataScope().isBlank()) {
            role.setDataScope(Role.DATA_SCOPE_ALL);
        }
        role.setDeleted(0);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());

        return logResult(
                "新增角色",
                "POST /api/role",
                buildRoleLogPayload(role, null),
                roleService.save(role) ? Result.success("角色新增成功") : Result.error("角色新增失败")
        );
    }

    /**
     * 编辑角色。
     * 更新前会校验角色是否存在，以及编码是否与其他角色冲突。
     */
    @PutMapping
    @PreAuthorize("hasAuthority('role:update')")
    public Result update(@RequestBody Role role) {
        if (role == null || role.getId() == null) {
            return logResult("编辑角色", "PUT /api/role", role, Result.error("角色ID不能为空"));
        }
        if (role.getName() == null || role.getName().trim().isEmpty()) {
            return logResult("编辑角色", "PUT /api/role", role, Result.error("角色名称不能为空"));
        }
        if (role.getCode() == null || role.getCode().trim().isEmpty()) {
            return logResult("编辑角色", "PUT /api/role", role, Result.error("角色编码不能为空"));
        }

        Role existingRole = roleService.getById(role.getId());
        if (existingRole == null || Integer.valueOf(1).equals(existingRole.getDeleted())) {
            return logResult("编辑角色", "PUT /api/role", role, Result.error("角色不存在"));
        }

        boolean codeExists = roleService.lambdaQuery()
                .eq(Role::getCode, role.getCode().trim())
                .ne(Role::getId, role.getId())
                .eq(Role::getDeleted, 0)
                .count() > 0;
        if (codeExists) {
            return logResult("编辑角色", "PUT /api/role", role, Result.error("角色编码已存在"));
        }

        Result dataScopeValidation = validateDataScope(role.getDataScope());
        if (dataScopeValidation != null) {
            return logResult("编辑角色", "PUT /api/role", role, dataScopeValidation);
        }

        role.setName(role.getName().trim());
        role.setCode(role.getCode().trim());
        if (role.getDescription() != null) {
            role.setDescription(role.getDescription().trim());
        }
        if (role.getDataScope() == null || role.getDataScope().isBlank()) {
            role.setDataScope(Role.DATA_SCOPE_ALL);
        }

        return logResult(
                "编辑角色",
                "PUT /api/role",
                buildRoleLogPayload(role, existingRole),
                roleService.updateById(role) ? Result.success("角色编辑成功") : Result.error("角色编辑失败")
        );
    }

    /**
     * 删除角色。
     * 如果角色还绑定用户或权限，会直接拦截，避免产生脏关联数据。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public Result delete(@PathVariable Long id) {
        Role existingRole = roleService.getById(id);
        if (existingRole == null || Integer.valueOf(1).equals(existingRole.getDeleted())) {
            return logResult("删除角色", "DELETE /api/role/" + id, id, Result.error("角色不存在"));
        }

        long userBindingCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, id)
        );
        if (userBindingCount > 0) {
            return logResult("删除角色", "DELETE /api/role/" + id, id, Result.error("该角色已分配给用户，无法删除"));
        }

        long permissionBindingCount = rolePermissionMapper.selectCount(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id)
        );
        if (permissionBindingCount > 0) {
            return logResult("删除角色", "DELETE /api/role/" + id, id, Result.error("该角色已绑定权限，请先解除关联"));
        }

        return logResult("删除角色", "DELETE /api/role/" + id, id, roleService.removeById(id) ? Result.success("角色删除成功") : Result.error("角色删除失败"));
    }

    /**
     * 查询角色已绑定的权限 ID 列表。
     * 前端打开“分配权限”弹窗时会用这个接口做默认勾选。
     */
    @GetMapping("/permissions/{roleId}")
    @PreAuthorize("hasAuthority('role:assign-permission')")
    public Result getRolePermissionIds(@PathVariable Long roleId) {
        return Result.success(roleService.getRolePermissionIds(roleId));
    }

    /**
     * 为角色分配权限。
     * 当前策略是覆盖式更新，即先清空再按新列表重新写入。
     */
    @PostMapping("/assign-permission")
    @PreAuthorize("hasAuthority('role:assign-permission')")
    @Transactional(rollbackFor = Exception.class)
    public Result assignPermission(@RequestBody PermissionAssignRequest request) {
        if (request == null || request.getRoleId() == null) {
            return logResult("分配角色权限", "POST /api/role/assign-permission", request, Result.error("角色ID不能为空"));
        }
        Role role = roleService.getById(request.getRoleId());
        if (role == null || Integer.valueOf(1).equals(role.getDeleted())) {
            return logResult("分配角色权限", "POST /api/role/assign-permission", request, Result.error("角色不存在"));
        }

        /*
         * 角色授权属于高价值审计动作。
         * 这里会先校验提交的权限是否都真实存在，再把分配前、分配后以及增删差异一起记录下来，
         * 这样后续排查“某个角色为什么突然能访问某个页面 / 按钮”时会更高效。
         */
        List<Long> requestedPermissionIds = request.getPermissionIds() == null
                ? Collections.emptyList()
                : request.getPermissionIds().stream()
                .filter(permissionId -> permissionId != null)
                .distinct()
                .collect(Collectors.toList());

        List<com.example.entity.Permission> targetPermissions = requestedPermissionIds.isEmpty()
                ? Collections.emptyList()
                : permissionService.lambdaQuery()
                .in(com.example.entity.Permission::getId, requestedPermissionIds)
                .eq(com.example.entity.Permission::getDeleted, 0)
                .list();
        if (targetPermissions.size() != requestedPermissionIds.size()) {
            return logResult("分配角色权限", "POST /api/role/assign-permission", request, Result.error("提交的权限包含无效或已删除项"));
        }

        List<Long> beforePermissionIds = roleService.getRolePermissionIds(request.getRoleId());
        List<com.example.entity.Permission> beforePermissions = beforePermissionIds.isEmpty()
                ? Collections.emptyList()
                : permissionService.lambdaQuery()
                .in(com.example.entity.Permission::getId, beforePermissionIds)
                .eq(com.example.entity.Permission::getDeleted, 0)
                .list();

        roleService.assignPermissions(request.getRoleId(), requestedPermissionIds);
        return logResult(
                "分配角色权限",
                "POST /api/role/assign-permission",
                buildAssignPermissionLogPayload(role, beforePermissions, targetPermissions),
                Result.success("权限分配成功")
        );
    }

    /**
     * 更新角色启用状态。
     * 只改状态字段，适合表格中的快速开关操作。
     */
    @PutMapping("/status")
    @PreAuthorize("hasAuthority('role:status')")
    @Transactional(rollbackFor = Exception.class)
    public Result updateStatus(@RequestBody StatusRequest request) {
        if (request == null || request.getId() == null || request.getStatus() == null) {
            return logResult("更新角色状态", "PUT /api/role/status", request, Result.error("参数不能为空"));
        }

        int affectedRows = roleMapper.update(
                null,
                new LambdaUpdateWrapper<Role>()
                        .eq(Role::getId, request.getId())
                        .set(Role::getStatus, request.getStatus())
        );

        return logResult("更新角色状态", "PUT /api/role/status", request, affectedRows > 0 ? Result.success("状态更新成功") : Result.error("状态更新失败"));
    }

    /**
     * 统一记录角色模块操作日志。
     * 让新增、编辑、删除、分配权限等关键动作都有审计痕迹。
     */
    private Result logResult(String operation, String method, Object params, Result result) {
        boolean success = result != null && result.getCode() != null && result.getCode() == 200;
        operationLogService.record(operation, method, params, success, success ? null : result.getMsg());
        return result;
    }

    /**
     * 组装“角色权限分配”的语义化日志参数。
     * 相比直接记录 permissionId 数组，这里额外保留权限名称、权限编码以及前后差异，
     * 可以显著提升日志的可读性和定位问题时的效率。
     */
    private Map<String, Object> buildAssignPermissionLogPayload(
            Role role,
            List<com.example.entity.Permission> beforePermissions,
            List<com.example.entity.Permission> afterPermissions
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("roleId", role.getId());
        payload.put("roleName", role.getName());
        payload.put("roleCode", role.getCode());
        payload.put("beforePermissionIds", extractPermissionIds(beforePermissions));
        payload.put("beforePermissionNames", extractPermissionNames(beforePermissions));
        payload.put("afterPermissionIds", extractPermissionIds(afterPermissions));
        payload.put("afterPermissionNames", extractPermissionNames(afterPermissions));
        payload.put("afterPermissionCodes", extractPermissionCodes(afterPermissions));
        payload.put("addedPermissionNames", calculatePermissionDifference(afterPermissions, beforePermissions));
        payload.put("removedPermissionNames", calculatePermissionDifference(beforePermissions, afterPermissions));
        return payload;
    }

    /**
     * 提取权限 ID 列表。
     * 这部分主要服务于机器可读的审计对照，便于后续和数据库、导出结果做交叉验证。
     */
    private List<Long> extractPermissionIds(List<com.example.entity.Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }
        return permissions.stream()
                .map(com.example.entity.Permission::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
    }

    /**
     * 提取权限名称列表。
     * 日志在管理后台主要是给人看的，所以名称比纯 ID 更能帮助快速理解变更内容。
     */
    private List<String> extractPermissionNames(List<com.example.entity.Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }
        return permissions.stream()
                .map(com.example.entity.Permission::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * 提取权限编码列表。
     * 当排查前后端权限匹配问题时，code 往往是最稳定、最便于沟通的关键字段。
     */
    private List<String> extractPermissionCodes(List<com.example.entity.Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }
        return permissions.stream()
                .map(com.example.entity.Permission::getCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * 计算权限前后差异。
     * 这里返回的是可读的权限名称集合，便于在日志详情中直接看到“新增了哪些权限、移除了哪些权限”。
     */
    private List<String> calculatePermissionDifference(
            List<com.example.entity.Permission> sourcePermissions,
            List<com.example.entity.Permission> comparePermissions
    ) {
        LinkedHashSet<String> compareNames = comparePermissions == null
                ? new LinkedHashSet<>()
                : comparePermissions.stream()
                .map(com.example.entity.Permission::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (sourcePermissions == null || sourcePermissions.isEmpty()) {
            return Collections.emptyList();
        }

        return sourcePermissions.stream()
                .map(com.example.entity.Permission::getName)
                .filter(name -> name != null && !name.isBlank() && !compareNames.contains(name))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 为角色列表补充“已分配权限数”。
     * 列表页直接读取这个字段，就能更直观地看到每个角色当前的授权规模。
     */
    private void fillRolePermissionCounts(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }

        List<Long> roleIds = roles.stream()
                .map(Role::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return;
        }

        Map<Long, Integer> permissionCountMap = rolePermissionMapper.countPermissionsByRoleIds(roleIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("role_id")).longValue(),
                        row -> ((Number) row.get("permission_count")).intValue(),
                        (left, right) -> right
                ));

        Map<Long, String> permissionSummaryMap = rolePermissionMapper.selectPermissionNamesByRoleIds(roleIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("role_id")).longValue(),
                        row -> buildSummary((String) row.get("permission_names")),
                        (left, right) -> right
                ));

        roles.forEach(role -> {
            role.setPermissionCount(permissionCountMap.getOrDefault(role.getId(), 0));
            role.setPermissionSummary(permissionSummaryMap.getOrDefault(role.getId(), "未分配权限"));
        });
    }

    /**
     * 把聚合后的权限名称整理成适合列表展示的摘要。
     * 当前最多展示前两个权限名，其余数量折叠成 +N，方便快速排查授权范围。
     */
    private String buildSummary(String names) {
        if (names == null || names.isBlank()) {
            return "";
        }

        List<String> items = Arrays.stream(names.split("\\|\\|"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
        if (items.isEmpty()) {
            return "";
        }
        if (items.size() <= 2) {
            return String.join("、", items);
        }
        return String.join("、", items.subList(0, 2)) + " +" + (items.size() - 2);
    }

    /**
     * 组装角色新增/编辑日志参数。
     * 这里把数据范围编码和中文描述一起写入日志，后续看审计记录时不需要再自己翻译 ALL / DEPT 这类值。
     */
    private Map<String, Object> buildRoleLogPayload(Role currentRole, Role previousRole) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", currentRole.getId());
        payload.put("name", currentRole.getName());
        payload.put("code", currentRole.getCode());
        payload.put("description", currentRole.getDescription());
        payload.put("status", currentRole.getStatus());
        payload.put("dataScope", currentRole.getDataScope());
        payload.put("dataScopeLabel", Role.getDataScopeLabel(currentRole.getDataScope()));

        if (previousRole != null) {
            payload.put("beforeDataScope", previousRole.getDataScope());
            payload.put("beforeDataScopeLabel", Role.getDataScopeLabel(previousRole.getDataScope()));
        }
        return payload;
    }

    /**
     * 校验角色数据权限范围是否属于系统允许的标准值。
     * 第一版先固定为 4 种标准范围，避免前后端各自定义导致规则不一致。
     */
    private Result validateDataScope(String dataScope) {
        if (dataScope == null || dataScope.isBlank()) {
            return null;
        }
        if (Role.DATA_SCOPE_ALL.equals(dataScope)
                || Role.DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope)
                || Role.DATA_SCOPE_DEPT.equals(dataScope)
                || Role.DATA_SCOPE_SELF.equals(dataScope)) {
            return null;
        }
        return Result.error("数据权限范围不合法");
    }

    static class StatusRequest {
        private Long id;
        private Integer status;

        /**
         * 返回要更新状态的角色 ID。
         */
        public Long getId() {
            return id;
        }

        /**
         * 设置要更新状态的角色 ID。
         */
        public void setId(Long id) {
            this.id = id;
        }

        /**
         * 返回角色新的启用状态。
         * 约定 1 表示启用，0 表示禁用。
         */
        public Integer getStatus() {
            return status;
        }

        /**
         * 设置角色新的启用状态。
         */
        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    static class PermissionAssignRequest {
        private Long roleId;
        private List<Long> permissionIds;

        /**
         * 返回要分配权限的角色 ID。
         */
        public Long getRoleId() {
            return roleId;
        }

        /**
         * 设置要分配权限的角色 ID。
         */
        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }

        /**
         * 返回本次提交的权限 ID 列表。
         */
        public List<Long> getPermissionIds() {
            return permissionIds;
        }

        /**
         * 设置本次提交的权限 ID 列表。
         */
        public void setPermissionIds(List<Long> permissionIds) {
            this.permissionIds = permissionIds;
        }
    }
}
