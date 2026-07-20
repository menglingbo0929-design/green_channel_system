package com.example.backend.application.dto;

import com.example.backend.application.domain.ApplicationStatus;
import com.example.backend.application.domain.ApplicationType;

public record ApplicationSummary(Long id, String applicationNo, ApplicationType applicationType,
                                 ApplicationStatus status, Integer version, String applicationReason) { }
