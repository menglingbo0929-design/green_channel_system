package com.example.backend.approval.service;

import com.example.backend.approval.domain.ApprovalErrorCode;
import com.example.backend.approval.domain.ApprovalException;
import com.example.backend.approval.persistence.entity.MessageReadRecordEntity;
import com.example.backend.approval.persistence.entity.SystemMessageEntity;
import com.example.backend.approval.persistence.mapper.MessageReadRecordMapper;
import com.example.backend.approval.persistence.mapper.SystemMessageMapper;
import com.example.backend.approval.persistence.type.MessageBusinessType;
import com.example.backend.approval.persistence.type.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultSystemMessageServiceTest {

    private SystemMessageMapper systemMessageMapper;
    private MessageReadRecordMapper messageReadRecordMapper;
    private DefaultSystemMessageService service;

    @BeforeEach
    void setUp() {
        systemMessageMapper = mock(SystemMessageMapper.class);
        messageReadRecordMapper = mock(MessageReadRecordMapper.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-20T02:30:00Z"),
                ZoneId.of("Asia/Shanghai")
        );
        service = new DefaultSystemMessageService(
                systemMessageMapper,
                messageReadRecordMapper,
                clock
        );
    }

    @Test
    void sendsReturnedMessageForApplication() {
        service.sendApprovalReturned(7L, 10L, "请补充证明材料");

        ArgumentCaptor<SystemMessageEntity> captor = ArgumentCaptor.forClass(SystemMessageEntity.class);
        verify(systemMessageMapper).insert(captor.capture());
        SystemMessageEntity message = captor.getValue();
        assertEquals(7L, message.getReceiverUserId());
        assertEquals(MessageType.APPROVAL_RETURNED, message.getMessageType());
        assertEquals(MessageBusinessType.APPLICATION, message.getBusinessType());
        assertEquals(10L, message.getBusinessId());
        assertEquals("请补充证明材料", message.getContent());
    }

    @Test
    void listsReadFilteredMessagesWithContractPagination() {
        SystemMessageEntity entity = SystemMessageEntity.builder()
                .id(1L)
                .receiverUserId(7L)
                .messageType(MessageType.APPROVAL_APPROVED)
                .businessType(MessageBusinessType.APPLICATION)
                .businessId(10L)
                .title("申请审核通过")
                .content("审核流程已完成")
                .read(true)
                .createTime(LocalDateTime.of(2026, 7, 20, 10, 0))
                .build();
        when(systemMessageMapper.listByReceiverAndRead(7L, true, 10, 10))
                .thenReturn(List.of(entity));
        when(systemMessageMapper.countByReceiverAndRead(7L, true)).thenReturn(1L);

        var page = service.listMessages(7L, 2, 10, true);

        assertEquals(1L, page.total());
        assertEquals(2, page.page());
        assertEquals(1, page.records().size());
        assertEquals(true, page.records().getFirst().read());
    }

    @Test
    void markAsReadIsIdempotent() {
        when(systemMessageMapper.findById(1L)).thenReturn(Optional.of(message(1L, 7L)));
        when(messageReadRecordMapper.findByMessageAndUser(1L, 7L))
                .thenReturn(Optional.of(MessageReadRecordEntity.builder().id(3L).build()));

        service.markAsRead(1L, 7L);

        verify(messageReadRecordMapper, never()).insertIgnore(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cannotReadAnotherUsersMessage() {
        when(systemMessageMapper.findById(1L)).thenReturn(Optional.of(message(1L, 8L)));

        ApprovalException exception = assertThrows(
                ApprovalException.class,
                () -> service.markAsRead(1L, 7L)
        );

        assertEquals(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, exception.getCode());
    }

    @Test
    void writesReadTimestampFromInjectedClock() {
        when(systemMessageMapper.findById(1L)).thenReturn(Optional.of(message(1L, 7L)));
        when(messageReadRecordMapper.findByMessageAndUser(1L, 7L)).thenReturn(Optional.empty());

        service.markAsRead(1L, 7L);

        ArgumentCaptor<MessageReadRecordEntity> captor = ArgumentCaptor.forClass(MessageReadRecordEntity.class);
        verify(messageReadRecordMapper).insertIgnore(captor.capture());
        assertEquals(LocalDateTime.of(2026, 7, 20, 10, 30), captor.getValue().getReadTime());
    }

    private SystemMessageEntity message(Long id, Long receiverUserId) {
        return SystemMessageEntity.builder()
                .id(id)
                .receiverUserId(receiverUserId)
                .messageType(MessageType.APPROVAL_RETURNED)
                .businessType(MessageBusinessType.APPLICATION)
                .businessId(10L)
                .title("申请已退回")
                .content("请修改")
                .build();
    }
}
