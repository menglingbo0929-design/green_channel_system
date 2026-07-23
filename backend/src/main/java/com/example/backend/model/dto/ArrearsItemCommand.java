package com.example.backend.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 欠费项目明细。reasonCode 为空时按数据库默认值 OTHER 保存，以兼容已有学生端请求；
 * 非空值由 ApplicationService 按固定枚举校验。
 */
public record ArrearsItemCommand(
        @NotNull Long feeItemId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal declaredAmount,
        @Size(max = 32) String arrearsReasonCode
) { }
