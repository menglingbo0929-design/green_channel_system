package com.example.backend.model.vo.statistics;

import lombok.Data;

import java.math.BigDecimal;

/** 历史批次汇总行；用于 6.1.5 的历史批次数据展示。 */
@Data
public class BatchHistoryStatisticsVO {
    private String batchType;
    private Long batchId;
    private String batchName;
    private Long applicantCount;
    private Long completedStudentCount;
    private BigDecimal confirmedArrearsAmount;
}
