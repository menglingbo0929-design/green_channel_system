package com.example.backend.approval.persistence.entity;

import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApprovalAction;
import com.example.backend.approval.persistence.type.ApprovalRecordLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRecordEntity {
    private Long id;
    private Long applicationId;
    private Integer reviewRound;
    private ApprovalRecordLevel approvalLevel;
    private Long approverId;
    private String approverNameSnapshot;
    private ApprovalAction action;
    private String comment;
    private ApplicationStatus oldStatus;
    private ApplicationStatus newStatus;
    private String modifiedFields;
    private String requestId;
    private LocalDateTime createTime;
}
