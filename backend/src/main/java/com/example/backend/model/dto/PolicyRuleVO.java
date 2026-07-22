package com.example.backend.model.dto;

/** 申请引导文案的稳定 API 返回结构。 */
public record PolicyRuleVO(
        Long id,
        String ruleCode,
        String ruleName,
        String ruleContent,
        String batchType,
        Integer sortOrder,
        Boolean enabled
) {
}
