package com.example.entity;

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
@TableName("sys_department")
public class Department implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String code;
    private Long parentId;
    /**
     * 部门负责人用户 ID。
     * 相比直接存负责人姓名，这种结构化绑定方式更适合后续做审批、负责人联动筛选和组织画像展示。
     */
    private Long leaderUserId;

    /**
     * 部门负责人展示名称。
     * 当前仍保留这个字段，既兼容历史数据，也方便前端树节点直接展示负责人姓名。
     */
    private String leader;
    private String phone;
    private Integer sort;
    private Integer status;

    /**
     * 部门当前绑定的用户数量。
     * 列表页会用它快速展示部门规模，不参与数据库持久化。
     */
    @TableField(exist = false)
    private Integer userCount;

    /**
     * 树形部门结构的子节点。
     * 前端部门树和部门选择器都会直接消费这个字段。
     */
    @TableField(exist = false)
    private List<Department> children;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
