package com.example.backend.service;

import com.example.backend.model.dto.LoginUser;
import com.example.backend.model.dto.ApprovalDashboard;
import com.example.backend.model.dto.ApprovalDetailView;
import com.example.backend.model.dto.ApprovalListItem;
import com.example.backend.model.dto.ApprovalListQuery;
import com.example.backend.model.dto.ApprovalPage;

/** Member-three query boundary for the role-specific approval workbench. */
public interface ApprovalWorkbenchQueryService {

    ApprovalPage<ApprovalListItem> pagePending(LoginUser user, ApprovalListQuery query);

    ApprovalPage<ApprovalListItem> pageProcessed(LoginUser user, ApprovalListQuery query);

    ApprovalDetailView getDetail(LoginUser user, Long applicationId);

    ApprovalDashboard getDashboard(LoginUser user, ApprovalListQuery query);
}
