package com.example.backend.approval.port;

import com.example.backend.approval.api.ApprovalCollegeCount;
import com.example.backend.approval.api.ApprovalFunnelCount;
import com.example.backend.approval.api.ApprovalLevelCount;
import java.util.List;

/** Aggregates owned by the application module, calculated only over an approved scope. */
public record ApprovalDashboardData(
        List<ApprovalLevelCount> pendingByLevel,
        List<ApprovalCollegeCount> pendingByCollege,
        List<ApprovalFunnelCount> flowFunnel
) {
}
