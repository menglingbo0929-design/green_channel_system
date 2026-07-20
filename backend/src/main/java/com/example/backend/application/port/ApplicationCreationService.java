package com.example.backend.application.port;

import com.example.backend.application.dto.ApplicationDraftCommand;
import com.example.backend.application.dto.ApplicationStateSnapshot;

public interface ApplicationCreationService {
    ApplicationStateSnapshot create(Long studentId, Long operatorId, ApplicationDraftCommand command);
    ApplicationStateSnapshot createSchoolProxyApplication(Long studentId, Long operatorId, ApplicationDraftCommand command);
}
