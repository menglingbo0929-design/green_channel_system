package com.example.backend.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-角色关联表 sys_user_role
 *
 * 一个用户可以拥有多个角色。
 * 例如：某学生兼职辅导员 → 同时有 STUDENT 和 COUNSELOR 两条记录
 */
@Data
@TableName("sys_user_role")
public class UserRole {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 ID，关联 sys_user.id */
    @TableField("user_id")
    private Long userId;

    /** 角色 ID，关联 sys_role.id */
    @TableField("role_id")
    private Long roleId;

    @TableField("create_time")
    private LocalDateTime createTime;
}
