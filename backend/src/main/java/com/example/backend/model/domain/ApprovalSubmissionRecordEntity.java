package com.example.backend.model.domain;

import com.example.backend.model.domain.BatchType;
import com.example.backend.model.domain.SubmissionLevel;
import com.example.backend.model.domain.SubmissionScopeType;
import com.example.backend.model.domain.SubmissionStatus;
import com.example.backend.model.domain.SubmissionType;
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
