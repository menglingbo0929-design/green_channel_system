package com.example.backend.model.dto;

import com.example.backend.model.domain.QuotaScope;

public record GiftQuotaView(Long id, Long batchId, QuotaScope scope, Long targetId, String targetName,
                            Integer quotaTotal, Integer reservedCount, Integer usedCount, Integer version) { }
