package com.example.backend.model.dto;

import com.example.backend.model.domain.ApplicationStatus;
import com.example.backend.model.domain.ApplicationType;

public record ApplicationSummary(Long id, String applicationNo, ApplicationType applicationType,
                                 ApplicationStatus status, Integer version, String applicationReason,
                                 Long batchId, String batchCode, String batchName) { }
