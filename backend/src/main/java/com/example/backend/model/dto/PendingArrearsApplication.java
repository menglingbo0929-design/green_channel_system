package com.example.backend.model.dto;

import java.math.BigDecimal;

public record PendingArrearsApplication(Long applicationId, String applicationNo, Integer version, Long studentId,
                                        BigDecimal appliedAmount) { }
