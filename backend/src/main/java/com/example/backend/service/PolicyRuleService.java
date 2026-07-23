package com.example.backend.service;

import com.example.backend.model.dto.PolicyRuleRequest;
import com.example.backend.model.dto.PolicyRuleVO;

import java.util.List;

/** 政策引导文案的唯一服务边界，同时承担读取和学校端维护。 */
public interface PolicyRuleService {
    List<PolicyRuleVO> listEnabledRules(String batchType);
    List<PolicyRuleVO> listAllRules();
    PolicyRuleVO create(PolicyRuleRequest request);
    PolicyRuleVO update(Long id, PolicyRuleRequest request);
    void delete(Long id);
}
