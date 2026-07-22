package com.example.backend.approval.integration;

import com.example.backend.approval.domain.ApprovalErrorCode;
import com.example.backend.approval.domain.ApprovalException;
import com.example.backend.approval.port.LoginUser;
import com.example.backend.approval.port.UserRole;
import com.example.backend.security.ICurrentUserProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApprovalCurrentUserProviderAdapterTest {

    private final ICurrentUserProvider delegate = mock(ICurrentUserProvider.class);
    private final ApprovalCurrentUserProviderAdapter adapter =
            new ApprovalCurrentUserProviderAdapter(delegate);

    @Test
    void mapsTrustedIdentityAndNormalizesRolePrefix() {
        when(delegate.getRequiredUser()).thenReturn(new com.example.backend.model.dto.LoginUser(
                11L, "counselor", List.of("ROLE_COUNSELOR"), null, 8L
        ));

        LoginUser result = adapter.getRequiredUser();

        assertEquals(11L, result.userId());
        assertEquals(UserRole.COUNSELOR, result.role());
        assertEquals(8L, result.collegeId());
    }

    @Test
    void rejectsAmbiguousMultipleApprovalRoles() {
        when(delegate.getRequiredUser()).thenReturn(new com.example.backend.model.dto.LoginUser(
                11L, "multi-role", List.of("COUNSELOR", "SCHOOL"), null, 8L
        ));

        ApprovalException exception = assertThrows(
                ApprovalException.class,
                adapter::getRequiredUser
        );

        assertEquals(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, exception.getCode());
    }

    @Test
    void requiresStudentIdForStudentRole() {
        when(delegate.getRequiredUser()).thenReturn(new com.example.backend.model.dto.LoginUser(
                12L, "student", List.of("STUDENT"), null, null
        ));

        ApprovalException exception = assertThrows(
                ApprovalException.class,
                adapter::getRequiredUser
        );

        assertEquals(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, exception.getCode());
    }
}
