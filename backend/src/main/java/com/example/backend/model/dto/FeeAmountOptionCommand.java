package com.example.backend.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 欠费金额档位写入参数。 */
public record FeeAmountOptionCommand(
        @NotNull(message = "欠费项目不能为空") Long feeItemId,
        @NotNull(message = "金额不能为空") @DecimalMin(value = "0.01", message = "金额必须大于 0") BigDecimal amount,
        @NotNull(message = "启用状态不能为空") Boolean enabled) {
}
