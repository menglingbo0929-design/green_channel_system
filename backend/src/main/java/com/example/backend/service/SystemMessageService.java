package com.example.backend.service;

import com.example.backend.model.dto.SystemMessagePage;

public interface SystemMessageService {

    void sendApplicationSubmitted(Long receiverUserId, Long applicationId, String applicationNo);

    void sendApprovalReturned(Long receiverUserId, Long applicationId, String reason);

    void sendApprovalRejected(Long receiverUserId, Long applicationId, String reason);

    void sendApprovalApproved(Long receiverUserId, Long applicationId, String notice);

    void sendApprovalProgress(Long receiverUserId, Long applicationId, String notice);

    void sendApprovalTask(Long receiverUserId, Long applicationId, String notice);

    void sendApprovalCancelled(Long receiverUserId, Long applicationId, String reason);

    SystemMessagePage listMessages(Long userId, int page, int size, Boolean read);

    void markAsRead(Long messageId, Long userId);
}
