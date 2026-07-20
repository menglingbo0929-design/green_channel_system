package com.example.backend.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 更新用户请求（不含密码，密码单独改）
 */
@Data
public class UpdateUserRequest {

    private String loginName;

    private String remark;

    @NotEmpty(message = "角色不能为空")
    private List<Long> roleIds;
}
