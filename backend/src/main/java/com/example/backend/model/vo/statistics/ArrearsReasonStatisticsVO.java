package com.example.backend.model.vo.statistics;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 一个欠费原因的双口径统计。
 *
 * <p>applicantCount 用于“按人数”展示，confirmedAmount 用于“按金额”展示；金额只累计
 * 已完成申请关联的有效最终确认金额。</p>
 */
@Data
public class ArrearsReasonStatisticsVO {
    private String arrearsReasonCode;
    private String arrearsReasonName;
    private Long applicantCount;
    private BigDecimal confirmedAmount;
}
