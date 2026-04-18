package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.User;
import com.example.entity.UserRole;
import com.example.mapper.RolePermissionMapper;
import com.example.mapper.UserMapper;
import com.example.mapper.UserRoleMapper;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private RoleServiceImpl roleService;

    @Autowired
    private PermissionServiceImpl permissionService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 按用户名查询用户。
     * 登录认证和新增/编辑时的重复性校验都会复用这个方法。
     */
    @Override
    public User findByUsername(String username) {
        return lambdaQuery().eq(User::getUsername, username).one();
    }

    /**
     * 新增用户并处理密码加密。
     * 这里兜底保证密码不会以明文形式直接落库。
     */
    @Override
    @Transactional
    public boolean createUser(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            return false;
        }

        if (findByUsername(user.getUsername()) != null) {
            return false;
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        if (user.getDeleted() == null) {
            user.setDeleted(0);
        }

        boolean created = save(user);
        if (!created) {
            return false;
        }
        if (user.getRoleIds() != null) {
            assignRole(user.getId(), user.getRoleIds());
        }
        return true;
    }

    /**
     * 更新用户信息。
     * 如果前端没有传新密码，则继续沿用旧密码；如果传了，则重新加密后保存。
     */
    @Override
    @Transactional
    public boolean updateUser(User user) {
        if (user == null || user.getId() == null) {
            return false;
        }

        User existingUser = getById(user.getId());
        if (existingUser == null) {
            return false;
        }

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            return false;
        }

        if (!user.getUsername().equals(existingUser.getUsername())) {
            User duplicateUser = findByUsername(user.getUsername());
            if (duplicateUser != null && !duplicateUser.getId().equals(user.getId())) {
                return false;
            }
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(existingUser.getPassword());
        } else {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        boolean updated = updateById(user);
        if (!updated) {
            return false;
        }
        if (user.getRoleIds() != null) {
            assignRole(user.getId(), user.getRoleIds());
        }
        return true;
    }

    /**
     * 重置指定用户的密码。
     * 这里会统一走 BCrypt 加密，避免任何场景把明文密码直接写入数据库。
     */
    @Override
    @Transactional
    public boolean resetPassword(Long userId, String newPassword) {
        if (userId == null || newPassword == null || newPassword.isBlank()) {
            return false;
        }

        User existingUser = getById(userId);
        if (existingUser == null) {
            return false;
        }

        existingUser.setPassword(passwordEncoder.encode(newPassword));
        return updateById(existingUser);
    }

    /**
     * 当前登录用户修改自己的密码。
     * 必须先校验旧密码正确，避免仅凭登录态就能静默篡改密码。
     */
    @Override
    @Transactional
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null || oldPassword == null || oldPassword.isBlank() || newPassword == null || newPassword.isBlank()) {
            return false;
        }

        User existingUser = getById(userId);
        if (existingUser == null || existingUser.getPassword() == null) {
            return false;
        }

        if (!passwordEncoder.matches(oldPassword, existingUser.getPassword())) {
            return false;
        }

        existingUser.setPassword(passwordEncoder.encode(newPassword));
        return updateById(existingUser);
    }

    /**
     * 查询用户拥有的角色编码列表。
     * Spring Security 在构建授权信息时会使用这里返回的数据。
     */
    /**
     * 统计某个岗位下当前仍绑定的用户数量。
     * 岗位模块删除前会使用这个结果做保护，确保不会删掉仍在被实际用户使用的岗位。
     */
    @Override
    public long countUsersByPostId(Long postId) {
        if (postId == null) {
            return 0;
        }

        return lambdaQuery()
                .eq(User::getPostId, postId)
                .eq(User::getDeleted, 0)
                .count();
    }

    @Override
    public List<String> getUserRoles(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        ).stream().map(UserRole::getRoleId).collect(Collectors.toList());

        if (roleIds.isEmpty()) {
            return List.of();
        }

        return roleService.listByIds(roleIds).stream()
                .map(com.example.entity.Role::getCode)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户拥有的权限编码列表。
     * 这里会先查角色，再通过角色关联权限得到最终权限码。
     */
    @Override
    public List<String> getUserPermissions(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        ).stream().map(UserRole::getRoleId).collect(Collectors.toList());

        if (roleIds.isEmpty()) {
            return List.of();
        }

        List<Long> permissionIds = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<com.example.entity.RolePermission>()
                        .in(com.example.entity.RolePermission::getRoleId, roleIds)
        ).stream().map(com.example.entity.RolePermission::getPermissionId).collect(Collectors.toList());

        if (permissionIds.isEmpty()) {
            return List.of();
        }

        return permissionService.listByIds(permissionIds).stream()
                .map(com.example.entity.Permission::getCode)
                .collect(Collectors.toList());
    }

    /**
     * 覆盖式分配用户角色。
     * 先清空旧角色，再批量写入新的角色关系。
     */
    @Override
    @Transactional
    public void assignRole(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        );

        if (roleIds != null && !roleIds.isEmpty()) {
            List<UserRole> userRoleList = roleIds.stream()
                    .distinct()
                    .map(roleId -> {
                        UserRole userRole = new UserRole();
                        userRole.setUserId(userId);
                        userRole.setRoleId(roleId);
                        return userRole;
                    })
                    .collect(Collectors.toList());
            userRoleMapper.insertBatch(userRoleList);
        }
    }
}
