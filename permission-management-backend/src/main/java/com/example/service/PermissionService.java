package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.Permission;

import java.util.List;

public interface PermissionService extends IService<Permission> {
    List<Permission> getPermissionsByRoleId(Long roleId);
    List<Permission> getPermissionsByUserId(Long userId);
    List<Permission> buildPermissionTree(List<Permission> permissions);
    List<Permission> getMenuPermissionsByUserId(Long userId);
}
