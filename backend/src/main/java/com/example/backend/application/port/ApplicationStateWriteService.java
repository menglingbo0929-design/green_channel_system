package com.example.backend.application.port;

import com.example.backend.application.domain.*;
import com.example.backend.application.dto.ApplicationStateSnapshot;

public interface ApplicationStateWriteService {
    ApplicationStateSnapshot updateState(Long applicationId, ApplicationStatus expectedStatus, ApplicationStatus targetStatus,
                                         ApprovalLevel targetLevel, Integer expectedVersion, Long operatorId);
    ApplicationStateSnapshot incrementReviewRoundAndUpdateState(Long applicationId, ApplicationStatus expectedStatus,
                                                                 ApplicationStatus targetStatus, ApprovalLevel targetLevel,
                                                                 Integer expectedVersion, Long operatorId);
}
