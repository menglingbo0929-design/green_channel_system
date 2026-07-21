package com.example.backend.approval.api;

import com.example.backend.approval.persistence.type.BatchType;
import com.example.backend.approval.persistence.type.SubmissionLevel;

import java.time.LocalDateTime;

/** Status shown to the current counselor or college administrator for one batch. */
public record ApprovalSubmissionStatus(
        BatchType batchType,
        Long batchId,
        SubmissionLevel submissionLevel,
        LocalDateTime applicationDeadline,
        LocalDateTime collegeDeadline,
        boolean initialSubmitted,
        LocalDateTime submittedAt,
        int pendingReviewCount,
        int approvedWaitingSubmitCount,
        int returnedCount,
        int rejectedCount,
        boolean canSubmit
) {
}
