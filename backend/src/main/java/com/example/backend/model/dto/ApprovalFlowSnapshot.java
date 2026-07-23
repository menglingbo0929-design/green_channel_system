package com.example.backend.model.dto;

import com.example.backend.model.domain.ApplicationStatus;
import com.example.backend.model.domain.ApprovalLevel;

import java.util.List;

public record ApprovalFlowSnapshot(
        Long applicationId,
        ApplicationStatus status,
        ApprovalLevel currentLevel,
        String returnReason,
        String rejectReason,
        List<ApprovalRecordSnapshot> records
) {
    public ApprovalFlowSnapshot {
        records = List.copyOf(records);
    }
}
