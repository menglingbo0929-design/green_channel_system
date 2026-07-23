package com.example.backend.service.adapter;

import com.example.backend.service.port.ArrearsVoucherAccessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 欠费单据访问校验和确认人名称查询适配器。
 *
 * <p>校级角色、学生本人关系和用户名均读取成员一已经建立的用户、角色和学生表，
 * 页面不再自行传入可信角色或确认人名称。</p>
 */
@Component
@RequiredArgsConstructor
public class ArrearsVoucherAccessPortAdapter implements ArrearsVoucherAccessPort {
    private final JdbcTemplate jdbcTemplate;

    /** 确认当前操作用户具有 SCHOOL 角色。 */
    @Override
    public void checkSchoolUser(Long userId) {
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

    /** 确认学生账号只能读取本人申请生成的欠费单据。 */
    @Override
    public void checkStudentOwnsApplication(Long userId, Long applicationId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM application a "
                        + "JOIN student s ON s.id=a.student_id AND s.deleted=0 "
                        + "WHERE a.id=? AND a.deleted=0 AND s.user_id=?",
                Long.class,
                applicationId,
                userId
        );
        if (count == null || count == 0) {
            throw new IllegalStateException("当前学生不能查看该欠费单据");
        }
    }

    /** 批量读取确认人登录名；旧测试数据没有对应用户时显示稳定的用户编号。 */
    @Override
    public Map<Long, String> findUserNamesByIds(Collection<Long> userIds) {
        Map<Long, String> result = new LinkedHashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        String placeholders = userIds.stream().map(id -> "?")
                .collect(Collectors.joining(","));
        jdbcTemplate.query(
                "SELECT id,login_name FROM sys_user WHERE deleted=0 AND id IN ("
                        + placeholders + ")",
                (RowCallbackHandler) rs -> result.put(
                        rs.getLong("id"), rs.getString("login_name")),
                userIds.toArray()
        );
        userIds.forEach(id -> result.putIfAbsent(id, "用户" + id));
        return result;
    }
}
