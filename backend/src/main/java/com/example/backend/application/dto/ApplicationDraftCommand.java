package com.example.backend.application.dto;

import com.example.backend.application.domain.ApplicationType;
import com.example.backend.application.domain.BatchType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApplicationDraftCommand(@NotNull ApplicationType applicationType, @NotNull BatchType batchType,
                                      @NotNull Long batchId, @NotBlank String requestId, String applicationReason) { }
