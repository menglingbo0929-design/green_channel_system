package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.PolicyRuleMapper;
import com.example.backend.model.domain.PolicyRule;
import com.example.backend.service.PolicyRuleQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 政策规则查询服务实现
 */
@Service
@RequiredArgsConstructor
public class PolicyRuleQueryServiceImpl implements PolicyRuleQueryService {

    private final PolicyRuleMapper policyRuleMapper;

    @Override
    public List<PolicyRule> listEnabledRules(String batchType) {
        return policyRuleMapper.selectList(
                new LambdaQueryWrapper<PolicyRule>()
                        .eq(PolicyRule::getEnabled, 1)
                        .eq(PolicyRule::getDeleted, 0)
                        .and(w -> w.eq(PolicyRule::getBatchType, batchType)
                                   .or().eq(PolicyRule::getBatchType, "ALL"))
                        .orderByAsc(PolicyRule::getSortOrder));
    }
}
