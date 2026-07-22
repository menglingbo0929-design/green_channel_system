package com.example.backend.approval.persistence.entity;

import com.example.backend.application.domain.BatchType;
import com.example.backend.approval.persistence.type.SubmissionLevel;
import com.example.backend.approval.persistence.type.SubmissionScopeType;
import com.example.backend.approval.persistence.type.SubmissionStatus;
import com.example.backend.approval.persistence.type.SubmissionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalSubmissionRecordEntity {
    private Long id;
    private BatchType batchType;
    private Long greenChannelBatchId;
    private Long subsidyBatchId;
    private SubmissionLevel submissionLevel;
    private SubmissionType submissionType;
    private SubmissionScopeType scopeType;
    private Long scopeId;
    private Long applicationId;
    private Integer reviewRound;
    private Long submitterId;
    private Integer submittedCount;
    private SubmissionStatus status;
    private String requestId;
    private LocalDateTime submitTime;
    private LocalDateTime createTime;

    public Long normalizedBatchId() {
        return batchType == BatchType.GREEN_CHANNEL ? greenChannelBatchId : subsidyBatchId;
    }
}
