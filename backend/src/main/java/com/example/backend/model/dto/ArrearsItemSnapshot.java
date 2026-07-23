package com.example.backend.model.dto;

import java.math.BigDecimal;

public record ArrearsItemSnapshot(Long applicationId, Long feeItemId, String feeItemName, BigDecimal declaredAmount,
                                  String arrearsReasonCode) { }
