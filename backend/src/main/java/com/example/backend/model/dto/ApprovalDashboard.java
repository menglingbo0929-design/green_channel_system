package com.example.backend.model.dto;

import java.util.List;

public record ApprovalDashboard(
        List<ApprovalLevelCount> pendingByLevel,
        List<ApprovalDecisionCount> decisionDistribution,
        List<ApprovalCollegeCount> pendingByCollege,
        List<ApprovalFunnelCount> flowFunnel
) {
    public ApprovalDashboard {
        pendingByLevel = pendingByLevel == null ? List.of() : List.copyOf(pendingByLevel);
        decisionDistribution = decisionDistribution == null ? List.of() : List.copyOf(decisionDistribution);
        pendingByCollege = pendingByCollege == null ? List.of() : List.copyOf(pendingByCollege);
        flowFunnel = flowFunnel == null ? List.of() : List.copyOf(flowFunnel);
    }
}
