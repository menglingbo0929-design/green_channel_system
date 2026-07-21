package com.example.backend.approval.api;

import com.example.backend.approval.port.LoginUser;

/** Member-three query boundary for the role-specific approval workbench. */
public interface ApprovalWorkbenchQueryService {

    ApprovalPage<ApprovalListItem> pagePending(LoginUser user, ApprovalListQuery query);

    ApprovalPage<ApprovalListItem> pageProcessed(LoginUser user, ApprovalListQuery query);

    ApprovalDetailView getDetail(LoginUser user, Long applicationId);

    ApprovalDashboard getDashboard(LoginUser user, ApprovalListQuery query);
}
