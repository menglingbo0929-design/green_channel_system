package com.example.backend.application.dto;

import com.example.backend.application.domain.*;

public record ApplicationStateSnapshot(Long applicationId, Long studentId, BatchType batchType, Long batchId,
                                       ApplicationType applicationType, ApplicationStatus status,
                                       ApprovalLevel currentLevel, Integer reviewRound, Integer version) { }
