package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.AssignRoleRequest;
import com.example.entity.Department;
import com.example.entity.Post;
import com.example.entity.Result;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.mapper.UserRoleMapper;
import com.example.service.DepartmentService;
import com.example.service.OperationLogService;
import com.example.service.PostService;
import com.example.service.RoleService;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private PostService postService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private RoleService roleService;

    /**
     * 分页查询用户列表。
     * 这里除了基础分页和搜索外，还会补充角色摘要、部门名称和岗位名称，避免前端再做多次映射拼装。
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('user:manage', 'user:view')")
    public Result list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName
    ) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            wrapper.like(User::getUsername, username.trim());
        }
        if (realName != null && !realName.isBlank()) {
            wrapper.like(User::getRealName, realName.trim());
        }
        wrapper.eq(User::getDeleted, 0);
        applyDataScope(wrapper);

        Page<User> pageResult = userService.page(new Page<>(page, size), wrapper);
        fillUserRoleCounts(pageResult.getRecords());
        fillUserDepartmentNames(pageResult.getRecords());
        fillUserPostNames(pageResult.getRecords());
        return Result.success(pageResult);
    }

    /**
     * 查询指定用户当前已绑定的角色 ID 列表。
     * 用户分配角色弹窗会用这份数据做回显。
     */
    @GetMapping("/roles/{userId}")
    @PreAuthorize("hasAuthority('user:assign-role')")
    public Result getUserRoles(@PathVariable Long userId) {
        try {
            return Result.success(userRoleMapper.selectUserRoleIds(userId));
        } catch (Exception exception) {
            return Result.success(Collections.emptyList());
        }
    }

    /**
     * 查询用户组织画像详情。
     * 这里把部门、岗位、负责人、角色和数据范围一次性组装好，方便前端直接做详情展示和项目演示。
     */
    @GetMapping("/profile/{userId}")
    @PreAuthorize("hasAnyAuthority('user:manage', 'user:view')")
    public Result profile(@PathVariable Long userId) {
        User user = userService.getById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            return Result.error("用户不存在");
        }

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("realName", user.getRealName());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        profile.put("status", user.getStatus());
        profile.put("createTime", user.getCreateTime());
        profile.put("updateTime", user.getUpdateTime());

        Department department = user.getDepartmentId() == null ? null : departmentService.getById(user.getDepartmentId());
        if (department != null && !Integer.valueOf(1).equals(department.getDeleted())) {
            profile.put("departmentId", department.getId());
            profile.put("departmentName", department.getName());
            profile.put("departmentPhone", department.getPhone());
            profile.put("departmentLeaderName", department.getLeader());
            profile.put("departmentLeaderUserId", department.getLeaderUserId());
        } else {
            profile.put("departmentId", null);
            profile.put("departmentName", "未分配部门");
            profile.put("departmentPhone", null);
            profile.put("departmentLeaderName", "未设置负责人");
            profile.put("departmentLeaderUserId", null);
        }

        Post post = user.getPostId() == null ? null : postService.getById(user.getPostId());
        if (post != null && !Integer.valueOf(1).equals(post.getDeleted())) {
            profile.put("postId", post.getId());
            profile.put("postName", post.getName());
            profile.put("postCode", post.getCode());
            profile.put("postRemark", post.getRemark());

            Department postDepartment = post.getDepartmentId() == null ? null : departmentService.getById(post.getDepartmentId());
            profile.put("postDepartmentName", postDepartment == null ? null : postDepartment.getName());

            List<Long> postDefaultRoleIds = postService.getDefaultRoleIds(post.getId());
            List<Role> postDefaultRoles = postDefaultRoleIds.isEmpty()
                    ? Collections.emptyList()
                    : roleService.lambdaQuery()
                    .in(Role::getId, postDefaultRoleIds)
                    .eq(Role::getDeleted, 0)
                    .list();
            profile.put("postDefaultRoleNames", extractRoleNames(postDefaultRoles));
        } else {
            profile.put("postId", null);
            profile.put("postName", "未分配岗位");
            profile.put("postCode", null);
            profile.put("postRemark", null);
            profile.put("postDepartmentName", null);
            profile.put("postDefaultRoleNames", Collections.emptyList());
        }

        List<Role> roles = roleService.getRolesByUserId(userId);
        profile.put("roleIds", extractRoleIds(roles));
        profile.put("roleNames", extractRoleNames(roles));
        profile.put("roleCodes", roles.stream()
                .map(Role::getCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toList()));
        profile.put("roleCount", roles.size());

        String effectiveDataScope = roleService.resolveHighestDataScope(roles);
        profile.put("effectiveDataScope", effectiveDataScope);
        profile.put("effectiveDataScopeLabel", Role.getDataScopeLabel(effectiveDataScope));
        return Result.success(profile);
    }

    /**
     * 覆盖式分配用户角色。
     * 提交的新角色会替换原有角色绑定，并把前后差异写入审计日志，方便后续追查“谁改了谁的权限边界”。
     */
    @PostMapping("/assign-role")
    @PreAuthorize("hasAuthority('user:assign-role')")
    @Transactional(rollbackFor = Exception.class)
    public Result assignRole(@RequestBody AssignRoleRequest request) {
        if (request == null || request.getUserId() == null) {
            return logResult("分配用户角色", "POST /api/user/assign-role", request, Result.error("用户ID不能为空"));
        }

        User targetUser = userService.getById(request.getUserId());
        if (targetUser == null || Integer.valueOf(1).equals(targetUser.getDeleted())) {
            return logResult("分配用户角色", "POST /api/user/assign-role", request, Result.error("用户不存在"));
        }

        List<Long> roleIds = request.getRoleIds();
        if (roleIds == null || roleIds.isEmpty()) {
            return logResult("分配用户角色", "POST /api/user/assign-role", request, Result.error("请至少选择一个角色"));
        }

        /*
         * 角色分配是权限治理里最敏感的操作之一。
         * 这里除了执行覆盖写入，还会把分配前后角色名称和差异项一起记录下来，
         * 让日志既能给人看，也方便后续做导出或审计分析。
         */
        List<Long> distinctRoleIds = roleIds.stream()
                .filter(roleId -> roleId != null)
                .distinct()
                .collect(Collectors.toList());
        if (distinctRoleIds.isEmpty()) {
            return logResult("分配用户角色", "POST /api/user/assign-role", request, Result.error("请至少选择一个有效角色"));
        }

        List<Role> targetRoles = roleService.lambdaQuery()
                .in(Role::getId, distinctRoleIds)
                .eq(Role::getDeleted, 0)
                .list();
        if (targetRoles.size() != distinctRoleIds.size()) {
            return logResult("分配用户角色", "POST /api/user/assign-role", request, Result.error("提交的角色包含无效或已删除项"));
        }

        List<Long> beforeRoleIds = userRoleMapper.selectUserRoleIds(request.getUserId());
        List<Role> beforeRoles = beforeRoleIds.isEmpty()
                ? Collections.emptyList()
                : roleService.lambdaQuery()
                .in(Role::getId, beforeRoleIds)
                .eq(Role::getDeleted, 0)
                .list();

        userService.assignRole(request.getUserId(), distinctRoleIds);
        return logResult(
                "分配用户角色",
                "POST /api/user/assign-role",
                buildAssignRoleLogPayload(targetUser, beforeRoles, targetRoles),
                Result.success("角色分配成功")
        );
    }

    /**
     * 新增用户。
     * 第一版组织模型里会同时校验部门和岗位绑定，确保用户资料里不会写入无效的组织引用。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public Result save(@RequestBody User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            return logResult("新增用户", "POST /api/user", user, Result.error("用户名不能为空"));
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            return logResult("新增用户", "POST /api/user", user, Result.error("密码不能为空"));
        }
        if (userService.findByUsername(user.getUsername()) != null) {
            return logResult("新增用户", "POST /api/user", user, Result.error("用户名已存在"));
        }

        Result departmentValidation = validateDepartmentBinding(user.getDepartmentId());
        if (departmentValidation != null) {
            return logResult("新增用户", "POST /api/user", user, departmentValidation);
        }

        Result postValidation = validatePostBinding(user.getDepartmentId(), user.getPostId());
        if (postValidation != null) {
            return logResult("新增用户", "POST /api/user", user, postValidation);
        }

        Result roleValidation = validateRoleBindings(user.getRoleIds());
        if (roleValidation != null) {
            return logResult("新增用户", "POST /api/user", user, roleValidation);
        }

        boolean success = userService.createUser(user);
        return logResult("新增用户", "POST /api/user", user, success ? Result.success("新增用户成功") : Result.error("新增用户失败"));
    }

    /**
     * 编辑用户基础信息。
     * 岗位增强后，用户资料会同时维护部门和岗位两个组织维度，后续做组织筛选和默认角色推荐都会更顺手。
     */
    @PutMapping
    @PreAuthorize("hasAuthority('user:update')")
    public Result update(@RequestBody User user) {
        if (user == null || user.getId() == null) {
            return logResult("编辑用户", "PUT /api/user", user, Result.error("用户ID不能为空"));
        }

        User existingUser = userService.getById(user.getId());
        if (existingUser == null) {
            return logResult("编辑用户", "PUT /api/user", user, Result.error("用户不存在"));
        }

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            return logResult("编辑用户", "PUT /api/user", user, Result.error("用户名不能为空"));
        }

        User duplicateUser = userService.findByUsername(user.getUsername());
        if (duplicateUser != null && !duplicateUser.getId().equals(user.getId())) {
            return logResult("编辑用户", "PUT /api/user", user, Result.error("用户名已存在"));
        }

        Result departmentValidation = validateDepartmentBinding(user.getDepartmentId());
        if (departmentValidation != null) {
            return logResult("编辑用户", "PUT /api/user", user, departmentValidation);
        }

        Result postValidation = validatePostBinding(user.getDepartmentId(), user.getPostId());
        if (postValidation != null) {
            return logResult("编辑用户", "PUT /api/user", user, postValidation);
        }

        Result roleValidation = validateRoleBindings(user.getRoleIds());
        if (roleValidation != null) {
            return logResult("编辑用户", "PUT /api/user", user, roleValidation);
        }

        boolean success = userService.updateUser(user);
        return logResult("编辑用户", "PUT /api/user", user, success ? Result.success("编辑用户成功") : Result.error("编辑用户失败"));
    }

    /**
     * 删除用户。
     * 这里继续采用逻辑删除，避免误删后彻底丢失审计线索。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public Result delete(@PathVariable Long id) {
        boolean success = userService.removeById(id);
        return logResult("删除用户", "DELETE /api/user/" + id, id, success ? Result.success("删除用户成功") : Result.error("删除用户失败"));
    }

    /**
     * 更新用户启用状态。
     * 管理员可以快速切换账号状态，不需要进入编辑弹窗再修改。
     */
    @PutMapping("/status")
    @PreAuthorize("hasAuthority('user:status')")
    public Result updateStatus(@RequestBody StatusRequest request) {
        User user = userService.getById(request.getId());
        if (user == null) {
            return logResult("更新用户状态", "PUT /api/user/status", request, Result.error("用户不存在"));
        }
        user.setStatus(request.getStatus());
        userService.updateById(user);
        return logResult("更新用户状态", "PUT /api/user/status", request, Result.success("状态更新成功"));
    }

    /**
     * 重置指定用户密码。
     * 管理员重置密码时仍然会走统一加密逻辑，避免任何场景写入明文密码。
     */
    @PutMapping("/reset-password")
    @PreAuthorize("hasAuthority('user:reset-password')")
    public Result resetPassword(@RequestBody ResetPasswordRequest request) {
        if (request == null || request.getId() == null) {
            return logResult("重置用户密码", "PUT /api/user/reset-password", request, Result.error("用户ID不能为空"));
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return logResult("重置用户密码", "PUT /api/user/reset-password", request, Result.error("新密码不能为空"));
        }
        if (request.getNewPassword().length() < 6) {
            return logResult("重置用户密码", "PUT /api/user/reset-password", request, Result.error("新密码长度不能少于6位"));
        }

        User user = userService.getById(request.getId());
        if (user == null) {
            return logResult("重置用户密码", "PUT /api/user/reset-password", request, Result.error("用户不存在"));
        }

        boolean success = userService.resetPassword(request.getId(), request.getNewPassword());
        return logResult("重置用户密码", "PUT /api/user/reset-password", request, success ? Result.success("密码重置成功") : Result.error("密码重置失败"));
    }

    /**
     * 当前登录用户修改自己的密码。
     * 必须先校验旧密码，防止仅凭登录态就能静默修改账号凭据。
     */
    @PutMapping("/change-password")
    public Result changePassword(@RequestBody ChangePasswordRequest request) {
        if (request == null) {
            return logResult("修改个人密码", "PUT /api/user/change-password", request, Result.error("请求参数不能为空"));
        }
        if (request.getOldPassword() == null || request.getOldPassword().isBlank()) {
            return logResult("修改个人密码", "PUT /api/user/change-password", request, Result.error("旧密码不能为空"));
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return logResult("修改个人密码", "PUT /api/user/change-password", request, Result.error("新密码不能为空"));
        }
        if (request.getNewPassword().length() < 6) {
            return logResult("修改个人密码", "PUT /api/user/change-password", request, Result.error("新密码长度不能少于6位"));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return logResult("修改个人密码", "PUT /api/user/change-password", request, Result.error("登录状态无效"));
        }

        User currentUser = userService.findByUsername(authentication.getName());
        if (currentUser == null) {
            return logResult("修改个人密码", "PUT /api/user/change-password", request, Result.error("当前用户不存在"));
        }

        boolean success = userService.changePassword(currentUser.getId(), request.getOldPassword(), request.getNewPassword());
        return logResult("修改个人密码", "PUT /api/user/change-password", request,
                success ? Result.success("密码修改成功") : Result.error("旧密码错误或密码修改失败"));
    }

    /**
     * 统一记录用户模块操作日志。
     * 成功和失败都会落日志，方便后续追踪谁调整了用户资料、角色或密码。
     */
    private Result logResult(String operation, String method, Object params, Result result) {
        boolean success = result != null && result.getCode() != null && result.getCode() == 200;
        operationLogService.record(operation, method, params, success, success ? null : result.getMsg());
        return result;
    }

    /**
     * 组装“用户角色分配”的审计载荷。
     * 这里会保留分配前后角色和差异项，保证日志既可读，又便于后续做分析。
     */
    private Map<String, Object> buildAssignRoleLogPayload(User targetUser, List<Role> beforeRoles, List<Role> afterRoles) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", targetUser.getId());
        payload.put("username", targetUser.getUsername());
        payload.put("realName", targetUser.getRealName());
        payload.put("beforeRoleIds", extractRoleIds(beforeRoles));
        payload.put("beforeRoleNames", extractRoleNames(beforeRoles));
        payload.put("afterRoleIds", extractRoleIds(afterRoles));
        payload.put("afterRoleNames", extractRoleNames(afterRoles));
        payload.put("addedRoleNames", calculateRoleDifference(afterRoles, beforeRoles));
        payload.put("removedRoleNames", calculateRoleDifference(beforeRoles, afterRoles));
        return payload;
    }

    /**
     * 提取角色 ID 列表。
     * 审计日志里保留 ID 是为了后续和数据库或导出结果做精确对照。
     */
    private List<Long> extractRoleIds(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .map(Role::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
    }

    /**
     * 提取角色名称列表。
     * 日志展示时优先看名称更直观，管理员不需要再手动拿 roleId 反查。
     */
    private List<String> extractRoleNames(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .map(Role::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * 计算角色前后差异。
     * 审计场景里最关键的是“新增了哪些角色、移除了哪些角色”，所以这里按角色名称做差集。
     */
    private List<String> calculateRoleDifference(List<Role> sourceRoles, List<Role> compareRoles) {
        LinkedHashSet<String> compareRoleNames = compareRoles == null
                ? new LinkedHashSet<>()
                : compareRoles.stream()
                .map(Role::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (sourceRoles == null || sourceRoles.isEmpty()) {
            return Collections.emptyList();
        }

        return sourceRoles.stream()
                .map(Role::getName)
                .filter(name -> name != null && !name.isBlank() && !compareRoleNames.contains(name))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 为用户列表补充角色数量和角色摘要。
     * 列表页优先展示概览，完整角色关系仍以分配弹窗中的数据为准。
     */
    private void fillUserRoleCounts(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        List<Long> userIds = users.stream()
                .map(User::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }

        Map<Long, Integer> roleCountMap = userRoleMapper.countRolesByUserIds(userIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("user_id")).longValue(),
                        row -> ((Number) row.get("role_count")).intValue(),
                        (left, right) -> right
                ));

        Map<Long, String> roleSummaryMap = userRoleMapper.selectRoleNamesByUserIds(userIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("user_id")).longValue(),
                        row -> buildSummary((String) row.get("role_names")),
                        (left, right) -> right
                ));

        users.forEach(user -> {
            user.setRoleCount(roleCountMap.getOrDefault(user.getId(), 0));
            user.setRoleSummary(roleSummaryMap.getOrDefault(user.getId(), "未分配角色"));
        });
    }

    /**
     * 为用户列表补充所属部门名称。
     * 前端表格可以直接使用这个结果展示部门归属。
     */
    private void fillUserDepartmentNames(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        List<Long> departmentIds = users.stream()
                .map(User::getDepartmentId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (departmentIds.isEmpty()) {
            users.forEach(user -> user.setDepartmentName("未分配部门"));
            return;
        }

        Map<Long, String> departmentNameMap = departmentService.listByIds(departmentIds).stream()
                .collect(Collectors.toMap(Department::getId, Department::getName, (left, right) -> right));

        users.forEach(user -> user.setDepartmentName(departmentNameMap.getOrDefault(user.getDepartmentId(), "未分配部门")));
    }

    /**
     * 为用户列表补充所属岗位名称。
     * 岗位增强后，用户页会同时展示组织归属和职责归属，帮助管理员更快判断账号身份。
     */
    private void fillUserPostNames(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        List<Long> postIds = users.stream()
                .map(User::getPostId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (postIds.isEmpty()) {
            users.forEach(user -> user.setPostName("未分配岗位"));
            return;
        }

        Map<Long, String> postNameMap = postService.listByIds(postIds).stream()
                .collect(Collectors.toMap(Post::getId, Post::getName, (left, right) -> right));

        users.forEach(user -> user.setPostName(postNameMap.getOrDefault(user.getPostId(), "未分配岗位")));
    }

    /**
     * 把聚合后的名称裁剪成适合列表展示的摘要。
     * 当前最多显示前两个名称，其余数量折叠成 +N，避免表格太拥挤。
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
     * 校验用户绑定的部门是否合法。
     * 允许为空；如果传了部门 ID，就必须保证部门存在且未被删除。
     */
    private Result validateDepartmentBinding(Long departmentId) {
        if (departmentId == null || departmentId == 0) {
            return null;
        }

        Department department = departmentService.getById(departmentId);
        if (department == null || Integer.valueOf(1).equals(department.getDeleted())) {
            return Result.error("所属部门不存在");
        }
        return null;
    }

    /**
     * 校验用户绑定的岗位是否合法。
     * 第一版同样允许岗位为空；如果有岗位绑定，则要求岗位存在且未删除。
     */
    private Result validatePostBinding(Long departmentId, Long postId) {
        if (postId == null || postId == 0) {
            return null;
        }

        Post post = postService.getById(postId);
        if (post == null || Integer.valueOf(1).equals(post.getDeleted())) {
            return Result.error("所属岗位不存在");
        }
        if (departmentId == null || departmentId == 0) {
            return Result.error("已选择岗位时必须同时选择所属部门");
        }
        if (post.getDepartmentId() == null || !post.getDepartmentId().equals(departmentId)) {
            return Result.error("所属岗位与所属部门不匹配，请重新选择");
        }
        return null;
    }

    /**
     * 校验用户本次提交的角色绑定是否合法。
     * 角色允许为空；如果传了角色列表，就必须保证这些角色都真实存在且未删除。
     */
    private Result validateRoleBindings(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return null;
        }

        List<Long> distinctRoleIds = roleIds.stream()
                .filter(roleId -> roleId != null)
                .distinct()
                .collect(Collectors.toList());
        if (distinctRoleIds.isEmpty()) {
            return null;
        }

        long validRoleCount = roleService.lambdaQuery()
                .in(Role::getId, distinctRoleIds)
                .eq(Role::getDeleted, 0)
                .count();
        if (validRoleCount != distinctRoleIds.size()) {
            return Result.error("所选角色中包含无效角色，请重新选择");
        }
        return null;
    }

    /**
     * 按当前登录人的角色数据范围为用户列表追加过滤条件。
     * 第一版只落到“用户列表”这个最核心、最容易感知的数据入口上。
     */
    private void applyDataScope(LambdaQueryWrapper<User> wrapper) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return;
        }

        User currentUser = userService.findByUsername(authentication.getName());
        if (currentUser == null || currentUser.getId() == null) {
            return;
        }

        List<Role> currentRoles = roleService.getRolesByUserId(currentUser.getId());
        if (currentRoles == null || currentRoles.isEmpty()) {
            wrapper.eq(User::getId, currentUser.getId());
            return;
        }

        String effectiveDataScope = roleService.resolveHighestDataScope(currentRoles);
        if (Role.DATA_SCOPE_ALL.equals(effectiveDataScope)) {
            return;
        }

        if (Role.DATA_SCOPE_SELF.equals(effectiveDataScope)) {
            wrapper.eq(User::getId, currentUser.getId());
            return;
        }

        /*
         * 依赖部门范围的数据权限，本质上是“当前人 + 某些部门内的用户”。
         * 即使当前用户自己还没绑定部门，也至少要保证他能看到自己，避免配置后连本人都查不到。
         */
        if (currentUser.getDepartmentId() == null || currentUser.getDepartmentId() == 0) {
            wrapper.eq(User::getId, currentUser.getId());
            return;
        }

        if (Role.DATA_SCOPE_DEPT.equals(effectiveDataScope)) {
            wrapper.and(query -> query
                    .eq(User::getId, currentUser.getId())
                    .or()
                    .eq(User::getDepartmentId, currentUser.getDepartmentId())
            );
            return;
        }

        List<Long> departmentIds = departmentService.listSelfAndChildDepartmentIds(currentUser.getDepartmentId());
        if (departmentIds.isEmpty()) {
            wrapper.eq(User::getId, currentUser.getId());
            return;
        }
        wrapper.and(query -> query
                .eq(User::getId, currentUser.getId())
                .or()
                .in(User::getDepartmentId, departmentIds)
        );
    }

    static class StatusRequest {
        private Long id;
        private Integer status;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    static class ResetPasswordRequest {
        private Long id;
        private String newPassword;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;

        public String getOldPassword() {
            return oldPassword;
        }

        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
