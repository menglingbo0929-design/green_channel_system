package com.example.backend.model.dto;

import com.example.backend.model.domain.ApprovalRecordLevel;
import com.example.backend.model.domain.UserRole;

/** Identity and approved role scope supplied to member-two read adapters. */
public record ApprovalWorkScope(
        Long userId,
        UserRole role,
        Long collegeId,
        ApprovalRecordLevel reviewLevel
) {
}
