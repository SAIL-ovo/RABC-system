package com.example.controller;

import com.example.entity.Department;
import com.example.entity.Result;
import com.example.entity.User;
import com.example.mapper.DepartmentMapper;
import com.example.service.DepartmentService;
import com.example.service.OperationLogService;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/department")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 查询全部部门列表。
     * 返回扁平结构，并补充当前部门用户数和结构化负责人名称，方便前端部门树和统计卡片直接复用。
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('department:manage', 'department:view')")
    public Result list() {
        List<Department> departments = departmentService.lambdaQuery()
                .eq(Department::getDeleted, 0)
                .orderByAsc(Department::getSort)
                .orderByAsc(Department::getId)
                .list();
        fillDepartmentUserCounts(departments);
        fillDepartmentLeaders(departments);
        return Result.success(departments);
    }

    /**
     * 查询部门树。
     * 前端部门管理页和用户绑定部门表单都会消费这份树结构。
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('department:manage', 'department:view', 'user:view', 'user:create', 'user:update')")
    public Result tree() {
        List<Department> departments = departmentService.lambdaQuery()
                .eq(Department::getDeleted, 0)
                .orderByAsc(Department::getSort)
                .orderByAsc(Department::getId)
                .list();
        fillDepartmentUserCounts(departments);
        fillDepartmentLeaders(departments);
        return Result.success(departmentService.buildDepartmentTree(departments));
    }

    /**
     * 新增部门。
     * 这里会统一校验部门名称、编码、上级关系，以及负责人是否绑定到了有效用户。
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('department:manage', 'department:create')")
    public Result save(@RequestBody Department department) {
        Result validationResult = validateDepartment(department, false);
        if (validationResult != null) {
            return logResult("新增部门", "POST /api/department", department, validationResult);
        }

        normalizeDepartment(department);
        applyLeaderSnapshot(department);
        department.setCreateTime(LocalDateTime.now());
        department.setUpdateTime(LocalDateTime.now());
        department.setDeleted(0);
        return logResult("新增部门", "POST /api/department", department,
                departmentService.save(department) ? Result.success("部门新增成功") : Result.error("部门新增失败"));
    }

    /**
     * 编辑部门。
     * 更新时会阻止把上级选成自己或自己的子节点，同时同步刷新负责人展示名称，避免出现旧负责人残留。
     */
    @PutMapping
    @PreAuthorize("hasAnyAuthority('department:manage', 'department:update')")
    public Result update(@RequestBody Department department) {
        Result validationResult = validateDepartment(department, true);
        if (validationResult != null) {
            return logResult("编辑部门", "PUT /api/department", department, validationResult);
        }

        normalizeDepartment(department);
        applyLeaderSnapshot(department);
        return logResult("编辑部门", "PUT /api/department", department,
                departmentService.updateById(department) ? Result.success("部门编辑成功") : Result.error("部门编辑失败"));
    }

    /**
     * 删除部门。
     * 如果部门下仍有子部门或绑定用户，则直接拦截，避免留下悬空组织数据。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('department:manage', 'department:delete')")
    public Result delete(@PathVariable Long id) {
        Department existingDepartment = departmentService.getById(id);
        if (existingDepartment == null || Integer.valueOf(1).equals(existingDepartment.getDeleted())) {
            return logResult("删除部门", "DELETE /api/department/" + id, id, Result.error("部门不存在"));
        }

        long childCount = departmentService.lambdaQuery()
                .eq(Department::getParentId, id)
                .eq(Department::getDeleted, 0)
                .count();
        if (childCount > 0) {
            return logResult("删除部门", "DELETE /api/department/" + id, id, Result.error("该部门存在子部门，无法删除"));
        }

        long userBindingCount = userService.lambdaQuery()
                .eq(User::getDepartmentId, id)
                .eq(User::getDeleted, 0)
                .count();
        if (userBindingCount > 0) {
            return logResult("删除部门", "DELETE /api/department/" + id, id, Result.error("该部门已绑定用户，请先调整用户归属"));
        }

        return logResult("删除部门", "DELETE /api/department/" + id, id,
                departmentService.removeById(id) ? Result.success("部门删除成功") : Result.error("部门删除失败"));
    }

    /**
     * 为部门列表补充当前绑定用户数量。
     * 这样前端无需额外请求统计接口，就能直接展示部门规模。
     */
    private void fillDepartmentUserCounts(List<Department> departments) {
        if (departments == null || departments.isEmpty()) {
            return;
        }

        List<Long> departmentIds = departments.stream()
                .map(Department::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (departmentIds.isEmpty()) {
            return;
        }

        Map<Long, Integer> userCountMap = departmentMapper.countUsersByDepartmentIds(departmentIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("department_id")).longValue(),
                        row -> ((Number) row.get("user_count")).intValue(),
                        (left, right) -> right
                ));

        departments.forEach(department -> department.setUserCount(userCountMap.getOrDefault(department.getId(), 0)));
    }

    /**
     * 根据负责人用户 ID 反查负责人姓名。
     * 这里保留 leader 文本快照，是为了兼容现有前端展示和历史数据，同时把负责人关系真正结构化。
     */
    private void fillDepartmentLeaders(List<Department> departments) {
        if (departments == null || departments.isEmpty()) {
            return;
        }

        List<Long> leaderUserIds = departments.stream()
                .map(Department::getLeaderUserId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (leaderUserIds.isEmpty()) {
            return;
        }

        Map<Long, String> leaderNameMap = userService.listByIds(leaderUserIds).stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(User::getId, User::getRealName, (left, right) -> right));

        departments.forEach(department -> {
            if (department.getLeaderUserId() != null) {
                department.setLeader(leaderNameMap.getOrDefault(department.getLeaderUserId(), department.getLeader()));
            }
        });
    }

    /**
     * 在真正入库前统一整理字段。
     * 这样可以把字符串裁剪、默认值补齐，也能避免负责人被清空后还残留旧名称。
     */
    private void normalizeDepartment(Department department) {
        department.setName(department.getName().trim());
        department.setCode(department.getCode().trim());
        if (department.getPhone() != null) {
            department.setPhone(department.getPhone().trim());
        }
        if (department.getParentId() == null) {
            department.setParentId(0L);
        }
        if (department.getLeaderUserId() == null || department.getLeaderUserId() == 0) {
            department.setLeaderUserId(null);
            department.setLeader(null);
        }
        if (department.getSort() == null) {
            department.setSort(0);
        }
        if (department.getStatus() == null) {
            department.setStatus(1);
        }
    }

    /**
     * 根据负责人用户 ID 落一份展示名快照。
     * 这样即使后续用户姓名变化，历史日志和旧数据里也仍然有一份当时的负责人展示值可追溯。
     */
    private void applyLeaderSnapshot(Department department) {
        if (department.getLeaderUserId() == null) {
            department.setLeader(null);
            return;
        }

        User leaderUser = userService.getById(department.getLeaderUserId());
        department.setLeader(leaderUser == null ? null : leaderUser.getRealName());
    }

    /**
     * 校验部门新增和编辑时的输入是否合法。
     * 这里集中处理名称、编码、父级关系，以及结构化负责人是否合法等规则。
     */
    private Result validateDepartment(Department department, boolean updating) {
        if (department == null) {
            return Result.error("部门数据不能为空");
        }
        if (updating && department.getId() == null) {
            return Result.error("部门ID不能为空");
        }
        if (department.getName() == null || department.getName().trim().isEmpty()) {
            return Result.error("部门名称不能为空");
        }
        if (department.getCode() == null || department.getCode().trim().isEmpty()) {
            return Result.error("部门编码不能为空");
        }

        String code = department.getCode().trim();
        var codeQuery = departmentService.lambdaQuery()
                .eq(Department::getCode, code)
                .eq(Department::getDeleted, 0);
        if (updating) {
            codeQuery.ne(Department::getId, department.getId());
        }
        if (codeQuery.count() > 0) {
            return Result.error("部门编码已存在");
        }

        Long parentId = department.getParentId() == null ? 0L : department.getParentId();
        department.setParentId(parentId);
        if (updating && department.getId().equals(parentId)) {
            return Result.error("上级部门不能选择自己");
        }
        if (parentId != 0) {
            Department parentDepartment = departmentService.getById(parentId);
            if (parentDepartment == null || Integer.valueOf(1).equals(parentDepartment.getDeleted())) {
                return Result.error("上级部门不存在");
            }
            if (updating && departmentService.hasChildDepartment(department.getId(), parentId)) {
                return Result.error("上级部门不能选择自己的子部门");
            }
        }

        if (department.getLeaderUserId() != null && department.getLeaderUserId() != 0) {
            User leaderUser = userService.getById(department.getLeaderUserId());
            if (leaderUser == null || Integer.valueOf(1).equals(leaderUser.getDeleted())) {
                return Result.error("负责人用户不存在");
            }
        }

        if (updating) {
            Department existingDepartment = departmentService.getById(department.getId());
            if (existingDepartment == null || Integer.valueOf(1).equals(existingDepartment.getDeleted())) {
                return Result.error("部门不存在");
            }
        }

        return null;
    }

    /**
     * 统一记录部门模块操作日志。
     * 成功和失败都会落日志，方便后续排查部门调整记录。
     */
    private Result logResult(String operation, String method, Object params, Result result) {
        boolean success = result != null && result.getCode() != null && result.getCode() == 200;
        operationLogService.record(operation, method, params, success, success ? null : result.getMsg());
        return result;
    }
}
