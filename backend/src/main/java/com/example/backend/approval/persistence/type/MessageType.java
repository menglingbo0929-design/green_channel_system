package com.example.backend.approval.persistence.type;

public enum MessageType {
    APPROVAL_RETURNED,
    APPROVAL_REJECTED,
    APPROVAL_APPROVED,
    APPROVAL_CANCELLED,
    BATCH_DEADLINE,
    OFFLINE_PROCESSING
}
