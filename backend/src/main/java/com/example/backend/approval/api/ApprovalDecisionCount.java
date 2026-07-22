package com.example.backend.approval.api;

import com.example.backend.approval.domain.ApprovalAction;

public record ApprovalDecisionCount(ApprovalAction action, long count) {
}
