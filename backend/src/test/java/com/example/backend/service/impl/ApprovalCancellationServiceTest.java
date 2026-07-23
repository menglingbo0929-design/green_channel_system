package com.example.backend.service.impl;

import com.example.backend.model.dto.ApplicationStatusResult;
import com.example.backend.service.SystemMessageService;
import com.example.backend.model.domain.ApplicationStatus;
import com.example.backend.model.domain.ApplicationType;
import com.example.backend.model.domain.ApprovalAction;
import com.example.backend.common.exception.ApprovalErrorCode;
import com.example.backend.common.exception.ApprovalException;
import com.example.backend.model.domain.ApprovalLevel;
import com.example.backend.model.domain.ApprovalStateMachine;
import com.example.backend.model.domain.ApprovalRecordEntity;
import com.example.backend.mapper.ApprovalRecordMapper;
import com.example.backend.model.domain.ApprovalRecordLevel;
import com.example.backend.model.domain.BatchType;
import com.example.backend.service.ApprovalResourceService;
import com.example.backend.service.ApprovalMessageRecipientResolver;
import com.example.backend.service.ApplicationStateQueryService;
import com.example.backend.model.dto.ApplicationStateSnapshot;
import com.example.backend.service.ApplicationStateWriteService;
import com.example.backend.service.ArrearsDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
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

    @Test
    void rejectsCancellationWhenApplicationVersionChanged() {
        when(stateQueryService.getRequiredState(10L)).thenReturn(snapshot(
                ApplicationStatus.APPROVED, ApprovalLevel.FINISHED, 2, 4
        ));

        ApprovalException exception = assertThrows(
                ApprovalException.class,
                () -> service.cancel(10L, 3, "cancel-version-conflict", "版本冲突", 99L)
        );

        assertEquals(ApprovalErrorCode.APPROVAL_VERSION_CONFLICT, exception.getCode());
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
    void resourceReleaseFailureStopsDocumentAuditAndMessageSideEffects() {
        when(stateQueryService.getRequiredState(10L)).thenReturn(snapshot(
                ApplicationStatus.COMPLETED, ApprovalLevel.FINISHED, 2, 3
        ));
        when(stateWriteService.updateState(
                10L,
                ApplicationStatus.COMPLETED,
                ApplicationStatus.CANCELLED,
                ApprovalLevel.FINISHED,
                3,
                99L
        )).thenReturn(snapshot(ApplicationStatus.CANCELLED, ApprovalLevel.FINISHED, 2, 4));
        when(arrearsDocumentService.hasIrreversibleOfflineProcessing(10L)).thenReturn(false);
        doThrow(new IllegalStateException("resource release failed"))
                .when(resourceService).releaseOnCancel(10L, "cancel-resource-failure", 99L);

        assertThrows(
                IllegalStateException.class,
                () -> service.cancel(10L, 3, "cancel-resource-failure", "资源释放失败", 99L)
        );

        verify(arrearsDocumentService, never()).voidDocumentForCancellation(
                org.mockito.ArgumentMatchers.any(), anyString(), org.mockito.ArgumentMatchers.any()
        );
        verify(approvalRecordMapper, never()).insert(org.mockito.ArgumentMatchers.any());
        verify(systemMessageService, never()).sendApprovalCancelled(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), anyString()
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
