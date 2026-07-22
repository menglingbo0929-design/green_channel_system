package com.example.backend.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批次快照 —— 成员一提供给成员二/三/四
 *
 * 包含批次基本信息和适用年级列表，用于申请校验和审核时间判断。
 */
@Data
@Builder
public class BatchSnapshot {

    private Long batchId;
    private String batchCode;
    private String batchName;
    private String batchType;
    private String applicationType;
    private String academicYear;

    /** 学生申请开始时间 */
    private LocalDateTime startTime;
    /** 学生申请截止时间 */
    private LocalDateTime endTime;
    /** 学院上报学校截止时间（仅绿通批次有值） */
    private LocalDateTime collegeDeadline;
    /** 批次状态：DRAFT / OPEN / CLOSED */
    private String status;
    private Integer enabled;

    /** 适用年级 ID 列表 */
    private List<Long> eligibleGradeIds;
    private List<String> fundingSourceCodes;
}
