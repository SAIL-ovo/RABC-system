package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class Role implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String DATA_SCOPE_ALL = "ALL";
    public static final String DATA_SCOPE_DEPT_AND_CHILD = "DEPT_AND_CHILD";
    public static final String DATA_SCOPE_DEPT = "DEPT";
    public static final String DATA_SCOPE_SELF = "SELF";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String code;
    private String description;
    private Integer status;
    /**
     * 角色数据权限范围。
     * 第一版先支持 4 种标准范围：
     * ALL：可查看全部数据；
     * DEPT_AND_CHILD：可查看本部门及下级部门；
     * DEPT：仅可查看本部门；
     * SELF：仅可查看本人。
     */
    private String dataScope;

    /**
     * 角色当前已绑定的权限数量。
     * 这个字段只在列表展示时由后端动态补充，不会落库。
     */
    @TableField(exist = false)
    private Integer permissionCount;

    /**
     * 角色当前已绑定的权限摘要。
     * 列表中只展示精简后的权限名称，完整权限关系仍以分配弹窗为准。
     */
    @TableField(exist = false)
    private String permissionSummary;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /**
     * 把数据范围编码转换成中文描述。
     * 后端写日志、前端做展示时都可以共用这一套映射，避免各处散落重复判断。
     */
    public static String getDataScopeLabel(String dataScope) {
        if (DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope)) {
            return "本部门及下级部门";
        }
        if (DATA_SCOPE_DEPT.equals(dataScope)) {
            return "仅本部门";
        }
        if (DATA_SCOPE_SELF.equals(dataScope)) {
            return "仅本人";
        }
        return "全部数据";
    }
}
