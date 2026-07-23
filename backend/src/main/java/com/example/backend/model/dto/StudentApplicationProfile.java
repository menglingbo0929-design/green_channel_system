package com.example.backend.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 学生申请画像 —— 成员一提供给成员二，用于创建申请时写入快照
 *
 * 包含学生基本信息、组织归属和资助贷款情况。
 * 成员二调用 StudentProfileQueryService.getRequiredProfile(studentId) 获取。
 */
@Data
@Builder
public class StudentApplicationProfile {

    // ── 学生基本信息 ──
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String phone;

    // ── 组织归属 ──
    private Long collegeId;
    private String collegeName;
    private Long majorId;
    private String majorName;
    private Long gradeId;
    private String gradeName;
    private Long classId;
    private String className;

    // ── 资助贷款情况 ──
    /** 生源地贷款：0=无 1=有 */
    private Integer originLoan;
    /** 拟申请校园地贷款：0=否 1=是 */
    private Integer campusLoan;
    /** 资助认定等级 */
    private String subsidyLevel;
    /** 家庭困难等级 */
    private String difficultyLevel;
    /** 信息是否完善：0=否 1=是 */
    private Integer infoComplete;
    /** 辅导员用户ID */
    private Long counselorId;
}
