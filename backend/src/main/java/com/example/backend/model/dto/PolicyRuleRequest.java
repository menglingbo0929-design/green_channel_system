package com.example.backend.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 学校维护的申请引导文案。 */
public record PolicyRuleRequest(
        @NotBlank @Size(max = 32) String ruleCode,
        @NotBlank @Size(max = 64) String ruleName,
        @NotBlank @Size(max = 2000) String ruleContent,
        @NotBlank
        @Pattern(regexp = "ALL|GREEN_CHANNEL|LIVING_SUBSIDY|TRAVEL_SUBSIDY")
        String batchType,
        @NotNull @Min(0) Integer sortOrder,
        @NotNull Boolean enabled
) {
}
