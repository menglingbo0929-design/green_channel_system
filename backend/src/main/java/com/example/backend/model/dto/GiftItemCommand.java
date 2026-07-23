package com.example.backend.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record GiftItemCommand(
        @NotBlank @Size(max = 100) String name,
        @NotNull Boolean enabled,
        @Size(max = 500) String imageUrl,
        @Size(max = 64) String itemType,
        @Size(max = 64) String itemSize,
        @Size(max = 1000) String description,
        @DecimalMin(value = "0.00") BigDecimal unitPrice,
        @Size(max = 16) String genderRestriction,
        @NotNull Boolean required
) { }
