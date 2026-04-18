package com.example.entity;

import java.util.List;

/**
 * 分配角色请求参数接收类
 */
public class AssignRoleRequest {
    // 用户ID
    private Long userId;
    // 角色ID列表
    private List<Long> roleIds;

    // 无参构造（必须）
    public AssignRoleRequest() {}

    // Getter & Setter
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}