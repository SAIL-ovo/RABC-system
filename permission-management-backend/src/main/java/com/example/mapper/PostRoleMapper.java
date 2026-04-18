package com.example.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.PostRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface PostRoleMapper extends BaseMapper<PostRole> {

    default List<Long> selectRoleIdsByPostId(Long postId) {
        if (postId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapper<PostRole>().eq(PostRole::getPostId, postId)).stream()
                .map(PostRole::getRoleId)
                .filter(roleId -> roleId != null)
                .collect(Collectors.toList());
    }

    default void deleteByPostId(Long postId) {
        if (postId == null) {
            return;
        }
        delete(new LambdaQueryWrapper<PostRole>().eq(PostRole::getPostId, postId));
    }

    @Select({
            "<script>",
            "SELECT pr.post_id,",
            "GROUP_CONCAT(r.name ORDER BY r.id SEPARATOR '||') AS role_names",
            "FROM sys_post_role pr",
            "JOIN sys_role r ON r.id = pr.role_id AND r.deleted = 0",
            "WHERE pr.post_id IN",
            "<foreach collection='postIds' item='postId' open='(' separator=',' close=')'>",
            "#{postId}",
            "</foreach>",
            "GROUP BY pr.post_id",
            "</script>"
    })
    List<Map<String, Object>> selectRoleNamesByPostIds(@Param("postIds") List<Long> postIds);

    @Select({
            "<script>",
            "SELECT pr.post_id, COUNT(1) AS role_count",
            "FROM sys_post_role pr",
            "JOIN sys_role r ON r.id = pr.role_id AND r.deleted = 0",
            "WHERE pr.post_id IN",
            "<foreach collection='postIds' item='postId' open='(' separator=',' close=')'>",
            "#{postId}",
            "</foreach>",
            "GROUP BY pr.post_id",
            "</script>"
    })
    List<Map<String, Object>> countRolesByPostIds(@Param("postIds") List<Long> postIds);
}
