package com.example.backend.approval.domain;

public class ApprovalException extends RuntimeException {

    private final ApprovalErrorCode code;

    public ApprovalException(ApprovalErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ApprovalErrorCode getCode() {
        return code;
    }
}
