package com.example.backend.application.dto;

import com.example.backend.application.domain.QuotaScope;
import java.math.BigDecimal;

public record SubsidyQuotaView(Long id, Long batchId, QuotaScope scope, Long targetId, String targetName,
                               BigDecimal quotaAmount, BigDecimal reservedAmount, BigDecimal usedAmount,
                               Integer version) { }
