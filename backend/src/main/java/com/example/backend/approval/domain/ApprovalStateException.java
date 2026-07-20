package com.example.backend.approval.domain;

public final class ApprovalStateException extends ApprovalException {

    public ApprovalStateException(ApprovalErrorCode code, String message) {
        super(code, message);
    }
}
