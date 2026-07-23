package com.example.backend.service;

import com.example.backend.model.domain.BatchType;
import com.example.backend.model.dto.ApplicationStateSnapshot;

import java.util.List;

/**
 * Member-two read boundary for batch submission.  The application module owns
 * filtering, deleted-record exclusion, and physical application-table access.
 */
public interface ApprovalSubmissionApplicationQueryService {

    List<ApplicationStateSnapshot> listByBatch(BatchType batchType, Long batchId);
}
