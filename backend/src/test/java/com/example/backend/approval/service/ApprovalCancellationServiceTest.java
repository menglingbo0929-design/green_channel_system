package com.example.backend.approval.service;

import com.example.backend.approval.api.ApplicationStatusResult;
import com.example.backend.approval.api.SystemMessageService;
import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApplicationType;
import com.example.backend.approval.domain.ApprovalAction;
import com.example.backend.approval.domain.ApprovalErrorCode;
import com.example.backend.approval.domain.ApprovalException;
import com.example.backend.approval.domain.ApprovalLevel;
import com.example.backend.approval.domain.ApprovalStateMachine;
import com.example.backend.approval.persistence.entity.ApprovalRecordEntity;
import com.example.backend.approval.persistence.mapper.ApprovalRecordMapper;
import com.example.backend.approval.persistence.type.ApprovalRecordLevel;
import com.example.backend.approval.persistence.type.BatchType;
import com.example.backend.approval.port.ApprovalResourceService;
import com.example.backend.approval.port.ApprovalMessageRecipientResolver;
import com.example.backend.approval.port.ApplicationStateQueryService;
import com.example.backend.approval.port.ApplicationStateSnapshot;
import com.example.backend.approval.port.ApplicationStateWriteService;
import com.example.backend.approval.port.ArrearsDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalCancellationServiceTest {

    private ApplicationStateQueryService stateQueryService;
    private ApplicationStateWriteService stateWriteService;
    private ApprovalRecordMapper approvalRecordMapper;
    private ApprovalResourceService resourceService;
    private ArrearsDocumentService arrearsDocumentService;
    private ApprovalMessageRecipientResolver messageRecipientResolver;
    private SystemMessageService systemMessageService;
    private ApprovalCancellationService service;

    @BeforeEach
    void setUp() {
        stateQueryService = mock(ApplicationStateQueryService.class);
        stateWriteService = mock(ApplicationStateWriteService.class);
        approvalRecordMapper = mock(ApprovalRecordMapper.class);
        resourceService = mock(ApprovalResourceService.class);
        arrearsDocumentService = mock(ArrearsDocumentService.class);
        messageRecipientResolver = mock(ApprovalMessageRecipientResolver.class);
        systemMessageService = mock(SystemMessageService.class);
        service = new ApprovalCancellationService(
                new ApprovalStateMachine(),
                stateQueryService,
                stateWriteService,
                approvalRecordMapper,
                resourceService,
                arrearsDocumentService,
                messageRecipientResolver,
                systemMessageService
        );
        when(approvalRecordMapper.findByRequestId(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void cancelsApprovedApplicationReleasesResourcesVoidsDocumentAndWritesAuditRecord() {
        when(stateQueryService.getRequiredState(10L)).thenReturn(snapshot(
                ApplicationStatus.APPROVED, ApprovalLevel.FINISHED, 2, 3
        ));
        when(stateWriteService.updateState(
                10L,
                ApplicationStatus.APPROVED,
                ApplicationStatus.CANCELLED,
                ApprovalLevel.FINISHED,
                3,
                99L
        )).thenReturn(snapshot(ApplicationStatus.CANCELLED, ApprovalLevel.FINISHED, 2, 4));
        when(arrearsDocumentService.hasIrreversibleOfflineProcessing(10L)).thenReturn(false);
        when(messageRecipientResolver.getStudentUserId(20L)).thenReturn(88L);

        ApplicationStatusResult result = service.cancel(10L, 3, "cancel-10", "材料重复", 99L);

        assertEquals(ApplicationStatus.CANCELLED, result.status());
        assertEquals(4, result.version());
        verify(resourceService).releaseOnCancel(10L, "cancel-10", 99L);
        verify(arrearsDocumentService).voidDocumentForCancellation(10L, "材料重复", 99L);
        verify(systemMessageService).sendApprovalCancelled(88L, 10L, "材料重复");
        ArgumentCaptor<ApprovalRecordEntity> captor = ArgumentCaptor.forClass(ApprovalRecordEntity.class);
        verify(approvalRecordMapper).insert(captor.capture());
        ApprovalRecordEntity record = captor.getValue();
        assertEquals(ApprovalAction.CANCEL, record.getAction());
        assertEquals(ApprovalRecordLevel.SCHOOL, record.getApprovalLevel());
        assertEquals(ApplicationStatus.APPROVED, record.getOldStatus());
        assertEquals(ApplicationStatus.CANCELLED, record.getNewStatus());
        assertEquals("材料重复", record.getComment());
    }

    @Test
    void rejectsCancellationAfterIrreversibleOfflineProcessing() {
        when(stateQueryService.getRequiredState(10L)).thenReturn(snapshot(
                ApplicationStatus.CONFIRM_PENDING, ApprovalLevel.CONFIRMATION, 2, 3
        ));
        when(arrearsDocumentService.hasIrreversibleOfflineProcessing(10L)).thenReturn(true);

        ApprovalException exception = assertThrows(
                ApprovalException.class,
                () -> service.cancel(10L, 3, "cancel-10", "线下撤销", 99L)
        );

        assertEquals(ApprovalErrorCode.APPROVAL_CANCEL_NOT_ALLOWED, exception.getCode());
        verify(stateWriteService, never()).updateState(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()
        );
        verify(resourceService, never()).releaseOnCancel(
                org.mockito.ArgumentMatchers.any(), anyString(), org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void rejectsCancellationForNonFinalStatus() {
        when(stateQueryService.getRequiredState(10L)).thenReturn(snapshot(
                ApplicationStatus.SCHOOL_PENDING, ApprovalLevel.SCHOOL, 2, 3
        ));

        ApprovalException exception = assertThrows(
                ApprovalException.class,
                () -> service.cancel(10L, 3, "cancel-10", "撤销", 99L)
        );

        assertEquals(ApprovalErrorCode.APPROVAL_CANCEL_NOT_ALLOWED, exception.getCode());
        verify(arrearsDocumentService, never()).hasIrreversibleOfflineProcessing(10L);
    }

    @Test
    void repeatedCancellationRequestReturnsFirstResultWithoutSideEffects() {
        ApprovalRecordEntity existing = ApprovalRecordEntity.builder()
                .applicationId(10L)
                .action(ApprovalAction.CANCEL)
                .newStatus(ApplicationStatus.CANCELLED)
                .requestId("cancel-10")
                .build();
        when(approvalRecordMapper.findByRequestId("cancel-10")).thenReturn(Optional.of(existing));

        ApplicationStatusResult result = service.cancel(10L, 3, "cancel-10", "重复请求", 99L);

        assertEquals(ApplicationStatus.CANCELLED, result.status());
        assertEquals(4, result.version());
        verify(stateQueryService, never()).getRequiredState(10L);
        verify(resourceService, never()).releaseOnCancel(
                org.mockito.ArgumentMatchers.any(), anyString(), org.mockito.ArgumentMatchers.any()
        );
    }

    private ApplicationStateSnapshot snapshot(
            ApplicationStatus status,
            ApprovalLevel level,
            int reviewRound,
            int version
    ) {
        return new ApplicationStateSnapshot(
                10L,
                20L,
                BatchType.GREEN_CHANNEL,
                30L,
                ApplicationType.GREEN_CHANNEL,
                status,
                level,
                reviewRound,
                version
        );
    }
}
