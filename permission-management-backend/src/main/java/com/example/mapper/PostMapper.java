package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Post;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface PostMapper extends BaseMapper<Post> {
    @Select({
            "<script>",
            "SELECT post_id, COUNT(1) AS user_count",
            "FROM sys_user",
            "WHERE deleted = 0",
            "AND post_id IN",
            "<foreach collection='postIds' item='postId' open='(' separator=',' close=')'>",
            "#{postId}",
            "</foreach>",
            "GROUP BY post_id",
            "</script>"
    })
    List<Map<String, Object>> countUsersByPostIds(@Param("postIds") List<Long> postIds);
}
