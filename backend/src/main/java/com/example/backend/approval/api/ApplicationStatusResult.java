package com.example.backend.approval.api;

import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApprovalLevel;

public record ApplicationStatusResult(
        Long applicationId,
        ApplicationStatus status,
        ApprovalLevel currentLevel,
        Integer version
) {
}
