package com.example.backend.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ArrearsItemCommand(@NotNull Long feeItemId, @NotNull @DecimalMin(value = "0.01") BigDecimal declaredAmount) { }
