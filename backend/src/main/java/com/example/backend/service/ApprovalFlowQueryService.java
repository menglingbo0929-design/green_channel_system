package com.example.backend.service;

import com.example.backend.model.dto.ApprovalFlowSnapshot;

public interface ApprovalFlowQueryService {

    ApprovalFlowSnapshot getFlow(Long applicationId);
}
