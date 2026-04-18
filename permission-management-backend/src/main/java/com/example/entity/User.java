package com.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_user")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String realName;

    private String email;

    private String phone;

    private Long departmentId;

    /**
     * 用户当前绑定的岗位 ID。
     * 第一版先采用“一人一岗”模型，后续如果要扩展成多岗位，可以在不破坏现有页面的前提下平滑升级到关联表。
     */
    private Long postId;

    private Integer status;

    @TableField(exist = false)
    private Integer roleCount;

    /**
     * 用户当前已绑定的角色摘要。
     * 列表页只展示精简后的角色名称，完整角色关系仍以分配弹窗为准。
     */
    @TableField(exist = false)
    private String roleSummary;

    /**
     * 用户所属部门名称。
     * 这个字段由列表接口动态补充，便于前端直接展示部门信息。
     */
    @TableField(exist = false)
    private String departmentName;

    /**
     * 用户当前绑定的岗位名称。
     * 由列表接口动态补充，前端直接展示，不参与数据库持久化。
     */
    @TableField(exist = false)
    private String postName;

    /**
     * 用户本次提交时携带的角色 ID 列表。
     * 这个字段不落库，只用于把“用户资料编辑”和“角色分配”收敛到同一条保存链路里。
     */
    @TableField(exist = false)
    private List<Long> roleIds;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
