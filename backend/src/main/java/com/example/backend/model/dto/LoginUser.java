package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息 —— 对齐 docs/member-code-contracts.md
 *
 * 字段来源：JWT Token
 *   userId     ← claim "userId"
 *   loginName   ← subject
 *   roles       ← claim "roles"
 *   studentId   ← claim "studentId"（学生角色时非空）
 *   collegeId   ← claim "collegeId"（学院角色时非空）
 */
@Data
@AllArgsConstructor
public class LoginUser {
    private Long userId;
    private String loginName;
    private List<String> roles;
    /** 学生 ID，仅 STUDENT 角色时非空 */
    private Long studentId;
    /** 学院 ID，COLLEGE 或 COUNSELOR 角色时非空 */
    private Long collegeId;
}
