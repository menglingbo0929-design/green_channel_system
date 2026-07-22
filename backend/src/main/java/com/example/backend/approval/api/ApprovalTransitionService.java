package com.example.backend.approval.api;

public interface ApprovalTransitionService {

    /**
     * Records a formal submission and advances a DRAFT application to
     * COUNSELOR_PENDING. Student applications and SCHOOL_PROXY applications
     * share this single transition; the application owner must invoke it from
     * the same transaction after its attachment and resource preconditions pass.
     */
    ApplicationStatusResult submitInitial(
            Long applicationId,
            Integer expectedVersion,
            String requestId,
            Long operatorId
    );

    ApplicationStatusResult resubmitReturned(
            Long applicationId,
            Integer expectedVersion,
            String requestId,
            Long operatorId
    );

    ApplicationStatusResult completeSupplementReview(
            Long applicationId,
            boolean containsArrears,
            Integer expectedVersion,
            String requestId,
            Long operatorId
    );
}
