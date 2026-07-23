package com.example.backend.model.dto;

/**
 * 学生进入绿色通道申请页前的资格判断结果。
 *
 * <p>原因码与 {@code docs/decisions/application-config.md} 6.1 保持一致；
 * 前端只展示后端给出的结果，不自行计算学生画像。</p>
 */
public record GreenChannelEligibility(
        boolean allowed,
        String reasonCode,
        String message,
        Long batchId,
        String batchName
) {
}
