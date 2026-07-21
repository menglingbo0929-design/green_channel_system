package com.example.backend.application.dto;

import com.example.backend.application.domain.QuotaScope;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GiftQuotaCommand(@NotNull Long batchId, @NotNull QuotaScope scope,
                               @NotNull Long targetId, @NotNull @Min(1) Integer quotaTotal) { }
