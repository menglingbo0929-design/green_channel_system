package com.example.backend.model.vo.statistics;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 6.1.5 统计大盘的完整响应对象。
 *
 * <p>所有集合默认空数组、金额默认 0.00，由成员二的单次聚合查询填充真实值；成员四不会
 * 用确认表或内存样本补造任何维度。</p>
 */
@Data
public class ApplicationStatisticsVO {
    /** APPROVED 与 COMPLETED 中按 studentId 去重后的申请总人数。 */
    private Long finalApplicantCount = 0L;
    /** COMPLETED 中按 studentId 去重后的实报人数。 */
    private Long completedStudentCount = 0L;
    /** 按 feeItemId 去重后的欠费项目申请人数；未筛选项目时为所有欠费项目去重人数。 */
    private Long feeItemApplicantCount = 0L;
    /** 已完成申请关联的有效确认金额合计。 */
    private BigDecimal confirmedArrearsAmount = BigDecimal.ZERO;
    private List<CollegeApplicantCountVO> collegeApplicantCounts = new ArrayList<>();
    private List<GradeApplicantCountVO> gradeApplicantCounts = new ArrayList<>();
    private List<ArrearsReasonStatisticsVO> arrearsReasonStatistics = new ArrayList<>();
    private List<GiftItemApplicationCountVO> giftItemApplicationCounts = new ArrayList<>();
    private List<BatchHistoryStatisticsVO> batchHistoryStatistics = new ArrayList<>();
}
