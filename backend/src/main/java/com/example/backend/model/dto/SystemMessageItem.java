package com.example.backend.model.dto;

import com.example.backend.model.domain.MessageBusinessType;
import com.example.backend.model.domain.MessageType;

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
