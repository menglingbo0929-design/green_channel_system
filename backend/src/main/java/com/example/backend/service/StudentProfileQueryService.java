package com.example.backend.service;

import com.example.backend.model.dto.StudentApplicationProfile;

/**
 * 学生信息查询服务 —— 成员一向成员二提供
 *
 * 成员二在创建申请时调用此服务获取学生信息和贷款情况，
 * 写入申请快照字段，确保不从前端接受不可信的学生数据。
 */
public interface StudentProfileQueryService {

    /**
     * 根据学生 ID 获取学生基本信息和贷款情况
     *
     * @param studentId 学生 ID（student 表主键）
     * @return 学生申请画像，含组织归属和资助信息
     * @throws IllegalArgumentException 学生不存在或已停用
     */
    StudentApplicationProfile getRequiredProfile(Long studentId);
}
