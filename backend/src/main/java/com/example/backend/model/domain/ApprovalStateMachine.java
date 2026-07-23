package com.example.backend.model.domain;

import com.example.backend.common.exception.ApprovalStateException;
import com.example.backend.common.exception.ApprovalErrorCode;
import com.example.backend.model.domain.ApplicationStatus;
import com.example.backend.model.domain.ApplicationType;
import com.example.backend.model.domain.ApprovalLevel;

import java.util.Objects;

import static com.example.backend.model.domain.ApplicationStatus.APPROVED;
import static com.example.backend.model.domain.ApplicationStatus.CANCELLED;
import static com.example.backend.model.domain.ApplicationStatus.COLLEGE_PENDING;
import static com.example.backend.model.domain.ApplicationStatus.COLLEGE_RETURNED;
import static com.example.backend.model.domain.ApplicationStatus.COMPLETED;
import static com.example.backend.model.domain.ApplicationStatus.CONFIRM_PENDING;
import static com.example.backend.model.domain.ApplicationStatus.COUNSELOR_PENDING;
import static com.example.backend.model.domain.ApplicationStatus.COUNSELOR_RETURNED;
import static com.example.backend.model.domain.ApplicationStatus.DRAFT;
import static com.example.backend.model.domain.ApplicationStatus.REJECTED;
import static com.example.backend.model.domain.ApplicationStatus.SCHOOL_PENDING;
import static com.example.backend.model.domain.ApplicationStatus.SCHOOL_RETURNED;

public final class ApprovalStateMachine {

    public ApprovalTransition submitInitial(ApplicationStatus currentStatus) {
        requireStatus(currentStatus, DRAFT, "initial submission");
        return transition(currentStatus, COUNSELOR_PENDING, 0);
    }

    public ApprovalTransition resubmitReturned(ApplicationStatus currentStatus) {
        requireNonNull(currentStatus, "currentStatus");
        if (!currentStatus.isReturned()) {
            throw invalid(currentStatus, "returned application resubmission");
        }
        return transition(currentStatus, COUNSELOR_PENDING, 1);
    }

    public ApprovalTransition review(
            ApplicationStatus currentStatus,
            ApprovalLevel reviewerLevel,
            ApprovalAction action,
            ApplicationType applicationType,
            boolean containsArrears
    ) {
        requireNonNull(currentStatus, "currentStatus");
        requireNonNull(reviewerLevel, "reviewerLevel");
        requireNonNull(action, "action");

        if (action != ApprovalAction.APPROVE
                && action != ApprovalAction.RETURN
                && action != ApprovalAction.REJECT) {
            throw invalid(currentStatus, "review action " + action);
        }

        return switch (reviewerLevel) {
            case COUNSELOR -> reviewAtPendingLevel(
                    currentStatus,
                    COUNSELOR_PENDING,
                    COUNSELOR_RETURNED,
                    action
            );
            case COLLEGE -> reviewAtPendingLevel(
                    currentStatus,
                    COLLEGE_PENDING,
                    COLLEGE_RETURNED,
                    action
            );
            case SCHOOL -> reviewAtSchool(
                    currentStatus,
                    action,
                    applicationType,
                    containsArrears
            );
            default -> throw invalid(currentStatus, "review by level " + reviewerLevel);
        };
    }

    public ApprovalTransition modifyAllowedFields(
            ApplicationStatus currentStatus,
            ApprovalLevel reviewerLevel
    ) {
        requireNonNull(currentStatus, "currentStatus");
        requireNonNull(reviewerLevel, "reviewerLevel");

        ApplicationStatus expectedStatus = switch (reviewerLevel) {
            case COUNSELOR -> COUNSELOR_PENDING;
            case COLLEGE -> COLLEGE_PENDING;
            case SCHOOL -> SCHOOL_PENDING;
            default -> throw invalid(currentStatus, "field modification by level " + reviewerLevel);
        };
        requireStatus(currentStatus, expectedStatus, "field modification");
        return transition(currentStatus, currentStatus, 0);
    }

