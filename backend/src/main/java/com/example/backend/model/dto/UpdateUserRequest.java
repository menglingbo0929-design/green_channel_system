package com.example.backend.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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
    @Size(max = 1, message = "每个用户只能选择一个角色")
    private List<Long> roleIds;
}
