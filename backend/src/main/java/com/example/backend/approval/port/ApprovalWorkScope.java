package com.example.backend.approval.port;

import com.example.backend.approval.persistence.type.ApprovalRecordLevel;

/** Identity and approved role scope supplied to member-two read adapters. */
public record ApprovalWorkScope(
        Long userId,
        UserRole role,
        Long collegeId,
        ApprovalRecordLevel reviewLevel
) {
}
