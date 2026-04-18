package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.entity.Permission;
import com.example.entity.Result;
import com.example.entity.RolePermission;
import com.example.mapper.RolePermissionMapper;
import com.example.service.OperationLogService;
import com.example.service.PermissionService;
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

import java.util.List;

@RestController
@RequestMapping("/api/permission")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 查询全部权限列表。
     * 返回扁平结构，主要给下拉框或列表型场景使用。
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('permission:manage', 'permission:view')")
    public Result list() {
        return Result.success(permissionService.list());
    }

    /**
     * 保留一个与 /list 行为一致的查询入口。
     * 便于前端按不同调用习惯访问同一批权限数据。
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('permission:manage', 'permission:view')")
    public Result listByQuery() {
        return Result.success(permissionService.list());
    }

    /**
     * 查询权限树。
     * 主要给权限管理页和角色分配权限时展示树形结构。
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('permission:manage', 'permission:view', 'role:assign-permission')")
    public Result tree() {
        List<Permission> permissions = permissionService.list();
        return Result.success(permissionService.buildPermissionTree(permissions));
    }

    /**
     * 新增权限。
     * 会先校验权限类型、权限编码、父子关系和必要字段是否合法。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('permission:create')")
    public Result save(@RequestBody Permission permission) {
        Result validationResult = validatePermission(permission, false);
        if (validationResult != null) {
            return logResult("新增权限", "POST /api/permission", permission, validationResult);
        }

        permission.setName(permission.getName().trim());
        permission.setCode(permission.getCode().trim());
        if (permission.getUrl() != null) {
            permission.setUrl(permission.getUrl().trim());
        }
        if (permission.getMethod() != null) {
            permission.setMethod(permission.getMethod().trim().toUpperCase());
        }
        if (permission.getParentId() == null) {
            permission.setParentId(0L);
        }
        permission.setLevel(permission.getParentId() == 0 ? 1 : 2);
        if (permission.getSort() == null) {
            permission.setSort(0);
        }
        if (permission.getType() == null) {
            permission.setType(1);
        }
        if (permission.getStatus() == null) {
            permission.setStatus(1);
        }
        if (permission.getDeleted() == null) {
            permission.setDeleted(0);
        }

        return logResult("新增权限", "POST /api/permission", permission, permissionService.save(permission) ? Result.success("新增权限成功") : Result.error("新增权限失败"));
    }

    /**
     * 编辑权限。
     * 更新时会校验权限类型是否合法，以及父节点不能选择自己或自己的子节点。
     */
    @PutMapping
    @PreAuthorize("hasAuthority('permission:update')")
    public Result update(@RequestBody Permission permission) {
        Result validationResult = validatePermission(permission, true);
        if (validationResult != null) {
            return logResult("编辑权限", "PUT /api/permission", permission, validationResult);
        }

        permission.setName(permission.getName().trim());
        permission.setCode(permission.getCode().trim());
        if (permission.getUrl() != null) {
            permission.setUrl(permission.getUrl().trim());
        }
        if (permission.getMethod() != null) {
            permission.setMethod(permission.getMethod().trim().toUpperCase());
        }
        permission.setLevel(permission.getParentId() == 0 ? 1 : 2);

        return logResult("编辑权限", "PUT /api/permission", permission, permissionService.updateById(permission) ? Result.success("编辑权限成功") : Result.error("编辑权限失败"));
    }

    /**
     * 删除权限。
     * 如果存在子节点或已被角色引用，会直接阻止删除。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:delete')")
    public Result delete(@PathVariable Long id) {
        Permission existingPermission = permissionService.getById(id);
        if (existingPermission == null || Integer.valueOf(1).equals(existingPermission.getDeleted())) {
            return logResult("删除权限", "DELETE /api/permission/" + id, id, Result.error("权限不存在"));
        }

        long childCount = permissionService.lambdaQuery()
                .eq(Permission::getParentId, id)
                .eq(Permission::getDeleted, 0)
                .count();
        if (childCount > 0) {
            return logResult("删除权限", "DELETE /api/permission/" + id, id, Result.error("该权限存在子节点，无法删除"));
        }

        long roleBindingCount = rolePermissionMapper.selectCount(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getPermissionId, id)
        );
        if (roleBindingCount > 0) {
            return logResult("删除权限", "DELETE /api/permission/" + id, id, Result.error("该权限已分配给角色，请先解除关联"));
        }

        return logResult("删除权限", "DELETE /api/permission/" + id, id, permissionService.removeById(id) ? Result.success("删除权限成功") : Result.error("删除权限失败"));
    }

    /**
     * 统一记录权限模块操作日志。
     * 让权限树的变更过程具备可追踪性。
     */
    private Result logResult(String operation, String method, Object params, Result result) {
        boolean success = result != null && result.getCode() != null && result.getCode() == 200;
        operationLogService.record(operation, method, params, success, success ? null : result.getMsg());
        return result;
    }

    /**
     * 按 ID 查询单个权限详情。
     * 前端如果后续做独立详情页或延迟加载，可以复用这个接口。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('permission:manage', 'permission:view')")
    public Result getById(@PathVariable Long id) {
        return Result.success(permissionService.getById(id));
    }

    /**
     * 统一校验权限新增/编辑时的输入是否合法。
     * 这里集中处理编码重复、父节点不存在、父子循环引用等规则。
     */
    private Result validatePermission(Permission permission, boolean updating) {
        if (permission == null) {
            return Result.error("权限数据不能为空");
        }
        if (updating && permission.getId() == null) {
            return Result.error("权限ID不能为空");
        }
        if (permission.getName() == null || permission.getName().trim().isEmpty()) {
            return Result.error("权限名称不能为空");
        }
        if (permission.getCode() == null || permission.getCode().trim().isEmpty()) {
            return Result.error("权限编码不能为空");
        }
        if (permission.getType() == null || (permission.getType() != 1 && permission.getType() != 2 && permission.getType() != 3)) {
            return Result.error("权限类型不合法");
        }

        String code = permission.getCode().trim();
        var query = permissionService.lambdaQuery()
                .eq(Permission::getCode, code)
                .eq(Permission::getDeleted, 0);
        if (updating) {
            query.ne(Permission::getId, permission.getId());
        }
        if (query.count() > 0) {
            return Result.error("权限编码已存在");
        }

        Long parentId = permission.getParentId() == null ? 0L : permission.getParentId();
        permission.setParentId(parentId);
        if (permission.getType() != 1 && parentId == 0) {
            return Result.error("按钮权限和接口权限必须选择父级菜单");
        }
        if (updating && permission.getId().equals(parentId)) {
            return Result.error("父级权限不能选择自己");
        }
        if (parentId != 0) {
            Permission parentPermission = permissionService.getById(parentId);
            if (parentPermission == null || Integer.valueOf(1).equals(parentPermission.getDeleted())) {
                return Result.error("父级权限不存在");
            }
            if (parentPermission.getType() == null || parentPermission.getType() != 1) {
                return Result.error("父级权限只能选择菜单类型");
            }
            if (updating && permission.getId() != null && hasChildPermission(permission.getId(), parentId)) {
                return Result.error("父级权限不能选择自己的子节点");
            }
        }
        if (permission.getType() == 3) {
            if (permission.getUrl() == null || permission.getUrl().trim().isEmpty()) {
                return Result.error("接口权限必须填写接口路径");
            }
            if (permission.getMethod() == null || permission.getMethod().trim().isEmpty()) {
                return Result.error("接口权限必须填写请求方式");
            }
        }

        return null;
    }

    /**
     * 判断目标父节点是否落在当前节点的子树里。
     * 这个检查用来防止把树结构改成循环引用。
     */
    private boolean hasChildPermission(Long currentId, Long targetParentId) {
        Long cursor = targetParentId;
        while (cursor != null && cursor != 0) {
            if (cursor.equals(currentId)) {
                return true;
            }
            Permission current = permissionService.getById(cursor);
            if (current == null) {
                return false;
            }
            cursor = current.getParentId();
        }
        return false;
    }
}
