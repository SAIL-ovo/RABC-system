package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.Role;
import com.example.entity.RolePermission;
import com.example.entity.UserRole;
import com.example.mapper.RoleMapper;
import com.example.mapper.RolePermissionMapper;
import com.example.mapper.UserRoleMapper;
import com.example.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private PermissionServiceImpl permissionService;

    /**
     * 查询某个用户拥有的全部角色。
     * 先查中间表，再根据角色 ID 批量取角色对象。
     */
    @Override
    public List<Role> getRolesByUserId(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        ).stream().map(UserRole::getRoleId).collect(Collectors.toList());

        return listByIds(roleIds);
    }

    /**
     * 查询角色对应的权限编码列表。
     * 主要给鉴权和调试时查看角色授权范围使用。
     */
    @Override
    public List<String> getRolePermissions(Long roleId) {
        List<Long> permissionIds = getRolePermissionIds(roleId);
        return permissionService.listByIds(permissionIds).stream()
                .map(com.example.entity.Permission::getCode)
                .collect(Collectors.toList());
    }

    /**
     * 查询角色当前绑定的权限 ID 列表。
     * 前端角色分配权限时会拿这个结果做树形回显。
     */
    @Override
    public List<Long> getRolePermissionIds(Long roleId) {
        return rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId)
        ).stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
    }

    /**
     * 覆盖式分配角色权限。
     * 先删旧关系，再按新提交的权限列表重新建立绑定。
     */
    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId)
        );

        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }

        permissionIds.stream()
                .filter(permissionId -> permissionId != null)
                .distinct()
                .forEach(permissionId -> {
                    RolePermission rolePermission = new RolePermission();
                    rolePermission.setRoleId(roleId);
                    rolePermission.setPermissionId(permissionId);
                    rolePermissionMapper.insert(rolePermission);
                });
    }

    /**
     * 在一个用户拥有多个角色时，取可见范围最大的那一个作为最终数据权限。
     * 这样角色叠加后的效果更符合常见权限设计习惯，不会出现“多给了角色反而看得更少”的情况。
     */
    @Override
    public String resolveHighestDataScope(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Role.DATA_SCOPE_SELF;
        }

        int highestPriority = Integer.MAX_VALUE;
        String highestScope = Role.DATA_SCOPE_SELF;
        for (Role role : roles) {
            String currentScope = role.getDataScope();
            if (currentScope == null || currentScope.isBlank()) {
                currentScope = Role.DATA_SCOPE_ALL;
            }
            int currentPriority = dataScopePriority(currentScope);
            if (currentPriority < highestPriority) {
                highestPriority = currentPriority;
                highestScope = currentScope;
            }
        }
        return highestScope;
    }

    /**
     * 数据范围优先级越小，表示可见范围越大。
     * 这套排序只服务于后端合并多角色时的决策逻辑。
     */
    private int dataScopePriority(String dataScope) {
        if (Role.DATA_SCOPE_ALL.equals(dataScope)) {
            return 1;
        }
        if (Role.DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope)) {
            return 2;
        }
        if (Role.DATA_SCOPE_DEPT.equals(dataScope)) {
            return 3;
        }
        return 4;
    }
}
