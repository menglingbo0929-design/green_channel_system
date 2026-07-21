package com.example.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建用户请求
 */
@Data
public class CreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    private String loginName;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String remark;

    @NotEmpty(message = "角色不能为空")
    @Size(max = 1, message = "每个用户只能选择一个角色")
    private List<Long> roleIds;
}
