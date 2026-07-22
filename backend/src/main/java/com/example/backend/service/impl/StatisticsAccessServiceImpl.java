package com.example.backend.service.impl;

import com.example.backend.model.dto.LoginUser;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.service.port.StatisticsAccessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * 统计权限校验实现 —— 成员一
 */
@Service
@RequiredArgsConstructor
public class StatisticsAccessServiceImpl implements StatisticsAccessPort {

    private final ICurrentUserProvider currentUserProvider;

    @Override
    public void checkSchoolStatisticsUser(Long userId) {
        // Port 参数继续保留以兼容既有 Service，但权限只认成员一从 JWT 解析出的当前用户。
        LoginUser currentUser = currentUserProvider.getRequiredUser();
        boolean schoolUser = currentUser.getRoles() != null
                && currentUser.getRoles().stream()
                .filter(role -> role != null && !role.isBlank())
                .map(role -> role.toUpperCase(Locale.ROOT))
                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                .anyMatch("SCHOOL"::equals);
        if (!schoolUser) {
            throw new SecurityException("仅学校管理员可访问统计数据");
        }
    }
}
