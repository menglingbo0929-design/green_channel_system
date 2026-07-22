package com.example.backend.service.adapter;

import com.example.backend.service.port.StatisticsAccessPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 学校统计页面的访问权限实现。
 *
 * <p>这里直接复用成员一已经建立的用户、用户角色和角色表。页面只能传当前登录用户 ID，
 * 是否具有 SCHOOL 角色必须以后端数据库中的真实角色关系为准。</p>
 */
/**
 * Historical local implementation retained for migration reference only.
 * Member one now provides {@code StatisticsAccessServiceImpl} as the sole
 * injectable {@code StatisticsAccessPort}; registering both would create a
 * duplicate Spring bean and make the server fail during dependency injection.
 */
@Deprecated(forRemoval = false)
public class StatisticsAccessPortAdapter implements StatisticsAccessPort {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 查询当前用户是否拥有学校管理员角色。 */
    @Override
    public void checkSchoolStatisticsUser(Long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role ur "
                        + "JOIN sys_role r ON r.id=ur.role_id "
                        + "JOIN sys_user u ON u.id=ur.user_id AND u.deleted=0 "
                        + "WHERE ur.user_id=? AND r.role_code='SCHOOL'",
                Long.class,
                userId
        );
        if (count == null || count == 0) {
            throw new IllegalStateException("当前用户不是学校管理员");
        }
    }
}
