package com.example.backend.approval.service;

import com.example.backend.approval.api.ApplicationStatusResult;
import com.example.backend.application.domain.ApplicationStatus;
import com.example.backend.application.domain.ApplicationType;
import com.example.backend.approval.domain.ApprovalAction;
import com.example.backend.approval.domain.ApprovalErrorCode;
import com.example.backend.approval.domain.ApprovalException;
import com.example.backend.application.domain.ApprovalLevel;
import com.example.backend.approval.domain.ApprovalStateMachine;
import com.example.backend.approval.persistence.entity.ApprovalRecordEntity;
import com.example.backend.approval.persistence.mapper.ApprovalRecordMapper;
import com.example.backend.approval.persistence.type.ApprovalRecordLevel;
import com.example.backend.application.domain.BatchType;
import com.example.backend.application.port.ApplicationStateQueryService;
import com.example.backend.application.dto.ApplicationStateSnapshot;
import com.example.backend.application.port.ApplicationStateWriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalWorkflowServiceTest {

    private ApplicationStateQueryService stateQueryService;
    private ApplicationStateWriteService stateWriteService;
    private ApprovalRecordMapper approvalRecordMapper;
    private ApprovalWorkflowService service;

    @BeforeEach
    void setUp() {
        stateQueryService = mock(ApplicationStateQueryService.class);
        stateWriteService = mock(ApplicationStateWriteService.class);
        approvalRecordMapper = mock(ApprovalRecordMapper.class);
        service = new ApprovalWorkflowService(
                new ApprovalStateMachine(),
                stateQueryService,
                stateWriteService,
                approvalRecordMapper
        );
        when(approvalRecordMapper.findByRequestId(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void submitsDraftThroughMemberTwoPortAndAppendsAuditRecord() {
        when(stateQueryService.getRequiredState(10L)).thenReturn(snapshot(
                ApplicationStatus.DRAFT,
                ApprovalLevel.STUDENT,
                0,
                3
        ));
        when(stateWriteService.updateState(
                10L,
                ApplicationStatus.DRAFT,
                ApplicationStatus.COUNSELOR_PENDING,
                ApprovalLevel.COUNSELOR,
                3,
                99L
        )).thenReturn(snapshot(
                ApplicationStatus.COUNSELOR_PENDING,
                ApprovalLevel.COUNSELOR,
                0,
                4
        ));

        ApplicationStatusResult result = service.submitInitial(10L, 3, "submit-10", 99L);

        assertEquals(ApplicationStatus.COUNSELOR_PENDING, result.status());
        assertEquals(4, result.version());
        ArgumentCaptor<ApprovalRecordEntity> recordCaptor = ArgumentCaptor.forClass(ApprovalRecordEntity.class);
        verify(approvalRecordMapper).insert(recordCaptor.capture());
        ApprovalRecordEntity record = recordCaptor.getValue();
        assertEquals(ApprovalRecordLevel.STUDENT, record.getApprovalLevel());
        assertEquals(ApprovalAction.SUBMIT, record.getAction());
        assertEquals(ApplicationStatus.DRAFT, record.getOldStatus());
        assertEquals(ApplicationStatus.COUNSELOR_PENDING, record.getNewStatus());
        assertEquals(0, record.getReviewRound());
    }

    @Test
    void resubmissionIncrementsReviewRoundUsingOwnedWritePort() {
        when(stateQueryService.getRequiredState(10L)).thenReturn(snapshot(
                ApplicationStatus.SCHOOL_RETURNED,
                ApprovalLevel.STUDENT,
                2,
                7
        ));
        when(stateWriteService.incrementReviewRoundAndUpdateState(
                10L,
                ApplicationStatus.SCHOOL_RETURNED,
                ApplicationStatus.COUNSELOR_PENDING,
                ApprovalLevel.COUNSELOR,
                7,
                99L
        )).thenReturn(snapshot(
                ApplicationStatus.COUNSELOR_PENDING,
                ApprovalLevel.COUNSELOR,
                3,
                8
        ));

        service.resubmitReturned(10L, 7, "resubmit-10", 99L);

        ArgumentCaptor<ApprovalRecordEntity> recordCaptor = ArgumentCaptor.forClass(ApprovalRecordEntity.class);
        verify(approvalRecordMapper).insert(recordCaptor.capture());
        assertEquals(3, recordCaptor.getValue().getReviewRound());
    }

    @Test
    void repeatedRequestReturnsOriginalTransitionWithoutWritingAgain() {
        ApprovalRecordEntity previous = ApprovalRecordEntity.builder()
                .applicationId(10L)
                .newStatus(ApplicationStatus.COUNSELOR_PENDING)
                .requestId("same-request")
                .build();
        when(approvalRecordMapper.findByRequestId("same-request")).thenReturn(Optional.of(previous));

        ApplicationStatusResult result = service.submitInitial(10L, 3, "same-request", 99L);

        assertEquals(ApplicationStatus.COUNSELOR_PENDING, result.status());
        assertEquals(4, result.version());
        verify(stateQueryService, never()).getRequiredState(10L);
        verify(approvalRecordMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsStaleVersionBeforeCallingStateWriter() {
        when(stateQueryService.getRequiredState(10L)).thenReturn(snapshot(
                ApplicationStatus.DRAFT,
                ApprovalLevel.STUDENT,
                0,
                4
        ));

        ApprovalException exception = assertThrows(
                ApprovalException.class,
                () -> service.submitInitial(10L, 3, "stale-request", 99L)
        );

        assertEquals(ApprovalErrorCode.APPROVAL_VERSION_CONFLICT, exception.getCode());
        verify(stateWriteService, never()).updateState(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void flowUsesChronologicalRecordsAndLatestReasons() {
        when(stateQueryService.getRequiredState(10L)).thenReturn(snapshot(
                ApplicationStatus.REJECTED,
                ApprovalLevel.FINISHED,
                1,
                9
        ));
        when(approvalRecordMapper.listByApplicationId(10L)).thenReturn(List.of(
                record(1L, ApprovalAction.RETURN, "首次退回"),
                record(2L, ApprovalAction.RETURN, "再次退回"),
                record(3L, ApprovalAction.REJECT, "材料不符合要求")
        ));

        var flow = service.getFlow(10L);

        assertEquals("再次退回", flow.returnReason());
        assertEquals("材料不符合要求", flow.rejectReason());
        assertEquals(List.of(1L, 2L, 3L), flow.records().stream().map(record -> record.id()).toList());
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

    private ApprovalRecordEntity record(Long id, ApprovalAction action, String comment) {
        return ApprovalRecordEntity.builder()
                .id(id)
                .applicationId(10L)
                .reviewRound(1)
                .approvalLevel(ApprovalRecordLevel.SCHOOL)
                .approverId(99L)
                .action(action)
                .comment(comment)
                .oldStatus(ApplicationStatus.SCHOOL_PENDING)
                .newStatus(action == ApprovalAction.REJECT
                        ? ApplicationStatus.REJECTED
                        : ApplicationStatus.SCHOOL_RETURNED)
                .requestId("record-" + id)
                .createTime(LocalDateTime.of(2026, 7, 20, 10, id.intValue()))
                .build();
    }
}
