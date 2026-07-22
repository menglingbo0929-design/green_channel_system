package com.example.backend.approval.api;

import com.example.backend.application.domain.ApplicationStatus;
import com.example.backend.application.domain.ApprovalLevel;

public record ApplicationStatusResult(
        Long applicationId,
        ApplicationStatus status,
        ApprovalLevel currentLevel,
        Integer version
) {
}
