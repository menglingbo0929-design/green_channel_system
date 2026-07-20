package com.example.backend.service;

/**
 * 学生数据范围校验服务 —— 成员一向成员二、三提供
 */
public interface StudentScopeService {

    /** 当前用户是否就是该学生本人 */
    // boolean isStudentSelf(Long userId, Long studentId);

    /** 当前辅导员是否负责该学生 */
    // boolean isCounselorResponsibleFor(Long counselorUserId, Long studentId);

    /** 该学生是否在当前学院范围内 */
    // boolean isStudentInCollege(Long studentId, Long collegeId);
}
