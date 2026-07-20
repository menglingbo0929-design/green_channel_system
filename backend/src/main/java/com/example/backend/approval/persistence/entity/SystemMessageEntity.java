package com.example.backend.approval.persistence.entity;

import com.example.backend.approval.persistence.type.MessageBusinessType;
import com.example.backend.approval.persistence.type.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMessageEntity {
    private Long id;
    private Long receiverUserId;
    private MessageType messageType;
    private MessageBusinessType businessType;
    private Long businessId;
    private String title;
    private String content;
    private Long createBy;
    private LocalDateTime createTime;
    private Boolean read;
}
