package com.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.Post;
import com.example.entity.PostRole;
import com.example.mapper.PostMapper;
import com.example.mapper.PostRoleMapper;
import com.example.service.PostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private final PostRoleMapper postRoleMapper;

    public PostServiceImpl(PostRoleMapper postRoleMapper) {
        this.postRoleMapper = postRoleMapper;
    }

    /**
     * 查询可供用户绑定的启用中岗位列表。
     * 用户表单只需要拿到稳定、可选、已排序的岗位选项，所以这里统一做状态和排序兜底。
     */
    @Override
    public List<Post> listEnabledPosts() {
        return lambdaQuery()
                .eq(Post::getDeleted, 0)
                .eq(Post::getStatus, 1)
                .orderByAsc(Post::getSort)
                .orderByAsc(Post::getId)
                .list();
    }

    /**
     * 按部门查询可绑定的启用岗位。
     * 用户表单里一旦已经选了所属部门，就只应该看到这个部门下的岗位，避免跨部门错配。
     */
    @Override
    public List<Post> listEnabledPostsByDepartmentId(Long departmentId) {
        if (departmentId == null || departmentId == 0) {
            return List.of();
        }
        return lambdaQuery()
                .eq(Post::getDeleted, 0)
                .eq(Post::getStatus, 1)
                .eq(Post::getDepartmentId, departmentId)
                .orderByAsc(Post::getSort)
                .orderByAsc(Post::getId)
                .list();
    }

    /**
     * 为岗位覆盖式分配默认角色。
     * 第一版先固定成“先清空再重建”，这样能保证岗位默认角色来源唯一，排查问题也更直接。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDefaultRoles(Long postId, List<Long> roleIds) {
        postRoleMapper.deleteByPostId(postId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        List<Long> distinctRoleIds = roleIds.stream()
                .filter(roleId -> roleId != null)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
        for (Long roleId : distinctRoleIds) {
            PostRole relation = new PostRole();
            relation.setPostId(postId);
            relation.setRoleId(roleId);
            postRoleMapper.insert(relation);
        }
    }

    /**
     * 查询岗位已配置的默认角色 ID。
     * 前端打开编辑弹窗时会直接用这个结果做勾选回显。
     */
    @Override
    public List<Long> getDefaultRoleIds(Long postId) {
        return postRoleMapper.selectRoleIdsByPostId(postId);
    }

    /**
     * 为岗位列表补齐默认角色数量和摘要。
     * 这样岗位管理页就能直接看出“这个岗位是否已经配置授权模板”。
     */
    @Override
    public void fillDefaultRoleInfo(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }

        List<Long> postIds = posts.stream()
                .map(Post::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (postIds.isEmpty()) {
            return;
        }

        Map<Long, Integer> roleCountMap = postRoleMapper.countRolesByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("post_id")).longValue(),
                        row -> ((Number) row.get("role_count")).intValue(),
                        (left, right) -> right
                ));

        Map<Long, String> roleSummaryMap = postRoleMapper.selectRoleNamesByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("post_id")).longValue(),
                        row -> buildSummary((String) row.get("role_names")),
                        (left, right) -> right
                ));

        posts.forEach(post -> {
            post.setDefaultRoleCount(roleCountMap.getOrDefault(post.getId(), 0));
            post.setDefaultRoleSummary(roleSummaryMap.getOrDefault(post.getId(), "未配置默认角色"));
        });
    }

    /**
     * 把聚合后的角色名转换成列表摘要。
     * 列表页只展示前两个角色名，其余折叠成 +N，既能表达信息量，也不会把表格撑得太散。
     */
    private String buildSummary(String names) {
        if (names == null || names.isBlank()) {
            return "未配置默认角色";
        }
        List<String> items = Arrays.stream(names.split("\\|\\|"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());
        if (items.isEmpty()) {
            return "未配置默认角色";
        }
        if (items.size() <= 2) {
            return String.join("、", items);
        }
        return String.join("、", items.subList(0, 2)) + " +" + (items.size() - 2);
    }
}
