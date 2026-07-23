package com.example.backend.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReadRecordEntity {
    private Long id;
    private Long messageId;
    private Long userId;
    private LocalDateTime readTime;
    private LocalDateTime createTime;
}
