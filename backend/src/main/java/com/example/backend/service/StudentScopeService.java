package com.example.backend.service;

/**
 * 学生数据范围校验服务 —— 成员一向成员二、三提供
 *
 * 供审核流程、申请创建等场景校验操作者是否有权处理某个学生。
 */
public interface StudentScopeService {

    /**
     * 判断当前登录用户是否就是该学生本人
     *
     * @param userId    当前登录用户 ID
     * @param studentId 学生 ID
     */
    boolean isStudentSelf(Long userId, Long studentId);

    /**
     * 判断辅导员是否负责该学生
     *
     * 优先查 counselor_student 关联表，其次查 student.counselor_id。
     *
     * @param counselorUserId 辅导员用户 ID
     * @param studentId       学生 ID
     */
    boolean isCounselorResponsibleFor(Long counselorUserId, Long studentId);

    /**
     * 判断该学生是否在指定学院范围内
     *
     * @param studentId 学生 ID
     * @param collegeId 学院 ID
     */
    boolean isStudentInCollege(Long studentId, Long collegeId);
}
