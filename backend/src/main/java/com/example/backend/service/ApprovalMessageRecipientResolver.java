package com.example.backend.service;

import com.example.backend.model.domain.ApprovalLevel;
import java.util.List;

/** Member-one integration point for resolving a student's login user ID. */
public interface ApprovalMessageRecipientResolver {
    Long getStudentUserId(Long studentId);

    /** Resolve active reviewers who currently own the student's application scope. */
    default List<Long> getReviewerUserIds(Long studentId, ApprovalLevel level) {
        return List.of();
    }
}
