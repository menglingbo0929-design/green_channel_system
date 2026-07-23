package com.example.backend.service;

import com.example.backend.model.dto.ApprovalListQuery;
import com.example.backend.model.dto.ApprovalPage;
import com.example.backend.model.dto.ApprovalApplicationDetail;
import com.example.backend.model.dto.ApprovalApplicationSnapshot;
import com.example.backend.model.dto.ApprovalDashboardData;
import com.example.backend.model.dto.ApprovalWorkScope;
import java.util.List;

/**
 * Member-two integration point. Implementations remain the sole reader of
 * application, student snapshots, details, and attachment tables.
 */
public interface ApprovalApplicationQueryPort {

    ApprovalPage<ApprovalApplicationSnapshot> pagePending(ApprovalWorkScope scope, ApprovalListQuery query);

    ApprovalPage<ApprovalApplicationSnapshot> pageByApplicationIds(
            ApprovalWorkScope scope,
            ApprovalListQuery query,
            List<Long> applicationIds
    );

    ApprovalApplicationDetail getRequiredApprovalDetail(Long applicationId);

    ApprovalDashboardData getDashboard(ApprovalWorkScope scope, ApprovalListQuery query);

    List<Long> listScopedApplicationIds(ApprovalWorkScope scope, ApprovalListQuery query);
}
