package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.Post;
import com.example.entity.Result;
import com.example.entity.Department;
import com.example.entity.Role;
import com.example.entity.User;
import com.example.mapper.PostMapper;
import com.example.service.DepartmentService;
import com.example.service.OperationLogService;
import com.example.service.PostService;
import com.example.service.RoleService;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/post")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 分页查询岗位列表。
     * 这一版除了岗位基础字段外，还会补充“绑定用户数”和“默认角色摘要”，让岗位配置是否完整一眼可见。
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('post:manage', 'post:view')")
    public Result list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Integer status
    ) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getDeleted, 0);
        if (name != null && !name.isBlank()) {
            wrapper.like(Post::getName, name.trim());
        }
        if (code != null && !code.isBlank()) {
            wrapper.like(Post::getCode, code.trim());
        }
        if (status != null) {
            wrapper.eq(Post::getStatus, status);
        }
        wrapper.orderByAsc(Post::getSort).orderByAsc(Post::getId);

        Page<Post> pageResult = postService.page(new Page<>(page, size), wrapper);
        fillPostUserCounts(pageResult.getRecords());
        fillPostDepartmentNames(pageResult.getRecords());
        postService.fillDefaultRoleInfo(pageResult.getRecords());
        return Result.success(pageResult);
    }

    /**
     * 查询岗位下拉选项。
     * 用户新增、编辑时只需要拿到可绑定的启用岗位，所以这里单独提供一个轻量接口。
     */
    @GetMapping("/options")
    @PreAuthorize("hasAnyAuthority('post:manage', 'post:view', 'user:create', 'user:update', 'user:view')")
    public Result options(@RequestParam(required = false) Long departmentId) {
        if (departmentId != null && departmentId > 0) {
            return Result.success(postService.listEnabledPostsByDepartmentId(departmentId));
        }
        return Result.success(postService.listEnabledPosts());
    }

    /**
     * 查询可供岗位配置的角色选项。
     * 这里单独提供岗位模块自己的角色下拉接口，避免岗位管理页额外依赖角色列表权限。
     */
    @GetMapping("/role-options")
    @PreAuthorize("hasAnyAuthority('post:manage', 'post:view', 'post:update', 'post:create')")
    public Result roleOptions() {
        List<Role> roles = roleService.lambdaQuery()
                .eq(Role::getDeleted, 0)
                .eq(Role::getStatus, 1)
                .orderByAsc(Role::getId)
                .list();
        return Result.success(roles);
    }

    /**
     * 查询岗位配置的默认角色 ID 列表。
     * 岗位编辑弹窗会拿这个结果回显当前默认角色勾选状态。
     */
    @GetMapping("/default-roles/{postId}")
    @PreAuthorize("hasAnyAuthority('post:manage', 'post:view', 'post:update', 'post:create', 'user:create', 'user:update', 'user:view')")
    public Result getDefaultRoleIds(@PathVariable Long postId) {
        Post post = postService.getById(postId);
        if (post == null || Integer.valueOf(1).equals(post.getDeleted())) {
            return Result.error("岗位不存在");
        }
        return Result.success(postService.getDefaultRoleIds(postId));
    }

    /**
     * 新增岗位。
     * 除了保存岗位基本信息，也会把岗位默认角色一并落库，方便后续做组织模型联动。
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('post:manage', 'post:create')")
    @Transactional(rollbackFor = Exception.class)
    public Result save(@RequestBody Post post) {
        Result validationResult = validatePost(post, false);
        if (validationResult != null) {
            return logResult("新增岗位", "POST /api/post", post, validationResult);
        }

        normalizePost(post);
        boolean saved = postService.save(post);
        if (saved) {
            postService.assignDefaultRoles(post.getId(), post.getDefaultRoleIds());
        }
        return logResult(
                "新增岗位",
                "POST /api/post",
                buildPostLogPayload(post),
                saved ? Result.success("岗位新增成功") : Result.error("岗位新增失败")
        );
    }

    /**
     * 编辑岗位。
     * 这里同样采用覆盖式更新默认角色，保证岗位与角色之间的模板关系始终是最新状态。
     */
    @PutMapping
    @PreAuthorize("hasAnyAuthority('post:manage', 'post:update')")
    @Transactional(rollbackFor = Exception.class)
    public Result update(@RequestBody Post post) {
        Result validationResult = validatePost(post, true);
        if (validationResult != null) {
            return logResult("编辑岗位", "PUT /api/post", post, validationResult);
        }

        normalizePost(post);
        boolean updated = postService.updateById(post);
        if (updated) {
            postService.assignDefaultRoles(post.getId(), post.getDefaultRoleIds());
        }
        return logResult(
                "编辑岗位",
                "PUT /api/post",
                buildPostLogPayload(post),
                updated ? Result.success("岗位编辑成功") : Result.error("岗位编辑失败")
        );
    }

    /**
     * 删除岗位。
     * 如果岗位下仍然绑定着用户，这一版直接拦截删除，避免出现用户资料里残留悬空岗位 ID。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('post:manage', 'post:delete')")
    @Transactional(rollbackFor = Exception.class)
    public Result delete(@PathVariable Long id) {
        Post existingPost = postService.getById(id);
        if (existingPost == null || Integer.valueOf(1).equals(existingPost.getDeleted())) {
            return logResult("删除岗位", "DELETE /api/post/" + id, id, Result.error("岗位不存在"));
        }

        long bindingCount = userService.lambdaQuery()
                .eq(User::getPostId, id)
                .eq(User::getDeleted, 0)
                .count();
        if (bindingCount > 0) {
            return logResult("删除岗位", "DELETE /api/post/" + id, id, Result.error("该岗位已绑定用户，请先调整用户岗位归属"));
        }

        postService.assignDefaultRoles(id, Collections.emptyList());
        return logResult(
                "删除岗位",
                "DELETE /api/post/" + id,
                id,
                postService.removeById(id) ? Result.success("岗位删除成功") : Result.error("岗位删除失败")
        );
    }

    /**
     * 统一校验岗位新增和编辑时的输入是否合法。
     * 这里把“必填、唯一、目标记录是否存在、默认角色是否合法”都收口到一起，后面扩展规则也更方便。
     */
    private Result validatePost(Post post, boolean updating) {
        if (post == null) {
            return Result.error("岗位数据不能为空");
        }
        if (updating && post.getId() == null) {
            return Result.error("岗位ID不能为空");
        }
        if (post.getName() == null || post.getName().trim().isEmpty()) {
            return Result.error("岗位名称不能为空");
        }
        if (post.getCode() == null || post.getCode().trim().isEmpty()) {
            return Result.error("岗位编码不能为空");
        }
        if (post.getDepartmentId() == null || post.getDepartmentId() == 0) {
            return Result.error("所属部门不能为空");
        }

        String name = post.getName().trim();
        String code = post.getCode().trim();

        LambdaQueryWrapper<Post> nameWrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getName, name)
                .eq(Post::getDeleted, 0);
        LambdaQueryWrapper<Post> codeWrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getCode, code)
                .eq(Post::getDeleted, 0);
        if (updating) {
            nameWrapper.ne(Post::getId, post.getId());
            codeWrapper.ne(Post::getId, post.getId());
        }

        if (postService.count(nameWrapper) > 0) {
            return Result.error("岗位名称已存在");
        }
        if (postService.count(codeWrapper) > 0) {
            return Result.error("岗位编码已存在");
        }

        Department department = departmentService.getById(post.getDepartmentId());
        if (department == null || Integer.valueOf(1).equals(department.getDeleted())) {
            return Result.error("所属部门不存在");
        }

        if (updating) {
            Post existingPost = postService.getById(post.getId());
            if (existingPost == null || Integer.valueOf(1).equals(existingPost.getDeleted())) {
                return Result.error("岗位不存在");
            }
        }

        List<Long> requestedRoleIds = normalizeRoleIds(post.getDefaultRoleIds());
        if (!requestedRoleIds.isEmpty()) {
            long validRoleCount = roleService.lambdaQuery()
                    .in(Role::getId, requestedRoleIds)
                    .eq(Role::getDeleted, 0)
                    .count();
            if (validRoleCount != requestedRoleIds.size()) {
                return Result.error("默认角色中包含无效角色，请重新选择");
            }
        }
        return null;
    }

    /**
     * 统一整理岗位输入数据。
     * 这样可以把空白字符、默认排序、默认状态和默认角色去重都在入库前一次性收束。
     */
    private void normalizePost(Post post) {
        post.setName(post.getName().trim());
        post.setCode(post.getCode().trim());
        if (post.getRemark() != null) {
            post.setRemark(post.getRemark().trim());
        }
        if (post.getDepartmentId() != null && post.getDepartmentId() == 0) {
            post.setDepartmentId(null);
        }
        if (post.getSort() == null) {
            post.setSort(0);
        }
        if (post.getStatus() == null) {
            post.setStatus(1);
        }
        post.setDefaultRoleIds(normalizeRoleIds(post.getDefaultRoleIds()));
    }

    /**
     * 为岗位列表补充当前绑定用户数。
     * 这样岗位管理页能直接显示“岗位是否在用”，删除拦截提示也更容易让管理员理解。
     */
    private void fillPostUserCounts(List<Post> posts) {
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

        Map<Long, Integer> countMap = postMapper.countUsersByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("post_id")).longValue(),
                        row -> ((Number) row.get("user_count")).intValue(),
                        (left, right) -> right
                ));

        posts.forEach(post -> post.setUserCount(countMap.getOrDefault(post.getId(), 0)));
    }

    /**
     * 为岗位列表补充所属部门名称。
     * 岗位一旦和部门绑定，列表里直接展示部门比只显示岗位名更能帮助管理员识别岗位归属。
     */
    private void fillPostDepartmentNames(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }

        List<Long> departmentIds = posts.stream()
                .map(Post::getDepartmentId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (departmentIds.isEmpty()) {
            posts.forEach(post -> post.setDepartmentName("未分配部门"));
            return;
        }

        Map<Long, String> departmentNameMap = departmentService.listByIds(departmentIds).stream()
                .collect(Collectors.toMap(Department::getId, Department::getName, (left, right) -> right));

        posts.forEach(post -> post.setDepartmentName(departmentNameMap.getOrDefault(post.getDepartmentId(), "未分配部门")));
    }

    /**
     * 统一规整默认角色 ID 列表。
     * 去重、过滤空值后再入库，避免岗位默认角色关系表出现重复脏数据。
     */
    private List<Long> normalizeRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(roleIds.stream()
                .filter(roleId -> roleId != null)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    /**
     * 组装岗位日志参数。
     * 这里额外保留默认角色 ID 和角色名称，后续排查“岗位模板为什么这样授权”时会更直接。
     */
    private Map<String, Object> buildPostLogPayload(Post post) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", post.getId());
        payload.put("name", post.getName());
        payload.put("code", post.getCode());
        payload.put("departmentId", post.getDepartmentId());
        if (post.getDepartmentId() != null) {
            Department department = departmentService.getById(post.getDepartmentId());
            payload.put("departmentName", department == null ? null : department.getName());
        } else {
            payload.put("departmentName", null);
        }
        payload.put("sort", post.getSort());
        payload.put("status", post.getStatus());
        payload.put("remark", post.getRemark());
        payload.put("defaultRoleIds", post.getDefaultRoleIds());
        if (post.getDefaultRoleIds() != null && !post.getDefaultRoleIds().isEmpty()) {
            List<String> roleNames = roleService.lambdaQuery()
                    .in(Role::getId, post.getDefaultRoleIds())
                    .eq(Role::getDeleted, 0)
                    .list()
                    .stream()
                    .map(Role::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.toList());
            payload.put("defaultRoleNames", roleNames);
        } else {
            payload.put("defaultRoleNames", Collections.emptyList());
        }
        return payload;
    }

    /**
     * 统一记录岗位模块操作日志。
     * 成功和失败都落日志，后续排查岗位删改和默认角色调整时会更顺手。
     */
    private Result logResult(String operation, String method, Object params, Result result) {
        boolean success = result != null && result.getCode() != null && result.getCode() == 200;
        operationLogService.record(operation, method, params, success, success ? null : result.getMsg());
        return result;
    }
}
