package com.example.backend.model.dto;

import com.example.backend.model.domain.ApplicationStatus;
import com.example.backend.model.domain.ApplicationType;
import com.example.backend.model.domain.BatchType;

/** Filters shared by pending, processed, and dashboard workbench queries. */
public record ApprovalListQuery(
        int page,
        int size,
        BatchType batchType,
        Long batchId,
        ApplicationType applicationType,
        String applicationNo,
        String studentNo,
        String studentName,
        Long collegeId,
        ApplicationStatus status
) {
    public ApprovalListQuery {
        if (page < 1) throw new IllegalArgumentException("page must be at least 1");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be between 1 and 100");
        if ((batchType == null) != (batchId == null)) {
            throw new IllegalArgumentException("batchType and batchId must be supplied together");
        }
    }

    public ApprovalListQuery withStatus(ApplicationStatus targetStatus) {
        return new ApprovalListQuery(page, size, batchType, batchId, applicationType, applicationNo,
                studentNo, studentName, collegeId, targetStatus);
    }
}
