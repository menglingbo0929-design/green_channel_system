package com.example.backend.service.port;

/**
 * 成员一实现的学校统计权限边界；真实实现必须从当前登录上下文校验 SCHOOL 角色。
 *
 * <p>当前唯一可注入实现为成员一的 StatisticsAccessServiceImpl，权限从 JWT 登录上下文读取
 * SCHOOL 角色，不再接受前端传入的 X-User-Id 或角色作为可信依据。</p>
 */
public interface StatisticsAccessPort {
    /** 无学校统计权限时抛出业务异常，不能通过前端传入的组织 ID 放行。 */
    void checkSchoolStatisticsUser(Long userId);
}
