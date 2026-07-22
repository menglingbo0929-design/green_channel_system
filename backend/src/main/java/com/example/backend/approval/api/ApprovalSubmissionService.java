package com.example.backend.approval.api;

import com.example.backend.application.domain.BatchType;
import com.example.backend.approval.port.LoginUser;

/** Member-three batch submission boundary used by the REST controller. */
public interface ApprovalSubmissionService {

    ApprovalSubmissionStatus getStatus(LoginUser user, BatchType batchType, Long batchId);

    ApprovalSubmissionResult submitInitial(LoginUser user, BatchType batchType, Long batchId, String requestId);

    ApprovalSubmissionResult submitReturnResubmit(LoginUser user, Long applicationId, Integer expectedVersion, String requestId);
}
