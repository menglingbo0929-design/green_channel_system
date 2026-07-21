package com.example.backend.service;

import java.util.List;

/**
 * 辅导员-学生关联管理服务 —— 成员一维护
 *
 * 辅导员只能操作自己负责的学生，这个关联关系由本服务管理。
 * 其他模块通过 StudentScopeService 查询，通过本服务修改。
 */
public interface CounselorStudentService {

    /** 为辅导员分配学生 */
    void assign(Long counselorUserId, Long studentId);

    /** 为辅导员批量分配学生 */
    void assignBatch(Long counselorUserId, List<Long> studentIds);

    /** 解除辅导员与学生的关联 */
    void remove(Long counselorUserId, Long studentId);

    /** 查询某辅导员负责的所有学生 ID */
    List<Long> listStudentIdsByCounselor(Long counselorUserId);

    /** 查询某学生的所有辅导员 ID */
    List<Long> listCounselorIdsByStudent(Long studentId);
}
