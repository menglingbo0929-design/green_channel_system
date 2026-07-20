package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户列表响应——不含密码
 */
@Data
@AllArgsConstructor
public class UserVO {
    private Long id;
    private String loginName;
    private String remark;
    private List<String> roles;
    private List<Long> roleIds;
    private Long deleted;         // 0=启用, 非0=停用
    private LocalDateTime lastLoginTime;
    private LocalDateTime gmtCreated;
}
