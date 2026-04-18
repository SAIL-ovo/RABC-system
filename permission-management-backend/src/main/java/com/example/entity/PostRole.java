package com.example.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("sys_post_role")
public class PostRole implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long postId;

    private Long roleId;
}
