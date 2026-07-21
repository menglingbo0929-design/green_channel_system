package com.example.backend.application.dto;

import com.example.backend.application.domain.QuotaScope;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SubsidyQuotaCommand(@NotNull Long batchId, @NotNull QuotaScope scope,
                                  @NotNull Long targetId,
                                  @NotNull @DecimalMin("0.01") BigDecimal quotaAmount) { }
