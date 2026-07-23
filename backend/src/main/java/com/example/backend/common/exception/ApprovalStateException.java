package com.example.backend.common.exception;

public final class ApprovalStateException extends ApprovalException {

    public ApprovalStateException(ApprovalErrorCode code, String message) {
        super(code, message);
    }
}
