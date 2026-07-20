package com.example.backend.application.dto;

import java.math.BigDecimal;

/** 欠费金额档位读取模型。 */
public record FeeAmountOptionView(Long id, Long feeItemId, BigDecimal amount, boolean enabled) {
}
