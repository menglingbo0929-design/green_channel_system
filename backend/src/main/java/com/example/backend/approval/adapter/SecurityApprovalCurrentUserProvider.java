package com.example.backend.approval.adapter;

import com.example.backend.approval.domain.ApprovalErrorCode;
import com.example.backend.approval.domain.ApprovalException;
import com.example.backend.approval.port.CurrentUserProvider;
import com.example.backend.approval.port.LoginUser;
import com.example.backend.approval.port.UserRole;
import com.example.backend.security.ICurrentUserProvider;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/** Adapts the authenticated JWT context to the approval module's trusted identity boundary. */
@Primary
@Service
public class SecurityApprovalCurrentUserProvider implements CurrentUserProvider {

    private final ICurrentUserProvider currentUserProvider;

    public SecurityApprovalCurrentUserProvider(ICurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public LoginUser getRequiredUser() {
        var user = currentUserProvider.getRequiredUser();
        if (user.getUserId() == null) throw forbidden("JWT 中缺少用户标识");
        Set<UserRole> roles = EnumSet.noneOf(UserRole.class);
        if (user.getRoles() != null) for (String value : user.getRoles()) {
            try { roles.add(UserRole.valueOf(value)); } catch (IllegalArgumentException ignored) { }
        }
        if (roles.size() != 1) throw forbidden("当前登录用户必须具备且仅具备一个审批业务角色");
        UserRole role = roles.iterator().next();
        if (role == UserRole.STUDENT && user.getStudentId() == null) throw forbidden("当前学生账号未关联学生档案");
        if (role == UserRole.COLLEGE && user.getCollegeId() == null) throw forbidden("当前学院账号未关联学院范围");
        return new LoginUser(user.getUserId(), role, user.getStudentId(), user.getCollegeId());
    }

    private ApprovalException forbidden(String message) {
        return new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, message);
    }
}
