package com.example.backend.service.port;

import java.util.Collection;
import java.util.Map;

/**
 * 成员一提供的权限和用户展示名能力；Controller 不信任任何前端角色或学生参数。
 *
 * <p>TODO(成员一)：接入真实登录上下文，实现 SCHOOL 权限、STUDENT 申请归属和
 * 用户姓名批量查询；演示前须完成该实现。</p>
 */
public interface ArrearsVoucherAccessPort {
    void checkSchoolUser(Long userId);
    void checkStudentOwnsApplication(Long userId, Long applicationId);
    Map<Long, String> findUserNamesByIds(Collection<Long> userIds);
}
