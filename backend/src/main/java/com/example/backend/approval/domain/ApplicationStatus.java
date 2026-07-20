package com.example.backend.approval.domain;

import java.util.EnumSet;
import java.util.Set;

public enum ApplicationStatus {
    DRAFT(ApprovalLevel.STUDENT),
    COUNSELOR_PENDING(ApprovalLevel.COUNSELOR),
    COUNSELOR_RETURNED(ApprovalLevel.STUDENT),
    COLLEGE_PENDING(ApprovalLevel.COLLEGE),
    COLLEGE_RETURNED(ApprovalLevel.STUDENT),
    SCHOOL_PENDING(ApprovalLevel.SCHOOL),
    SCHOOL_RETURNED(ApprovalLevel.STUDENT),
    REJECTED(ApprovalLevel.FINISHED),
    APPROVED(ApprovalLevel.FINISHED),
    CONFIRM_PENDING(ApprovalLevel.CONFIRMATION),
    COMPLETED(ApprovalLevel.FINISHED),
    CANCELLED(ApprovalLevel.FINISHED);

    private static final Set<ApplicationStatus> RETURNED_STATUSES = EnumSet.of(
            COUNSELOR_RETURNED,
            COLLEGE_RETURNED,
            SCHOOL_RETURNED
    );

    private static final Set<ApplicationStatus> CANCELLABLE_STATUSES = EnumSet.of(
            APPROVED,
            CONFIRM_PENDING,
            COMPLETED
    );

    private final ApprovalLevel level;

    ApplicationStatus(ApprovalLevel level) {
        this.level = level;
    }

    public ApprovalLevel level() {
        return level;
    }

    public boolean isReturned() {
        return RETURNED_STATUSES.contains(this);
    }

    public boolean isCancellable() {
        return CANCELLABLE_STATUSES.contains(this);
    }
}
