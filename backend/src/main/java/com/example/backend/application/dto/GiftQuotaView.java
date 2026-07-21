package com.example.backend.application.dto;

import com.example.backend.application.domain.QuotaScope;

public record GiftQuotaView(Long id, Long batchId, QuotaScope scope, Long targetId, String targetName,
                            Integer quotaTotal, Integer reservedCount, Integer usedCount, Integer version) { }
