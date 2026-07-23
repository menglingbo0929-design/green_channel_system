package com.example.backend.model.dto;

import com.example.backend.model.domain.QuotaScope;
import java.math.BigDecimal;

public record SubsidyQuotaView(Long id, Long batchId, QuotaScope scope, Long targetId, String targetName,
                               BigDecimal quotaAmount, BigDecimal reservedAmount, BigDecimal usedAmount,
                               Integer version) { }
