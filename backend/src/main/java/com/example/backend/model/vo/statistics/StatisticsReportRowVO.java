package com.example.backend.model.vo.statistics;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成员二向成员四返回的一行真实统计明细。
 *
 * <p>该对象只保存已经脱敏、允许进入学校报表的字段，不包含身份证号、手机号、
 * 附件地址等敏感信息。欠费项目和礼包有多项时，由成员二按固定顺序拼成中文名称。</p>
 */
@Data
public class StatisticsReportRowVO {
    private Long applicationId;
    private String applicationNo;
    private String studentNo;
    private String studentName;
    private String collegeName;
    private String majorName;
    private String gradeName;
    private String className;
    private String applicationType;
    private String batchType;
    private Long batchId;
    private String batchName;
    private String applicationStatus;
    private String applicationSource;
    private String arrearsItemNames;
    private String arrearsReasonName;
    private BigDecimal declaredAmount;
    private BigDecimal confirmedAmount;
    private String giftItemNames;
    private BigDecimal subsidyAmount;
    private LocalDateTime applicationTime;
    private LocalDateTime completionTime;
}
