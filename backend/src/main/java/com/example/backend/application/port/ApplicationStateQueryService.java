package com.example.backend.application.port;

import com.example.backend.application.dto.ApplicationStateSnapshot;

public interface ApplicationStateQueryService { ApplicationStateSnapshot getRequiredState(Long applicationId); }
