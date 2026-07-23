package com.example.backend.model.dto;

import com.example.backend.model.domain.ApplicationType;
import com.example.backend.model.domain.BatchType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApplicationDraftCommand(@NotNull ApplicationType applicationType, @NotNull BatchType batchType,
                                      @NotNull Long batchId, @NotBlank String requestId, String applicationReason) { }
