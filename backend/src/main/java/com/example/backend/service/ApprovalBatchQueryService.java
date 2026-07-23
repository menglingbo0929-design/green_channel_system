package com.example.backend.service;

import com.example.backend.model.domain.BatchType;

import java.time.LocalDateTime;

/**
 * Member-one read boundary for approval submission timing.  It deliberately
 * supports both batch types so member three never reads batch tables directly.
 */
public interface ApprovalBatchQueryService {

    ApprovalBatchSnapshot getRequiredBatch(BatchType batchType, Long batchId);

    record ApprovalBatchSnapshot(
            BatchType batchType,
            Long batchId,
            boolean open,
            LocalDateTime applicationDeadline,
            LocalDateTime collegeDeadline
    ) {
    }
}
