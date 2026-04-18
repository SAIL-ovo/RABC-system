package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.User;

import java.util.List;

public interface UserService extends IService<User> {
    User findByUsername(String username);
    List<String> getUserRoles(Long userId);
    List<String> getUserPermissions(Long userId);
    void assignRole(Long userId, List<Long> roleIds);
    boolean createUser(User user);
    boolean updateUser(User user);
    boolean resetPassword(Long userId, String newPassword);
    boolean changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 查询指定岗位下是否仍绑定着用户。
     * 岗位删除前会复用这个能力做约束判断，避免留下悬空岗位引用。
     */
    long countUsersByPostId(Long postId);
}
