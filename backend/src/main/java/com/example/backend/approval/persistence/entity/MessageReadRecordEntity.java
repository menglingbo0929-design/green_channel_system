package com.example.backend.approval.persistence.entity;

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
