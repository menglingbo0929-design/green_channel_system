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
        return ResponseEntity.badRequest().body(Map.of(
                "code", exception.getCode().name(),
                "message", exception.getMessage()
        ));
    }

    @ExceptionHandler(ApprovalIntegrationUnavailableException.class)
    ResponseEntity<Map<String, String>> handleUnavailable(ApprovalIntegrationUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "code", "APPROVAL_INTEGRATION_UNAVAILABLE",
                "message", exception.getMessage()
        ));
    }
}
