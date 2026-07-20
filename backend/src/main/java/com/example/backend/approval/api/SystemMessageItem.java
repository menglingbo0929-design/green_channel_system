package com.example.backend.approval.api;

import com.example.backend.approval.persistence.type.MessageBusinessType;
import com.example.backend.approval.persistence.type.MessageType;

import java.time.LocalDateTime;

public record SystemMessageItem(
        Long messageId,
        MessageType messageType,
        MessageBusinessType businessType,
        Long businessId,
        String title,
        String content,
        boolean read,
        LocalDateTime createTime
) {
}
