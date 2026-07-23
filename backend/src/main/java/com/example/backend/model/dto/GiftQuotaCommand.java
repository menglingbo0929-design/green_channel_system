package com.example.backend.model.dto;

import com.example.backend.model.domain.QuotaScope;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GiftQuotaCommand(@NotNull Long batchId, @NotNull QuotaScope scope,
                               @NotNull Long targetId, @NotNull @Min(1) Integer quotaTotal) { }
