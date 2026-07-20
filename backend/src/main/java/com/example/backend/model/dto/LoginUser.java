package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息
 *
 * 从 JWT Token 中解析，由 CurrentUserProvider 返回给所有需要用户身份的模块。
 *
 * 字段来源：
 *   userId    ← JWT claim "userId"
 *   loginName  ← JWT subject
 *   roles      ← JWT claim "roles"（逗号分隔后拆分）
 *   studentId  ← 预留，等 student 表建好后补
 *   collegeId  ← 预留，等 college 建好后补
 */
@Data
@AllArgsConstructor
public class LoginUser {
    private Long userId;
    private String loginName;
    private List<String> roles;
    // TODO: 等 student 表和 college 表建好后补充
    // private Long studentId;
    // private Long collegeId;
}
