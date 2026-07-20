package com.example.backend.model.dto.schoolproxy;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

/** 一项由学校代填的欠费项目；金额仍由成员二按项目规则校验。 */
@Data
public class SchoolProxyArrearsItemDTO {
    @NotNull private Long feeItemId;
    @NotNull @Positive private BigDecimal declaredAmount;
}
