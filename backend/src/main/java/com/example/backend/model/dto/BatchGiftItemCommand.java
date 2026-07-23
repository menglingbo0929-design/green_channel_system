package com.example.backend.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BatchGiftItemCommand(@NotNull Long batchId, @NotNull Long giftItemId,
                                   @NotNull @Min(0) Integer stockTotal,
                                   @NotNull @Min(1) Integer perStudentLimit) { }
