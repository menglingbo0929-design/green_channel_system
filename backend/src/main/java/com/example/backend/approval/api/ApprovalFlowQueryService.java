package com.example.backend.approval.api;

public interface ApprovalFlowQueryService {

    ApprovalFlowSnapshot getFlow(Long applicationId);
}
