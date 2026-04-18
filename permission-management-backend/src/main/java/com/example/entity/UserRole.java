package com.example.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

// 适配 sys_user_role 联合主键、无 deleted 字段的特性
@Data
@TableName("sys_user_role")
public class UserRole {

    // 联合主键：user_id（对应数据库字段）
    @TableField("user_id")
    private Long userId;

    // 联合主键：role_id（对应数据库字段）
    @TableField("role_id")
    private Long roleId;

}