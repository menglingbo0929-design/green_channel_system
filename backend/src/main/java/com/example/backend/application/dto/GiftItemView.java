package com.example.backend.application.dto;

import java.math.BigDecimal;

public record GiftItemView(
        Long id, String name, boolean enabled, String imageUrl, String itemType,
        String itemSize, String description, BigDecimal unitPrice,
        String genderRestriction, boolean required
) { }
