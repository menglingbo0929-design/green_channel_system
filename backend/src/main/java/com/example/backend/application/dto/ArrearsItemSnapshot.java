package com.example.backend.application.dto;

import java.math.BigDecimal;

public record ArrearsItemSnapshot(Long applicationId, Long feeItemId, String feeItemName, BigDecimal declaredAmount) { }
