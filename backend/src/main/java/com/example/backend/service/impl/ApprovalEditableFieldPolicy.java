package com.example.backend.service.impl;

import com.example.backend.model.domain.ApplicationStatus;
import com.example.backend.model.domain.ApplicationType;
import com.example.backend.model.dto.ApplicationStateSnapshot;
import com.example.backend.model.dto.LoginUser;
import com.example.backend.model.domain.UserRole;
import java.util.ArrayList;
import java.util.List;

/**
 * Single source of truth for reviewer-editable application fields.
 * Student identity, organization, batch, source and primary keys are never exposed.
 */
final class ApprovalEditableFieldPolicy {

    static final String APPLICATION_REASON = "applicationReason";
    static final String ARREARS_ITEMS = "arrearsItems";
    static final String GIFT_ITEMS = "giftItems";
    static final String EXPECTED_SUBSIDY_AMOUNT = "expectedSubsidyAmount";
    static final String FINAL_SUBSIDY_AMOUNT = "finalSubsidyAmount";

    private ApprovalEditableFieldPolicy() {
    }

    static List<String> editableFields(LoginUser user, ApplicationStateSnapshot state) {
        if (!isActiveReviewer(user, state)) return List.of();

        List<String> fields = new ArrayList<>();
        fields.add(APPLICATION_REASON);
        if (state.applicationType() == ApplicationType.GREEN_CHANNEL) {
            fields.add(ARREARS_ITEMS);
            fields.add(GIFT_ITEMS);
        } else {
            fields.add(EXPECTED_SUBSIDY_AMOUNT);
            if (user.role() == UserRole.COUNSELOR) fields.add(FINAL_SUBSIDY_AMOUNT);
        }
        return List.copyOf(fields);
    }

    static List<String> applicationWriteFields(LoginUser user, ApplicationStateSnapshot state) {
        return editableFields(user, state).stream()
                .filter(field -> !FINAL_SUBSIDY_AMOUNT.equals(field))
                .toList();
    }

    private static boolean isActiveReviewer(LoginUser user, ApplicationStateSnapshot state) {
        if (user == null || user.userId() == null || state == null) return false;
        return switch (user.role()) {
            case COUNSELOR -> state.status() == ApplicationStatus.COUNSELOR_PENDING;
            case COLLEGE -> state.status() == ApplicationStatus.COLLEGE_PENDING;
            case SCHOOL -> state.status() == ApplicationStatus.SCHOOL_PENDING;
            case STUDENT -> false;
        };
    }
}
