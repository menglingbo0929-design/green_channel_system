package com.example.backend.approval.api;

import com.example.backend.approval.persistence.type.BatchType;
import com.example.backend.approval.persistence.type.SubmissionLevel;
import com.example.backend.approval.persistence.type.SubmissionType;

import java.time.LocalDateTime;

/** Immutable result of one batch submission or returned-application resubmission. */
public record ApprovalSubmissionResult(
        BatchType batchType,
        Long batchId,
        SubmissionLevel submissionLevel,
        SubmissionType submissionType,
        Long applicationId,
        int submittedCount,
        String requestId,
        LocalDateTime submittedAt
) {
}
