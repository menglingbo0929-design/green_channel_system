package com.example.backend.application.domain;

import java.time.LocalDateTime;

public class Application {
    private Long id; private String applicationNo; private Long studentId; private ApplicationType applicationType;
    private ApplicationSource source; private BatchType batchType; private Long greenChannelBatchId; private Long subsidyBatchId;
    private ApplicationStatus status; private ApprovalLevel currentLevel; private Integer reviewRound; private Integer version;
    private String applicationReason; private String supplementReason; private LocalDateTime supplementedAt; private LocalDateTime submitTime; private Long createBy; private Long updateBy;
    public Long getId() { return id; } public void setId(Long v) { id = v; }
    public String getApplicationNo() { return applicationNo; } public void setApplicationNo(String v) { applicationNo = v; }
    public Long getStudentId() { return studentId; } public void setStudentId(Long v) { studentId = v; }
    public ApplicationType getApplicationType() { return applicationType; } public void setApplicationType(ApplicationType v) { applicationType = v; }
    public ApplicationSource getSource() { return source; } public void setSource(ApplicationSource v) { source = v; }
    public BatchType getBatchType() { return batchType; } public void setBatchType(BatchType v) { batchType = v; }
    public Long getGreenChannelBatchId() { return greenChannelBatchId; } public void setGreenChannelBatchId(Long v) { greenChannelBatchId = v; }
    public Long getSubsidyBatchId() { return subsidyBatchId; } public void setSubsidyBatchId(Long v) { subsidyBatchId = v; }
    public ApplicationStatus getStatus() { return status; } public void setStatus(ApplicationStatus v) { status = v; }
    public ApprovalLevel getCurrentLevel() { return currentLevel; } public void setCurrentLevel(ApprovalLevel v) { currentLevel = v; }
    public Integer getReviewRound() { return reviewRound; } public void setReviewRound(Integer v) { reviewRound = v; }
    public Integer getVersion() { return version; } public void setVersion(Integer v) { version = v; }
    public String getApplicationReason() { return applicationReason; } public void setApplicationReason(String v) { applicationReason = v; }
    public String getSupplementReason() { return supplementReason; } public void setSupplementReason(String v) { supplementReason = v; }
    public LocalDateTime getSupplementedAt() { return supplementedAt; } public void setSupplementedAt(LocalDateTime v) { supplementedAt = v; }
    public LocalDateTime getSubmitTime() { return submitTime; } public void setSubmitTime(LocalDateTime v) { submitTime = v; }
    public Long getCreateBy() { return createBy; } public void setCreateBy(Long v) { createBy = v; }
    public Long getUpdateBy() { return updateBy; } public void setUpdateBy(Long v) { updateBy = v; }
}
