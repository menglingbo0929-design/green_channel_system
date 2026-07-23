package com.example.backend.model.dto;

import java.util.List;
import java.util.Map;

/** Application-owned details; the approval module only adds flow records and permissions. */
public record ApprovalApplicationDetail(
        Map<String, Object> application,
        Object arrearsDetail,
        Object giftDetail,
        Object subsidyDetail,
        List<?> attachments,
        Integer version
) {
}
