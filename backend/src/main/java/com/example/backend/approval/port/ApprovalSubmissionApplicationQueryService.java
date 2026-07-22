package com.example.backend.approval.port;

import com.example.backend.application.domain.BatchType;
import com.example.backend.application.dto.ApplicationStateSnapshot;

import java.util.List;

/**
 * Member-two read boundary for batch submission.  The application module owns
 * filtering, deleted-record exclusion, and physical application-table access.
 */
public interface ApprovalSubmissionApplicationQueryService {

    List<ApplicationStateSnapshot> listByBatch(BatchType batchType, Long batchId);
}
