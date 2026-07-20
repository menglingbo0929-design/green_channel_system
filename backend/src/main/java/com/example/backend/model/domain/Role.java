package com.example.backend.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色字典表 sys_role
 *
 * 只有 4 条固定数据，基本不变：
 *   STUDENT   - 学生
 *   COUNSELOR - 辅导员
 *   COLLEGE   - 学院管理员
 *   SCHOOL    - 学校管理员
 */
@Data
@TableName("sys_role")
public class Role {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 角色编码，如 STUDENT、COUNSELOR */
    @TableField("role_code")
    private String roleCode;

    /** 角色中文名，如 学生、辅导员 */
    @TableField("role_name")
    private String roleName;

    @TableField("create_time")
    private LocalDateTime createTime;
}
