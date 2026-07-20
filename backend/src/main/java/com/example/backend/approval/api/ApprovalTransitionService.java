package com.example.backend.approval.api;

public interface ApprovalTransitionService {

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
