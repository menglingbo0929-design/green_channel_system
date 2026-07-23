package com.example.backend.model.dto;

import com.example.backend.model.domain.ApprovalAction;

public record ApprovalDecisionCount(ApprovalAction action, long count) {
}
