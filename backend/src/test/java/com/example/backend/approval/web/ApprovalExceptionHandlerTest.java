package com.example.backend.approval.web;

import com.example.backend.approval.domain.ApprovalErrorCode;
import com.example.backend.approval.domain.ApprovalException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApprovalExceptionHandlerTest {

    private final ApprovalExceptionHandler handler = new ApprovalExceptionHandler();

    @Test
    void mapsForbiddenScopeTo403() {
        var response = handler.handleApproval(new ApprovalException(
                ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE,
                "forbidden"
        ));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("APPROVAL_FORBIDDEN_SCOPE", response.getBody().get("code"));
    }

    @Test
    void mapsVersionConflictTo409() {
        var response = handler.handleApproval(new ApprovalException(
                ApprovalErrorCode.APPROVAL_VERSION_CONFLICT,
                "conflict"
        ));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void keepsValidationErrorsAt400() {
        var response = handler.handleApproval(new ApprovalException(
                ApprovalErrorCode.APPROVAL_COMMENT_REQUIRED,
                "comment required"
        ));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
