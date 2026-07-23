package com.example.backend.model.domain;

import com.example.backend.model.domain.ApplicationStatus;
import com.example.backend.model.domain.ApprovalLevel;

import java.util.Objects;

public record ApprovalTransition(
        ApplicationStatus sourceStatus,
        ApplicationStatus targetStatus,
        ApprovalLevel sourceLevel,
        ApprovalLevel targetLevel,
        int reviewRoundDelta
) {
    public ApprovalTransition {
        Objects.requireNonNull(sourceStatus, "sourceStatus must not be null");
        Objects.requireNonNull(targetStatus, "targetStatus must not be null");
        Objects.requireNonNull(sourceLevel, "sourceLevel must not be null");
        Objects.requireNonNull(targetLevel, "targetLevel must not be null");
        if (reviewRoundDelta < 0) {
            throw new IllegalArgumentException("reviewRoundDelta must not be negative");
        }
    }

    public boolean changesStatus() {
        return sourceStatus != targetStatus;
    }
}
