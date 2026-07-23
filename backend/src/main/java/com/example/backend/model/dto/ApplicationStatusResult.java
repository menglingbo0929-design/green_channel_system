package com.example.backend.model.dto;

import com.example.backend.model.domain.ApplicationStatus;
import com.example.backend.model.domain.ApprovalLevel;

public record ApplicationStatusResult(
        Long applicationId,
        ApplicationStatus status,
        ApprovalLevel currentLevel,
        Integer version
) {
}
