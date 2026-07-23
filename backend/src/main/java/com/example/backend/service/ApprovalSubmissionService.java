package com.example.backend.service;

import com.example.backend.model.domain.BatchType;
import com.example.backend.model.dto.ApprovalSubmissionResult;
import com.example.backend.model.dto.ApprovalSubmissionStatus;
import com.example.backend.model.dto.LoginUser;

/** Member-three batch submission boundary used by the REST controller. */
public interface ApprovalSubmissionService {

    ApprovalSubmissionStatus getStatus(LoginUser user, BatchType batchType, Long batchId);

    ApprovalSubmissionResult submitInitial(LoginUser user, BatchType batchType, Long batchId, String requestId);

    ApprovalSubmissionResult submitReturnResubmit(LoginUser user, Long applicationId, Integer expectedVersion, String requestId);
}
