package com.example.backend.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

@TableName("user")
public class User {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "用户名")
    @TableField("login_name")
    private String loginName;

    @TableField("password")
    private String password;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField("remark")
    private String remark;

    @ApiModelProperty(value = "是否删除")
    @TableField("is_deleted")
    @TableLogic
    private Boolean deleted;

    @ApiModelProperty(value = "创建时间")
    @TableField("gmt_created")
    private LocalDateTime gmtCreated;

    @ApiModelProperty(value = "更新时间")
    @TableField("gmt_modified")
    private LocalDateTime gmtModified;
}
