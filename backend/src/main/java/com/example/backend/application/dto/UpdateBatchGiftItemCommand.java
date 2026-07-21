package com.example.backend.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateBatchGiftItemCommand(@NotNull @Min(0) Integer stockTotal,
                                         @NotNull @Min(1) Integer perStudentLimit,
                                         @NotNull @Min(0) Integer version) { }
