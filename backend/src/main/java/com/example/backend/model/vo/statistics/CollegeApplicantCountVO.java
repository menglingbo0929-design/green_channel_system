package com.example.backend.model.vo.statistics;

import lombok.Data;

/** 按学院统计的最终状态申请人数量；人数始终按 studentId 去重。 */
@Data
public class CollegeApplicantCountVO {
    private Long collegeId;
    private String collegeName;
    private Long applicantCount;
}
