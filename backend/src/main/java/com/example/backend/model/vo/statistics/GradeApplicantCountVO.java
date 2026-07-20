package com.example.backend.model.vo.statistics;

import lombok.Data;

/** 按年级统计的最终状态申请人数量；人数始终按 studentId 去重。 */
@Data
public class GradeApplicantCountVO {
    private Long gradeId;
    private String gradeName;
    private Long applicantCount;
}
