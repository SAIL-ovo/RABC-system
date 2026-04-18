package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.Permission;
import com.example.entity.RolePermission;
import com.example.entity.UserRole;
import com.example.mapper.PermissionMapper;
import com.example.mapper.RolePermissionMapper;
import com.example.mapper.UserRoleMapper;
import com.example.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    /**
     * 查询某个角色拥有的权限对象列表。
     * 主要用于角色维度查看或后续扩展角色详情能力。
     */
    @Override
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        List<Long> permissionIds = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId)
        ).stream().map(RolePermission::getPermissionId).collect(Collectors.toList());

        if (permissionIds.isEmpty()) {
            return List.of();
        }

        return listByIds(permissionIds);
    }

    /**
     * 查询某个用户拥有的全部权限对象。
     * 这里会自动去重，并过滤掉已逻辑删除的权限。
     */
    @Override
    public List<Permission> getPermissionsByUserId(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        ).stream().map(UserRole::getRoleId).collect(Collectors.toList());

        if (roleIds.isEmpty()) {
            return List.of();
        }

        List<Long> permissionIds = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds)
        ).stream().map(RolePermission::getPermissionId).distinct().collect(Collectors.toList());

        if (permissionIds.isEmpty()) {
            return List.of();
        }

        return lambdaQuery()
                .in(Permission::getId, permissionIds)
                .eq(Permission::getDeleted, 0)
                .list();
    }

    /**
     * 查询用户可见的菜单权限树。
     * 这里只保留启用中的菜单型权限，避免按钮权限进入左侧菜单。
     */
    @Override
    public List<Permission> getMenuPermissionsByUserId(Long userId) {
        List<Permission> permissions = getPermissionsByUserId(userId).stream()
                // 这里只返回启用中的菜单权限，避免按钮权限和停用节点进入左侧菜单。
                .filter(permission -> permission.getType() != null && permission.getType() == 1)
                .filter(permission -> permission.getStatus() != null && permission.getStatus() == 1)
                .sorted(Comparator.comparing(Permission::getSort, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());

        return buildPermissionTree(permissions);
    }

    /**
     * 把扁平权限列表组装成树结构。
     * 前端菜单、权限树展示都会直接消费这个结果。
     */
    @Override
    public List<Permission> buildPermissionTree(List<Permission> permissions) {
        List<Permission> sortedPermissions = permissions.stream()
                .sorted(Comparator.comparing(Permission::getSort, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());

        List<Permission> tree = new ArrayList<>();
        for (Permission permission : sortedPermissions) {
            permission.setChildren(new ArrayList<>());
        }

        for (Permission permission : sortedPermissions) {
            if (permission.getParentId() == null || permission.getParentId() == 0) {
                tree.add(permission);
                continue;
            }

            Permission parent = sortedPermissions.stream()
                    .filter(item -> item.getId().equals(permission.getParentId()))
                    .findFirst()
                    .orElse(null);
            if (parent != null) {
                parent.getChildren().add(permission);
            }
        }

        return tree;
    }
}
