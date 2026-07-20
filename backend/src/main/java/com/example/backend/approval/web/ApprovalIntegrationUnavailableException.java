package com.example.backend.approval.web;

public class ApprovalIntegrationUnavailableException extends RuntimeException {
    public ApprovalIntegrationUnavailableException(String capability) {
        super(capability + " 暂未接入，请在成员一/成员二相关 Service 合入后重试");
    }
}
