package com.example.backend.model.domain;

import com.example.backend.common.exception.ApprovalErrorCode;
import com.example.backend.common.exception.ApprovalStateException;
import com.example.backend.model.domain.ApplicationStatus;
import com.example.backend.model.domain.ApplicationType;
import com.example.backend.model.domain.ApprovalLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprovalStateMachineTest {

    private ApprovalStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new ApprovalStateMachine();
    }

    @Test
    void mapsEveryStatusToItsApprovedCurrentLevel() {
        Map<ApplicationStatus, ApprovalLevel> expectedLevels = Map.ofEntries(
                Map.entry(ApplicationStatus.DRAFT, ApprovalLevel.STUDENT),
                Map.entry(ApplicationStatus.COUNSELOR_PENDING, ApprovalLevel.COUNSELOR),
                Map.entry(ApplicationStatus.COUNSELOR_RETURNED, ApprovalLevel.STUDENT),
                Map.entry(ApplicationStatus.COLLEGE_PENDING, ApprovalLevel.COLLEGE),
                Map.entry(ApplicationStatus.COLLEGE_RETURNED, ApprovalLevel.STUDENT),
                Map.entry(ApplicationStatus.SCHOOL_PENDING, ApprovalLevel.SCHOOL),
                Map.entry(ApplicationStatus.SCHOOL_RETURNED, ApprovalLevel.STUDENT),
                Map.entry(ApplicationStatus.REJECTED, ApprovalLevel.FINISHED),
                Map.entry(ApplicationStatus.APPROVED, ApprovalLevel.FINISHED),
                Map.entry(ApplicationStatus.CONFIRM_PENDING, ApprovalLevel.CONFIRMATION),
                Map.entry(ApplicationStatus.COMPLETED, ApprovalLevel.FINISHED),
                Map.entry(ApplicationStatus.CANCELLED, ApprovalLevel.FINISHED)
        );

        expectedLevels.forEach((status, level) -> assertEquals(level, status.level()));
    }

    @Test
    void submitsDraftToCounselorWithoutChangingReviewRound() {
        ApprovalTransition transition = stateMachine.submitInitial(ApplicationStatus.DRAFT);

        assertTransition(
                transition,
                ApplicationStatus.DRAFT,
                ApplicationStatus.COUNSELOR_PENDING,
                0
        );
    }

    @Test
    void resubmitsEveryReturnedStatusToCounselorAndIncrementsReviewRound() {
        for (ApplicationStatus returnedStatus : new ApplicationStatus[]{
                ApplicationStatus.COUNSELOR_RETURNED,
                ApplicationStatus.COLLEGE_RETURNED,
                ApplicationStatus.SCHOOL_RETURNED
        }) {
            ApprovalTransition transition = stateMachine.resubmitReturned(returnedStatus);

            assertTransition(
                    transition,
                    returnedStatus,
                    ApplicationStatus.COUNSELOR_PENDING,
                    1
            );
        }
    }

    @Test
    void counselorApprovalRecordsDecisionWithoutChangingStatus() {
        ApprovalTransition transition = stateMachine.review(
                ApplicationStatus.COUNSELOR_PENDING,
                ApprovalLevel.COUNSELOR,
                ApprovalAction.APPROVE,
                ApplicationType.GREEN_CHANNEL,
                true
        );

        assertTransition(
                transition,
                ApplicationStatus.COUNSELOR_PENDING,
                ApplicationStatus.COUNSELOR_PENDING,
                0
        );
        assertFalse(transition.changesStatus());
    }

    @Test
    void collegeApprovalRecordsDecisionWithoutChangingStatus() {
        ApprovalTransition transition = stateMachine.review(
                ApplicationStatus.COLLEGE_PENDING,
                ApprovalLevel.COLLEGE,
                ApprovalAction.APPROVE,
                ApplicationType.LIVING_SUBSIDY,
                false
        );

        assertTransition(
                transition,
                ApplicationStatus.COLLEGE_PENDING,
                ApplicationStatus.COLLEGE_PENDING,
                0
        );
    }

    @Test
    void returnsAtEachReviewLevelToTheMatchingStudentEditableStatus() {
        assertEquals(
                ApplicationStatus.COUNSELOR_RETURNED,
                review(ApplicationStatus.COUNSELOR_PENDING, ApprovalLevel.COUNSELOR, ApprovalAction.RETURN)
                        .targetStatus()
        );
        assertEquals(
                ApplicationStatus.COLLEGE_RETURNED,
                review(ApplicationStatus.COLLEGE_PENDING, ApprovalLevel.COLLEGE, ApprovalAction.RETURN)
                        .targetStatus()
        );
        assertEquals(
                ApplicationStatus.SCHOOL_RETURNED,
                review(ApplicationStatus.SCHOOL_PENDING, ApprovalLevel.SCHOOL, ApprovalAction.RETURN)
                        .targetStatus()
        );
    }

    @Test
    void rejectionAtEveryReviewLevelEndsTheFlow() {
        assertEquals(
                ApplicationStatus.REJECTED,
                review(ApplicationStatus.COUNSELOR_PENDING, ApprovalLevel.COUNSELOR, ApprovalAction.REJECT)
                        .targetStatus()
        );
        assertEquals(
                ApplicationStatus.REJECTED,
                review(ApplicationStatus.COLLEGE_PENDING, ApprovalLevel.COLLEGE, ApprovalAction.REJECT)
                        .targetStatus()
        );
        assertEquals(
                ApplicationStatus.REJECTED,
                review(ApplicationStatus.SCHOOL_PENDING, ApprovalLevel.SCHOOL, ApprovalAction.REJECT)
                        .targetStatus()
        );
    }

    @Test
    void schoolApprovalSendsGreenChannelWithArrearsToConfirmation() {
        ApprovalTransition transition = stateMachine.review(
                ApplicationStatus.SCHOOL_PENDING,
                ApprovalLevel.SCHOOL,
                ApprovalAction.APPROVE,
                ApplicationType.GREEN_CHANNEL,
                true
        );

        assertTransition(
                transition,
                ApplicationStatus.SCHOOL_PENDING,
                ApplicationStatus.CONFIRM_PENDING,
                0
        );
    }

    @Test
    void schoolApprovalEndsOtherFirstStageApplicationsAtApproved() {
        for (ApplicationType applicationType : ApplicationType.values()) {
            boolean containsArrears = applicationType != ApplicationType.GREEN_CHANNEL;
            ApprovalTransition transition = stateMachine.review(
                    ApplicationStatus.SCHOOL_PENDING,
                    ApprovalLevel.SCHOOL,
                    ApprovalAction.APPROVE,
                    applicationType,
                    containsArrears
            );

            assertEquals(ApplicationStatus.APPROVED, transition.targetStatus());
            assertEquals(ApprovalLevel.FINISHED, transition.targetLevel());
        }
    }

    @Test
    void approvedDecisionCanBeSubmittedToTheNextReviewLevel() {
        assertTransition(
                stateMachine.submitApprovedToNextLevel(
                        ApplicationStatus.COUNSELOR_PENDING,
                        ApprovalLevel.COUNSELOR
                ),
                ApplicationStatus.COUNSELOR_PENDING,
                ApplicationStatus.COLLEGE_PENDING,
                0
        );
        assertTransition(
                stateMachine.submitApprovedToNextLevel(
                        ApplicationStatus.COLLEGE_PENDING,
                        ApprovalLevel.COLLEGE
                ),
                ApplicationStatus.COLLEGE_PENDING,
                ApplicationStatus.SCHOOL_PENDING,
                0
        );
    }

    @Test
    void allowedFieldModificationDoesNotChangeStatus() {
        assertFalse(stateMachine.modifyAllowedFields(
                ApplicationStatus.COUNSELOR_PENDING,
                ApprovalLevel.COUNSELOR
        ).changesStatus());
        assertFalse(stateMachine.modifyAllowedFields(
                ApplicationStatus.COLLEGE_PENDING,
                ApprovalLevel.COLLEGE
        ).changesStatus());
        assertFalse(stateMachine.modifyAllowedFields(
                ApplicationStatus.SCHOOL_PENDING,
                ApprovalLevel.SCHOOL
        ).changesStatus());
    }

    @Test
    void confirmationCompletesOnlyConfirmPendingApplication() {
        assertTransition(
                stateMachine.completeAfterConfirmation(ApplicationStatus.CONFIRM_PENDING),
                ApplicationStatus.CONFIRM_PENDING,
                ApplicationStatus.COMPLETED,
                0
        );
    }

    @Test
    void supplementAutomaticReviewUsesArrearsMapping() {
        assertEquals(
                ApplicationStatus.CONFIRM_PENDING,
                stateMachine.completeSupplementReview(ApplicationStatus.DRAFT, true).targetStatus()
        );
        assertEquals(
                ApplicationStatus.COMPLETED,
                stateMachine.completeSupplementReview(ApplicationStatus.DRAFT, false).targetStatus()
        );
    }

    @Test
    void schoolCanCancelOnlyApprovedConfirmationPendingOrCompleted() {
        for (ApplicationStatus cancellableStatus : new ApplicationStatus[]{
                ApplicationStatus.APPROVED,
                ApplicationStatus.CONFIRM_PENDING,
                ApplicationStatus.COMPLETED
        }) {
            assertEquals(
                    ApplicationStatus.CANCELLED,
                    stateMachine.cancel(cancellableStatus).targetStatus()
            );
        }
    }

    @Test
    void optionalPostApprovalCompletionEndsAtCompleted() {
        assertTransition(
                stateMachine.completeApproved(ApplicationStatus.APPROVED),
                ApplicationStatus.APPROVED,
                ApplicationStatus.COMPLETED,
                0
        );
    }

    @Test
    void rejectsWrongStatusOrRoleForReviewAndSubmission() {
        assertInvalid(() -> stateMachine.review(
                ApplicationStatus.COLLEGE_PENDING,
                ApprovalLevel.COUNSELOR,
                ApprovalAction.APPROVE,
                ApplicationType.GREEN_CHANNEL,
                false
        ));
        assertInvalid(() -> stateMachine.review(
                ApplicationStatus.COUNSELOR_PENDING,
                ApprovalLevel.STUDENT,
                ApprovalAction.APPROVE,
                ApplicationType.GREEN_CHANNEL,
                false
        ));
        assertInvalid(() -> stateMachine.review(
                ApplicationStatus.COUNSELOR_PENDING,
                ApprovalLevel.COUNSELOR,
                ApprovalAction.CANCEL,
                ApplicationType.GREEN_CHANNEL,
                false
        ));
        assertInvalid(() -> stateMachine.submitApprovedToNextLevel(
                ApplicationStatus.SCHOOL_PENDING,
                ApprovalLevel.SCHOOL
        ));
    }

    @Test
    void rejectsIllegalLifecycleTransitions() {
        assertInvalid(() -> stateMachine.submitInitial(ApplicationStatus.COUNSELOR_PENDING));
        assertInvalid(() -> stateMachine.resubmitReturned(ApplicationStatus.REJECTED));
        assertInvalid(() -> stateMachine.completeAfterConfirmation(ApplicationStatus.APPROVED));
        assertInvalid(() -> stateMachine.completeSupplementReview(ApplicationStatus.COMPLETED, false));
        assertInvalid(() -> stateMachine.cancel(ApplicationStatus.SCHOOL_PENDING));
        assertInvalid(() -> stateMachine.completeApproved(ApplicationStatus.COMPLETED));
    }

    private ApprovalTransition review(
            ApplicationStatus status,
            ApprovalLevel level,
            ApprovalAction action
    ) {
        return stateMachine.review(
                status,
                level,
                action,
                ApplicationType.GREEN_CHANNEL,
                false
        );
    }

    private void assertInvalid(Executable executable) {
        ApprovalStateException exception = assertThrows(ApprovalStateException.class, executable);
        assertEquals(ApprovalErrorCode.APPROVAL_INVALID_STATUS, exception.getCode());
    }

    private void assertTransition(
            ApprovalTransition transition,
            ApplicationStatus source,
            ApplicationStatus target,
            int reviewRoundDelta
    ) {
        assertEquals(source, transition.sourceStatus());
        assertEquals(target, transition.targetStatus());
        assertEquals(source.level(), transition.sourceLevel());
        assertEquals(target.level(), transition.targetLevel());
        assertEquals(reviewRoundDelta, transition.reviewRoundDelta());
        assertEquals(source != target, transition.changesStatus());
        assertTrue(transition.reviewRoundDelta() >= 0);
    }
}
