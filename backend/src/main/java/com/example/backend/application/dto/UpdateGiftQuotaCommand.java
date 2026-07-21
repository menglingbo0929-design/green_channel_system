package com.example.backend.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateGiftQuotaCommand(@NotNull @Min(1) Integer quotaTotal,
                                     @NotNull @Min(0) Integer version) { }
