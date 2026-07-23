package com.example.backend.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GiftApplicationItemCommand(@NotNull Long batchGiftItemId, @NotNull @Min(1) Integer quantity) { }
