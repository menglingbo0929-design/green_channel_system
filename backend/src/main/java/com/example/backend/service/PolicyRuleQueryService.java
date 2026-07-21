package com.example.backend.service;

import com.example.backend.model.domain.PolicyRule;

import java.util.List;

/**
 * 政策规则查询服务 —— 成员一向成员二提供
 *
 * 成员二在申请页面展示政策提示时调用，帮助学生在填写申请前了解规则。
 */
public interface PolicyRuleQueryService {

    /**
     * 查询某批次适用的启用政策规则，按排序号升序
     *
     * @param batchType 批次类型（GREEN_CHANNEL / LIVING_SUBSIDY / TRAVEL_SUBSIDY / ALL）
     */
    List<PolicyRule> listEnabledRules(String batchType);
}
