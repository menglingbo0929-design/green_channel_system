package com.example.backend.model.dto;

import com.example.backend.model.dto.ApprovalCollegeCount;
import com.example.backend.model.dto.ApprovalFunnelCount;
import com.example.backend.model.dto.ApprovalLevelCount;
import java.util.List;

/** Aggregates owned by the application module, calculated only over an approved scope. */
public record ApprovalDashboardData(
        List<ApprovalLevelCount> pendingByLevel,
        List<ApprovalCollegeCount> pendingByCollege,
        List<ApprovalFunnelCount> flowFunnel
) {
}
