package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.RolePermission;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    /**
     * 按角色批量统计权限绑定数量。
     * 角色列表页会使用这份结果展示“已分配权限数”。
     */
    @Select("<script>" +
            "SELECT role_id, COUNT(*) AS permission_count " +
            "FROM sys_role_permission " +
            "WHERE role_id IN " +
            "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>" +
            "#{roleId}" +
            "</foreach> " +
            "GROUP BY role_id" +
            "</script>")
    List<Map<String, Object>> countPermissionsByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 按角色批量聚合权限名称。
     * 返回完整名称串，控制器会再整理成适合列表展示的摘要。
     */
    @Select("<script>" +
            "SELECT rp.role_id, GROUP_CONCAT(p.name ORDER BY p.id SEPARATOR '||') AS permission_names " +
            "FROM sys_role_permission rp " +
            "JOIN sys_permission p ON p.id = rp.permission_id " +
            "WHERE rp.role_id IN " +
            "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>" +
            "#{roleId}" +
            "</foreach> " +
            "AND p.deleted = 0 " +
            "GROUP BY rp.role_id" +
            "</script>")
    List<Map<String, Object>> selectPermissionNamesByRoleIds(@Param("roleIds") List<Long> roleIds);
}
