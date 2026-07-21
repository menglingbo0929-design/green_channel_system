package com.example.backend.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateSubsidyRequest(@NotNull Integer version, @NotNull @DecimalMin("0.01") BigDecimal expectedAmount) { }
