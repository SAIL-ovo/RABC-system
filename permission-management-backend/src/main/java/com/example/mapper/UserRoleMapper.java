package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.UserRole;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 批量写入用户角色关系。
     * 角色分配保存时会一次性插入多条中间表数据。
     */
    @Insert("<script>" +
            "INSERT INTO sys_user_role (user_id, role_id) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.userId}, #{item.roleId})" +
            "</foreach>" +
            "</script>")
    void insertBatch(@Param("list") List<UserRole> list);

    /**
     * 查询指定用户当前已绑定的角色 ID 列表。
     * 角色分配弹窗会用这份结果做默认勾选。
     */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectUserRoleIds(@Param("userId") Long userId);

    /**
     * 按用户批量统计角色绑定数量。
     * 返回结果中的键名为 user_id 和 role_count，便于控制器回填到列表对象。
     */
    @Select("<script>" +
            "SELECT user_id, COUNT(*) AS role_count " +
            "FROM sys_user_role " +
            "WHERE user_id IN " +
            "<foreach collection='userIds' item='userId' open='(' separator=',' close=')'>" +
            "#{userId}" +
            "</foreach> " +
            "GROUP BY user_id" +
            "</script>")
    List<Map<String, Object>> countRolesByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 按用户批量聚合角色名称。
     * 列表页会在后端把角色名称裁剪成摘要，前端只负责展示结果。
     */
    @Select("<script>" +
            "SELECT ur.user_id, GROUP_CONCAT(r.name ORDER BY r.id SEPARATOR '||') AS role_names " +
            "FROM sys_user_role ur " +
            "JOIN sys_role r ON r.id = ur.role_id " +
            "WHERE ur.user_id IN " +
            "<foreach collection='userIds' item='userId' open='(' separator=',' close=')'>" +
            "#{userId}" +
            "</foreach> " +
            "AND r.deleted = 0 " +
            "GROUP BY ur.user_id" +
            "</script>")
    List<Map<String, Object>> selectRoleNamesByUserIds(@Param("userIds") List<Long> userIds);
}
