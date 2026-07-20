package com.example.backend.model.vo.voucher;

import lombok.Data;
import java.math.BigDecimal;

/** 单据中展示的一项欠费项目和申报金额。 */
@Data
public class ArrearsVoucherItemVO {
    private String feeItemName;
    private BigDecimal declaredAmount;
}
