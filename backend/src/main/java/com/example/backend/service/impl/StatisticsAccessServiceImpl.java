package com.example.backend.service.impl;

import com.example.backend.mapper.UserRoleMapper;
import com.example.backend.service.port.StatisticsAccessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 统计权限校验实现 —— 成员一
 */
@Service
@RequiredArgsConstructor
public class StatisticsAccessServiceImpl implements StatisticsAccessPort {

    private final UserRoleMapper userRoleMapper;

    @Override
    public void checkSchoolStatisticsUser(Long userId) {
        List<String> roles = userRoleMapper.selectRoleCodesByUserId(userId);
        if (roles == null || !roles.contains("SCHOOL")) {
            throw new SecurityException("仅学校管理员可访问统计数据");
        }
    }
}
