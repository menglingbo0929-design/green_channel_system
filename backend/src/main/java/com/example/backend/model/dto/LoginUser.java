package com.example.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.example.backend.model.domain.UserRole;

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

    /** 审批模块也直接使用这一份可信登录快照，不再维护第二套 LoginUser。 */
    public LoginUser(Long userId, UserRole role, Long studentId, Long collegeId) {
        this(userId, null, role == null ? List.of() : List.of(role.name()), studentId, collegeId);
    }

    public Long userId() { return userId; }
    public Long studentId() { return studentId; }
    public Long collegeId() { return collegeId; }

    public UserRole role() {
        List<UserRole> resolved = roles == null ? List.of() : roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .map(value -> value.startsWith("ROLE_") ? value.substring(5) : value)
                .map(value -> {
                    try { return UserRole.valueOf(value); }
                    catch (IllegalArgumentException ignored) { return null; }
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (resolved.size() != 1) {
            throw new SecurityException("当前用户必须且只能具有一个业务角色");
        }
        UserRole role = resolved.getFirst();
        if (role == UserRole.STUDENT && studentId == null) {
            throw new SecurityException("学生身份缺少 studentId");
        }
        if (role == UserRole.COLLEGE && collegeId == null) {
            throw new SecurityException("学院身份缺少 collegeId");
        }
        return role;
    }
}
