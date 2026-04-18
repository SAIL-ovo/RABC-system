package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.Post;

import java.util.List;

public interface PostService extends IService<Post> {
    List<Post> listEnabledPosts();

    List<Post> listEnabledPostsByDepartmentId(Long departmentId);

    void assignDefaultRoles(Long postId, List<Long> roleIds);

    List<Long> getDefaultRoleIds(Long postId);

    void fillDefaultRoleInfo(List<Post> posts);
}
