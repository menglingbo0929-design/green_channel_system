package com.example.backend.approval.api;

import com.example.backend.approval.domain.ApprovalAction;
import java.util.List;
import java.util.Map;

/** Approval-owned decoration around application-owned detail data. */
public record ApprovalDetailView(
        Map<String, Object> application,
        Object arrearsDetail,
        Object giftDetail,
        Object subsidyDetail,
        List<?> attachments,
        List<ApprovalRecordSnapshot> approvalRecords,
        List<String> editableFields,
        List<ApprovalAction> allowedActions,
        Integer version
) {
    public ApprovalDetailView {
        application = application == null ? Map.of() : Map.copyOf(application);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
        approvalRecords = approvalRecords == null ? List.of() : List.copyOf(approvalRecords);
        editableFields = editableFields == null ? List.of() : List.copyOf(editableFields);
        allowedActions = allowedActions == null ? List.of() : List.copyOf(allowedActions);
    }
}
