package com.example.backend.service;

import com.example.backend.model.dto.ApplicationDraftCommand;
import com.example.backend.model.dto.ApplicationStateSnapshot;

public interface ApplicationCreationService {
    ApplicationStateSnapshot create(Long studentId, Long operatorId, ApplicationDraftCommand command);
    ApplicationStateSnapshot createSchoolProxyApplication(Long studentId, Long operatorId, ApplicationDraftCommand command);
}
