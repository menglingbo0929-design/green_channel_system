package com.example.backend.service.port;

import java.util.Collection;
import java.util.Map;

/**
 * 成员一提供的权限和用户展示名能力；Controller 不信任任何前端角色或学生参数。
 *
 * <p>正式适配器从用户、角色、学生和申请表校验 SCHOOL 权限与 STUDENT 申请归属，
 * 并批量解析用户展示名。</p>
 */
public interface ArrearsVoucherAccessPort {
    void checkSchoolUser(Long userId);
    void checkStudentOwnsApplication(Long userId, Long applicationId);
    Map<Long, String> findUserNamesByIds(Collection<Long> userIds);
}
