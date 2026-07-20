package com.example.backend.approval.api;

public interface SystemMessageService {

    void sendApprovalReturned(Long receiverUserId, Long applicationId, String reason);

    void sendApprovalRejected(Long receiverUserId, Long applicationId, String reason);

    void sendApprovalApproved(Long receiverUserId, Long applicationId, String notice);

    SystemMessagePage listMessages(Long userId, int page, int size, Boolean read);

    void markAsRead(Long messageId, Long userId);
}
