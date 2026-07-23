package com.example.backend.service;

import com.example.backend.model.dto.ApplicationStateSnapshot;

public interface ApplicationStateQueryService { ApplicationStateSnapshot getRequiredState(Long applicationId); }
