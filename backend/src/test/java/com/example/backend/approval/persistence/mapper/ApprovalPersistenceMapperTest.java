package com.example.backend.approval.persistence.mapper;

import com.example.backend.application.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApprovalAction;
import com.example.backend.approval.persistence.entity.ApprovalRecordEntity;
import com.example.backend.approval.persistence.entity.ApprovalSubmissionRecordEntity;
import com.example.backend.approval.persistence.entity.MessageReadRecordEntity;
import com.example.backend.approval.persistence.entity.SystemMessageEntity;
import com.example.backend.approval.persistence.type.ApprovalRecordLevel;
import com.example.backend.application.domain.BatchType;
import com.example.backend.approval.persistence.type.MessageBusinessType;
import com.example.backend.approval.persistence.type.MessageType;
import com.example.backend.approval.persistence.type.SubmissionLevel;
import com.example.backend.approval.persistence.type.SubmissionScopeType;
import com.example.backend.approval.persistence.type.SubmissionStatus;
import com.example.backend.approval.persistence.type.SubmissionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ApprovalPersistenceMapperTest {

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private ApprovalSubmissionRecordMapper approvalSubmissionRecordMapper;

    @Autowired
    private SystemMessageMapper systemMessageMapper;

    @Autowired
    private MessageReadRecordMapper messageReadRecordMapper;

    @Test
    void insertsAndQueriesImmutableApprovalRecord() {
        ApprovalRecordEntity record = ApprovalRecordEntity.builder()
                .applicationId(101L)
                .reviewRound(0)
                .approvalLevel(ApprovalRecordLevel.COUNSELOR)
                .approverId(2001L)
                .approverNameSnapshot("测试辅导员")
                .action(ApprovalAction.APPROVE)
                .comment("材料齐全")
                .oldStatus(ApplicationStatus.COUNSELOR_PENDING)
                .newStatus(ApplicationStatus.COUNSELOR_PENDING)
                .requestId("approval-request-001")
                .build();

        assertEquals(1, approvalRecordMapper.insert(record));
        assertNotNull(record.getId());

        ApprovalRecordEntity stored = approvalRecordMapper.findByRequestId(record.getRequestId()).orElseThrow();
        assertEquals(ApprovalRecordLevel.COUNSELOR, stored.getApprovalLevel());
        assertEquals(ApprovalAction.APPROVE, stored.getAction());
        assertEquals(ApplicationStatus.COUNSELOR_PENDING, stored.getNewStatus());
        assertNotNull(stored.getCreateTime());

        assertEquals(1, approvalRecordMapper.listByApplicationId(101L).size());
        assertTrue(approvalRecordMapper.findLatestDecision(
                101L,
                0,
                ApprovalRecordLevel.COUNSELOR
        ).isPresent());
    }

    @Test
    void mapsNormalizedGreenChannelSubmissionScope() {
        LocalDateTime submitTime = LocalDateTime.of(2026, 7, 20, 10, 0);
        ApprovalSubmissionRecordEntity submission = ApprovalSubmissionRecordEntity.builder()
                .batchType(BatchType.GREEN_CHANNEL)
                .greenChannelBatchId(301L)
                .submissionLevel(SubmissionLevel.COUNSELOR)
                .submissionType(SubmissionType.INITIAL_BATCH)
                .scopeType(SubmissionScopeType.COUNSELOR)
                .scopeId(2001L)
                .applicationId(0L)
                .reviewRound(0)
                .submitterId(2001L)
                .submittedCount(12)
                .status(SubmissionStatus.SUBMITTED)
                .requestId("submission-request-001")
                .submitTime(submitTime)
                .build();

        assertEquals(1, approvalSubmissionRecordMapper.insert(submission));

        ApprovalSubmissionRecordEntity stored = approvalSubmissionRecordMapper
                .findByRequestId(submission.getRequestId())
                .orElseThrow();
        assertEquals(BatchType.GREEN_CHANNEL, stored.getBatchType());
        assertEquals(301L, stored.normalizedBatchId());
        assertEquals(submitTime, stored.getSubmitTime());

        List<ApprovalSubmissionRecordEntity> records = approvalSubmissionRecordMapper.listByScope(
                BatchType.GREEN_CHANNEL,
                301L,
                SubmissionLevel.COUNSELOR,
                SubmissionScopeType.COUNSELOR,
                2001L
        );
        assertEquals(1, records.size());
    }

    @Test
    void insertsMessagesAndMarksReadIdempotently() {
        SystemMessageEntity message = SystemMessageEntity.builder()
                .receiverUserId(9001L)
                .messageType(MessageType.APPROVAL_RETURNED)
                .businessType(MessageBusinessType.APPLICATION)
                .businessId(101L)
                .title("申请已退回")
                .content("请修改材料后重新提交")
                .createBy(3001L)
                .build();

        assertEquals(1, systemMessageMapper.insert(message));
        assertNotNull(message.getId());
        assertEquals(1, systemMessageMapper.countByReceiver(9001L));
        assertEquals(1, systemMessageMapper.countUnreadByReceiver(9001L));
        assertEquals(message.getId(), systemMessageMapper.listByReceiver(9001L, 0, 10).getFirst().getId());
        assertEquals(1, systemMessageMapper.listByReceiverAndRead(9001L, false, 0, 10).size());
        assertEquals(1, systemMessageMapper.countByReceiverAndRead(9001L, false));

        MessageReadRecordEntity readRecord = MessageReadRecordEntity.builder()
                .messageId(message.getId())
                .userId(9001L)
                .readTime(LocalDateTime.of(2026, 7, 20, 10, 30))
                .build();

        assertEquals(1, messageReadRecordMapper.insertIgnore(readRecord));
        assertEquals(0, messageReadRecordMapper.insertIgnore(readRecord));
        assertTrue(messageReadRecordMapper.findByMessageAndUser(message.getId(), 9001L).isPresent());
        assertEquals(0, systemMessageMapper.countUnreadByReceiver(9001L));
        assertTrue(systemMessageMapper.listByReceiverAndRead(9001L, true, 0, 10).getFirst().getRead());
        assertEquals(1, systemMessageMapper.countByReceiverAndRead(9001L, true));
    }
}