    public ApprovalTransition submitApprovedToNextLevel(
            ApplicationStatus currentStatus,
            ApprovalLevel submitterLevel
    ) {
        requireNonNull(currentStatus, "currentStatus");
        requireNonNull(submitterLevel, "submitterLevel");

        return switch (submitterLevel) {
            case COUNSELOR -> {
                requireStatus(currentStatus, COUNSELOR_PENDING, "counselor submission");
                yield transition(currentStatus, COLLEGE_PENDING, 0);
            }
            case COLLEGE -> {
                requireStatus(currentStatus, COLLEGE_PENDING, "college submission");
                yield transition(currentStatus, SCHOOL_PENDING, 0);
            }
            default -> throw invalid(currentStatus, "submission by level " + submitterLevel);
        };
    }

    public ApprovalTransition completeAfterConfirmation(ApplicationStatus currentStatus) {
        requireStatus(currentStatus, CONFIRM_PENDING, "arrears confirmation completion");
        return transition(currentStatus, COMPLETED, 0);
    }

    public ApprovalTransition completeSupplementReview(
            ApplicationStatus currentStatus,
            boolean containsArrears
    ) {
        requireStatus(currentStatus, DRAFT, "supplement automatic review");
        return transition(currentStatus, containsArrears ? CONFIRM_PENDING : COMPLETED, 0);
    }

    public ApprovalTransition cancel(ApplicationStatus currentStatus) {
        requireNonNull(currentStatus, "currentStatus");
        if (!currentStatus.isCancellable()) {
            throw invalid(currentStatus, "school cancellation");
        }
        return transition(currentStatus, CANCELLED, 0);
    }

    public ApprovalTransition completeApproved(ApplicationStatus currentStatus) {
        requireStatus(currentStatus, APPROVED, "post-approval completion");
        return transition(currentStatus, COMPLETED, 0);
    }

    private ApprovalTransition reviewAtPendingLevel(
            ApplicationStatus currentStatus,
            ApplicationStatus expectedStatus,
            ApplicationStatus returnedStatus,
            ApprovalAction action
    ) {
        requireStatus(currentStatus, expectedStatus, "review at " + expectedStatus.level());
        ApplicationStatus targetStatus = switch (action) {
            case APPROVE -> currentStatus;
            case RETURN -> returnedStatus;
            case REJECT -> REJECTED;
            default -> throw invalid(currentStatus, "review action " + action);
        };
        return transition(currentStatus, targetStatus, 0);
    }

    private ApprovalTransition reviewAtSchool(
            ApplicationStatus currentStatus,
            ApprovalAction action,
            ApplicationType applicationType,
            boolean containsArrears
    ) {
        requireStatus(currentStatus, SCHOOL_PENDING, "school review");
        ApplicationStatus targetStatus = switch (action) {
            case APPROVE -> schoolApprovalTarget(applicationType, containsArrears);
            case RETURN -> SCHOOL_RETURNED;
            case REJECT -> REJECTED;
            default -> throw invalid(currentStatus, "school review action " + action);
        };
        return transition(currentStatus, targetStatus, 0);
    }

    private ApplicationStatus schoolApprovalTarget(
            ApplicationType applicationType,
            boolean containsArrears
    ) {
        requireNonNull(applicationType, "applicationType");
        if (applicationType == ApplicationType.GREEN_CHANNEL && containsArrears) {
            return CONFIRM_PENDING;
        }
        return APPROVED;
    }

    private ApprovalTransition transition(
            ApplicationStatus sourceStatus,
            ApplicationStatus targetStatus,
            int reviewRoundDelta
    ) {
        return new ApprovalTransition(
                sourceStatus,
                targetStatus,
                sourceStatus.level(),
                targetStatus.level(),
                reviewRoundDelta
        );
    }

    private void requireStatus(
            ApplicationStatus currentStatus,
            ApplicationStatus expectedStatus,
            String operation
    ) {
        requireNonNull(currentStatus, "currentStatus");
        if (currentStatus != expectedStatus) {
            throw invalid(currentStatus, operation);
        }
    }

    private void requireNonNull(Object value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
    }

    private ApprovalStateException invalid(ApplicationStatus currentStatus, String operation) {
        return new ApprovalStateException(
                ApprovalErrorCode.APPROVAL_INVALID_STATUS,
                "Status " + currentStatus + " does not allow " + operation
        );
    }
}
