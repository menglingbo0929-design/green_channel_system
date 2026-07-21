package com.example.backend.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@TableName("sys_user")
public class User {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "Username")
    @TableField("login_name")
    private String loginName;

    @TableField("password")
    private String password;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField("remark")
    private String remark;

    @Schema(description = "Logical deletion flag")
    @TableField("deleted")
    private Long deleted;

    @Schema(description = "Created at")
    @TableField("gmt_created")
    private LocalDateTime gmtCreated;

    @Schema(description = "Updated at")
    @TableField("gmt_modified")
    private LocalDateTime gmtModified;
}
