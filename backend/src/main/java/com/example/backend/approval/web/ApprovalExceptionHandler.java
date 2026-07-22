package com.example.backend.approval.web;

import com.example.backend.approval.domain.ApprovalException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApprovalExceptionHandler {

    @ExceptionHandler(ApprovalException.class)
    ResponseEntity<Map<String, String>> handleApproval(ApprovalException exception) {
        return ResponseEntity.status(statusFor(exception)).body(Map.of(
                "code", exception.getCode().name(),
                "message", exception.getMessage()
        ));
    }

    private HttpStatus statusFor(ApprovalException exception) {
        return switch (exception.getCode()) {
            case APPROVAL_FORBIDDEN_SCOPE -> HttpStatus.FORBIDDEN;
            case APPROVAL_INVALID_STATUS,
                    APPROVAL_ALREADY_PROCESSED,
                    APPROVAL_VERSION_CONFLICT,
                    APPROVAL_UNREVIEWED_EXISTS,
                    APPROVAL_BATCH_NOT_CLOSED,
                    APPROVAL_BATCH_ALREADY_SUBMITTED,
                    APPROVAL_COLLEGE_DEADLINE_EXPIRED,
                    APPROVAL_QUOTA_INSUFFICIENT,
                    APPROVAL_CANCEL_NOT_ALLOWED -> HttpStatus.CONFLICT;
            case APPROVAL_RESOURCE_ROLLBACK_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    @ExceptionHandler(ApprovalIntegrationUnavailableException.class)
    ResponseEntity<Map<String, String>> handleUnavailable(ApprovalIntegrationUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "code", "APPROVAL_INTEGRATION_UNAVAILABLE",
                "message", exception.getMessage()
        ));
    }
}
