package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.PolicyRuleMapper;
import com.example.backend.model.domain.PolicyRule;
import com.example.backend.model.dto.PolicyRuleRequest;
import com.example.backend.model.dto.PolicyRuleVO;
import com.example.backend.service.PolicyRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyRuleServiceImpl implements PolicyRuleService {

    private final PolicyRuleMapper policyRuleMapper;

    @Override
    public List<PolicyRuleVO> listEnabledRules(String batchType) {
        String normalized = normalizeBatchType(batchType);
        return policyRuleMapper.selectList(new LambdaQueryWrapper<PolicyRule>()
                        .eq(PolicyRule::getEnabled, 1)
                        .eq(PolicyRule::getDeleted, 0)
                        .and(wrapper -> wrapper.eq(PolicyRule::getBatchType, normalized)
                                .or().eq(PolicyRule::getBatchType, "ALL"))
                        .orderByAsc(PolicyRule::getSortOrder)
                        .orderByAsc(PolicyRule::getId))
                .stream().map(this::toVO).toList();
    }

    @Override
    public List<PolicyRuleVO> listAllRules() {
        return policyRuleMapper.selectList(new LambdaQueryWrapper<PolicyRule>()
                        .eq(PolicyRule::getDeleted, 0)
                        .orderByAsc(PolicyRule::getSortOrder)
                        .orderByAsc(PolicyRule::getId))
                .stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public PolicyRuleVO create(PolicyRuleRequest request) {
        ensureCodeAvailable(request.ruleCode().trim(), null);
        PolicyRule rule = new PolicyRule();
        apply(rule, request);
        policyRuleMapper.insert(rule);
        return toVO(rule);
    }

    @Override
    @Transactional
    public PolicyRuleVO update(Long id, PolicyRuleRequest request) {
        PolicyRule rule = required(id);
        ensureCodeAvailable(request.ruleCode().trim(), id);
        apply(rule, request);
        policyRuleMapper.updateById(rule);
        return toVO(required(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        required(id);
        policyRuleMapper.deleteById(id);
    }

    private void apply(PolicyRule rule, PolicyRuleRequest request) {
        rule.setRuleCode(request.ruleCode().trim());
        rule.setRuleName(request.ruleName().trim());
        rule.setRuleContent(request.ruleContent().trim());
        rule.setBatchType(normalizeBatchType(request.batchType()));
        rule.setSortOrder(request.sortOrder());
        rule.setEnabled(Boolean.TRUE.equals(request.enabled()) ? 1 : 0);
    }

    private PolicyRule required(Long id) {
        PolicyRule rule = policyRuleMapper.selectById(id);
        if (rule == null || !Long.valueOf(0).equals(rule.getDeleted())) {
            throw new IllegalArgumentException("政策规则不存在");
        }
        return rule;
    }

    private void ensureCodeAvailable(String code, Long currentId) {
        PolicyRule existing = policyRuleMapper.selectOne(new LambdaQueryWrapper<PolicyRule>()
                .eq(PolicyRule::getRuleCode, code)
                .eq(PolicyRule::getDeleted, 0));
        if (existing != null && !existing.getId().equals(currentId)) {
            throw new IllegalArgumentException("政策规则编码已存在");
        }
    }

    private String normalizeBatchType(String batchType) {
        String normalized = batchType == null ? "GREEN_CHANNEL" : batchType.trim().toUpperCase();
        if (!List.of("ALL", "GREEN_CHANNEL", "LIVING_SUBSIDY", "TRAVEL_SUBSIDY").contains(normalized)) {
            throw new IllegalArgumentException("政策适用类型无效");
        }
        return normalized;
    }

    private PolicyRuleVO toVO(PolicyRule rule) {
        return new PolicyRuleVO(rule.getId(), rule.getRuleCode(), rule.getRuleName(),
                rule.getRuleContent(), rule.getBatchType(), rule.getSortOrder(),
                Integer.valueOf(1).equals(rule.getEnabled()));
    }
}
