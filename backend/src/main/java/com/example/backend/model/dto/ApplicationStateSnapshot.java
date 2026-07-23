package com.example.backend.model.dto;

import com.example.backend.model.domain.*;

public record ApplicationStateSnapshot(Long applicationId, Long studentId, BatchType batchType, Long batchId,
                                       ApplicationType applicationType, ApplicationStatus status,
                                       ApprovalLevel currentLevel, Integer reviewRound, Integer version) { }
