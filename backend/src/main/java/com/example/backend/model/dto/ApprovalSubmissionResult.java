package com.example.backend.model.dto;

import com.example.backend.model.domain.BatchType;
import com.example.backend.model.domain.SubmissionLevel;
import com.example.backend.model.domain.SubmissionType;

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
