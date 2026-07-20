package com.example.backend.service.port;

/**
 * 成员一实现的学校统计权限边界；真实实现必须从当前登录上下文校验 SCHOOL 角色。
 *
 * <p>TODO(成员一)：使用 CurrentUserProvider 实现 SCHOOL 角色和学校数据范围校验，
 * 替换本地调试使用的 X-User-Id。</p>
 */
public interface StatisticsAccessPort {
    /** 无学校统计权限时抛出业务异常，不能通过前端传入的组织 ID 放行。 */
    void checkSchoolStatisticsUser(Long userId);
}
