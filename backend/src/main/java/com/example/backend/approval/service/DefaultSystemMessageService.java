package com.example.backend.approval.service;

import com.example.backend.approval.api.SystemMessageItem;
import com.example.backend.approval.api.SystemMessagePage;
import com.example.backend.approval.api.SystemMessageService;
import com.example.backend.approval.domain.ApprovalErrorCode;
import com.example.backend.approval.domain.ApprovalException;
import com.example.backend.approval.persistence.entity.MessageReadRecordEntity;
import com.example.backend.approval.persistence.entity.SystemMessageEntity;
import com.example.backend.approval.persistence.mapper.MessageReadRecordMapper;
import com.example.backend.approval.persistence.mapper.SystemMessageMapper;
import com.example.backend.approval.persistence.type.MessageBusinessType;
import com.example.backend.approval.persistence.type.MessageType;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

public class DefaultSystemMessageService implements SystemMessageService {

    private static final int MAX_PAGE_SIZE = 100;

    private final SystemMessageMapper systemMessageMapper;
    private final MessageReadRecordMapper messageReadRecordMapper;
    private final Clock clock;

    public DefaultSystemMessageService(
            SystemMessageMapper systemMessageMapper,
            MessageReadRecordMapper messageReadRecordMapper,
            Clock clock
    ) {
        this.systemMessageMapper = systemMessageMapper;
        this.messageReadRecordMapper = messageReadRecordMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void sendApprovalReturned(Long receiverUserId, Long applicationId, String reason) {
        send(
                receiverUserId,
                applicationId,
                MessageType.APPROVAL_RETURNED,
                "申请已退回",
                requireText(reason, "reason")
        );
    }

    @Override
    @Transactional
    public void sendApprovalRejected(Long receiverUserId, Long applicationId, String reason) {
        send(
                receiverUserId,
                applicationId,
                MessageType.APPROVAL_REJECTED,
                "申请未通过",
                requireText(reason, "reason")
        );
    }

    @Override
    @Transactional
    public void sendApprovalApproved(Long receiverUserId, Long applicationId, String notice) {
        send(
                receiverUserId,
                applicationId,
                MessageType.APPROVAL_APPROVED,
                "申请审核通过",
                requireText(notice, "notice")
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SystemMessagePage listMessages(
            Long userId,
            int page,
            int size,
            Boolean read
    ) {
        requirePositive(userId, "userId");
        if (page < 1) {
            throw new IllegalArgumentException("page must be at least 1");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        int offset = Math.multiplyExact(page - 1, size);
        var records = systemMessageMapper.listByReceiverAndRead(userId, read, offset, size)
                .stream()
                .map(entity -> new SystemMessageItem(
                        entity.getId(),
                        entity.getMessageType(),
                        entity.getBusinessType(),
                        entity.getBusinessId(),
                        entity.getTitle(),
                        entity.getContent(),
                        Boolean.TRUE.equals(entity.getRead()),
                        entity.getCreateTime()
                ))
                .toList();
        long total = systemMessageMapper.countByReceiverAndRead(userId, read);
        return new SystemMessagePage(records, total, page, size);
    }

    @Override
    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        requirePositive(messageId, "messageId");
        requirePositive(userId, "userId");
        SystemMessageEntity message = systemMessageMapper.findById(messageId)
                .orElseThrow(() -> new ApprovalException(
                        ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE,
                        "消息不存在或无权访问"
                ));
        if (!userId.equals(message.getReceiverUserId())) {
            throw new ApprovalException(
                    ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE,
                    "只能标记发送给当前用户的消息"
            );
        }
        if (messageReadRecordMapper.findByMessageAndUser(messageId, userId).isPresent()) {
            return;
        }
        messageReadRecordMapper.insertIgnore(MessageReadRecordEntity.builder()
                .messageId(messageId)
                .userId(userId)
                .readTime(LocalDateTime.now(clock))
                .build());
    }

    private void send(
            Long receiverUserId,
            Long applicationId,
            MessageType messageType,
            String title,
            String content
    ) {
        requirePositive(receiverUserId, "receiverUserId");
        requirePositive(applicationId, "applicationId");
        systemMessageMapper.insert(SystemMessageEntity.builder()
                .receiverUserId(receiverUserId)
                .messageType(messageType)
                .businessType(MessageBusinessType.APPLICATION)
                .businessId(applicationId)
                .title(title)
                .content(content)
                .build());
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.strip();
    }

    private void requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
