package com.example.backend.approval.integration;

import com.example.backend.approval.domain.ApprovalErrorCode;
import com.example.backend.approval.domain.ApprovalException;
import com.example.backend.approval.port.CurrentUserProvider;
import com.example.backend.approval.port.LoginUser;
import com.example.backend.approval.port.UserRole;
import com.example.backend.security.ICurrentUserProvider;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Adapts the shared trusted Spring Security identity to the approval contract.
 *
 * <p>The approval contract currently represents one active role. Refusing an
 * ambiguous multi-role identity is safer than silently selecting a role with
 * broader privileges. A future role-switching contract can replace this guard
 * without changing the shared authentication implementation.</p>
 */
@Component
public class ApprovalCurrentUserProviderAdapter implements CurrentUserProvider {

    private final ICurrentUserProvider delegate;

    public ApprovalCurrentUserProviderAdapter(ICurrentUserProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public LoginUser getRequiredUser() {
        com.example.backend.model.dto.LoginUser source = delegate.getRequiredUser();
        if (source == null || source.getUserId() == null) {
            throw forbidden("当前登录身份缺少用户 ID");
        }

        List<UserRole> roles = source.getRoles() == null
                ? List.of()
                : source.getRoles().stream()
                        .map(this::toApprovalRole)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList();
        if (roles.isEmpty()) {
            throw forbidden("当前用户没有审批系统角色");
        }
        if (roles.size() != 1) {
            throw forbidden("当前用户拥有多个审批角色，请先完成审批角色选择能力");
        }

        UserRole role = roles.getFirst();
        if (role == UserRole.STUDENT && source.getStudentId() == null) {
            throw forbidden("学生身份缺少 studentId");
        }
        if (role == UserRole.COLLEGE && source.getCollegeId() == null) {
            throw forbidden("学院身份缺少 collegeId");
        }
        return new LoginUser(source.getUserId(), role, source.getStudentId(), source.getCollegeId());
    }

    private UserRole toApprovalRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return null;
        }
        String normalized = rawRole.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        try {
            return UserRole.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private ApprovalException forbidden(String message) {
        return new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, message);
    }
}
