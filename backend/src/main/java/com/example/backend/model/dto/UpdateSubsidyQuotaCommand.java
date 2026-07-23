package com.example.backend.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateSubsidyQuotaCommand(@NotNull @DecimalMin("0.01") BigDecimal quotaAmount,
                                        @NotNull @Min(0) Integer version) { }
