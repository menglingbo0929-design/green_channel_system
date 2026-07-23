package com.example.backend.model.dto;

import com.example.backend.model.domain.ApprovalAction;

public class ApprovalDecisionCountProjection {
    private ApprovalAction action;
    private long count;

    public ApprovalAction getAction() { return action; }
    public void setAction(ApprovalAction action) { this.action = action; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
