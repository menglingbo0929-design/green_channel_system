package com.example.backend.approval.api;

import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApprovalLevel;

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
