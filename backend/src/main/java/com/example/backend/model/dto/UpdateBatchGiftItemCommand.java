package com.example.backend.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateBatchGiftItemCommand(@NotNull @Min(0) Integer stockTotal,
                                         @NotNull @Min(1) Integer perStudentLimit,
                                         @NotNull @Min(0) Integer version) { }
