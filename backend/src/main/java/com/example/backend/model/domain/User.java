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

    @Schema(description = "用户名")
    @TableField("login_name")
    private String loginName;

    @TableField("password")
    private String password;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField("remark")
    private String remark;

    @Schema(description = "逻辑删除，0=有效，非0=已删除")
    @TableField("deleted")
    @TableLogic(value = "0", delval = "1")
    private Long deleted;

    @Schema(description = "创建时间")
    @TableField("gmt_created")
    private LocalDateTime gmtCreated;

    @Schema(description = "更新时间")
    @TableField("gmt_modified")
    private LocalDateTime gmtModified;
}
