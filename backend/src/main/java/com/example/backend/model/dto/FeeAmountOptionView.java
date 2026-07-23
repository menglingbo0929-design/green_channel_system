package com.example.backend.model.dto;

import java.math.BigDecimal;

/** 欠费金额档位读取模型。 */
public record FeeAmountOptionView(Long id, Long feeItemId, BigDecimal amount, boolean enabled) {
}
