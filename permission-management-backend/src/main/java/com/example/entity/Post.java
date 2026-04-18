package com.example.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_post")
public class Post implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    /**
     * 岗位所属部门 ID。
     * 这层归属关系是组织模型的关键约束，后续用户绑定岗位、岗位统计和组织画像都会依赖它。
     */
    private Long departmentId;

    private Integer sort;

    private Integer status;

    private String remark;

    /**
     * 当前岗位绑定的用户数量。
     * 这个字段只用于列表展示和删除前提示，不参与数据库持久化。
     */
    @TableField(exist = false)
    private Integer userCount;

    /**
     * 岗位所属部门名称。
     * 由列表接口动态补齐，前端展示时不需要再额外自己查部门映射。
     */
    @TableField(exist = false)
    private String departmentName;

    /**
     * 岗位配置的默认角色 ID 列表。
     * 第一版先把“岗位推荐哪些角色”沉淀下来，后续做用户创建联动和审批流时可以直接复用。
     */
    @TableField(exist = false)
    private List<Long> defaultRoleIds;

    /**
     * 岗位配置的默认角色名称列表。
     * 主要用于前端弹窗默认勾选和日志记录，避免前后端都去重复翻译角色 ID。
     */
    @TableField(exist = false)
    private List<String> defaultRoleNames;

    /**
     * 岗位默认角色数量。
     * 列表直接显示这个数字，管理员能更快判断岗位授权配置是否完整。
     */
    @TableField(exist = false)
    private Integer defaultRoleCount;

    /**
     * 岗位默认角色摘要。
     * 只展示前两个角色名，其余数量折叠成 +N，兼顾可读性和列表紧凑度。
     */
    @TableField(exist = false)
    private String defaultRoleSummary;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
