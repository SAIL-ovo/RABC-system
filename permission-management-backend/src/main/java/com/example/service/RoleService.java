package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.Role;

import java.util.List;

public interface RoleService extends IService<Role> {
    List<Role> getRolesByUserId(Long userId);
    List<String> getRolePermissions(Long roleId);
    List<Long> getRolePermissionIds(Long roleId);
    void assignPermissions(Long roleId, List<Long> permissionIds);
    String resolveHighestDataScope(List<Role> roles);
}
