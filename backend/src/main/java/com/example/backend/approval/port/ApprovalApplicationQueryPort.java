package com.example.backend.approval.port;

import com.example.backend.approval.api.ApprovalListQuery;
import com.example.backend.approval.api.ApprovalPage;
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
