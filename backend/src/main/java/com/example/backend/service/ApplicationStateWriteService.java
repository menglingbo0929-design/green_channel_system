package com.example.backend.service;

import com.example.backend.model.domain.*;
import com.example.backend.model.dto.ApplicationStateSnapshot;

public interface ApplicationStateWriteService {
    ApplicationStateSnapshot updateState(Long applicationId, ApplicationStatus expectedStatus, ApplicationStatus targetStatus,
                                         ApprovalLevel targetLevel, Integer expectedVersion, Long operatorId);
    ApplicationStateSnapshot incrementReviewRoundAndUpdateState(Long applicationId, ApplicationStatus expectedStatus,
                                                                 ApplicationStatus targetStatus, ApprovalLevel targetLevel,
                                                                 Integer expectedVersion, Long operatorId);
}
