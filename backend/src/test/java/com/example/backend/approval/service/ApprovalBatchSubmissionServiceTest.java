package com.example.backend.approval.service;

import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApplicationType;
import com.example.backend.approval.domain.ApprovalAction;
import com.example.backend.approval.domain.ApprovalErrorCode;
import com.example.backend.approval.domain.ApprovalException;
import com.example.backend.approval.domain.ApprovalLevel;
import com.example.backend.approval.persistence.entity.ApprovalRecordEntity;
import com.example.backend.approval.persistence.entity.ApprovalSubmissionRecordEntity;
import com.example.backend.approval.persistence.mapper.ApprovalRecordMapper;
import com.example.backend.approval.persistence.mapper.ApprovalSubmissionRecordMapper;
import com.example.backend.approval.persistence.type.ApprovalRecordLevel;
import com.example.backend.approval.persistence.type.BatchType;
import com.example.backend.approval.persistence.type.SubmissionLevel;
import com.example.backend.approval.persistence.type.SubmissionType;
import com.example.backend.approval.port.ApprovalBatchQueryService;
import com.example.backend.approval.port.ApprovalResourceService;
import com.example.backend.approval.port.ApprovalSubmissionApplicationQueryService;
import com.example.backend.approval.port.ApplicationStateQueryService;
import com.example.backend.approval.port.ApplicationStateSnapshot;
import com.example.backend.approval.port.ApplicationStateWriteService;
import com.example.backend.approval.port.LoginUser;
import com.example.backend.approval.port.StudentScopeService;
import com.example.backend.approval.port.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalBatchSubmissionServiceTest {

    private ApprovalSubmissionRecordMapper submissions;
    private ApprovalRecordMapper records;
    private ApplicationStateQueryService stateQuery;
    private ApplicationStateWriteService stateWriter;
    private ApprovalBatchQueryService batches;
    private ApprovalSubmissionApplicationQueryService applications;
    private StudentScopeService scopes;
    private ApprovalResourceService resources;
    private ApprovalBatchSubmissionService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        submissions = mock(ApprovalSubmissionRecordMapper.class);
        records = mock(ApprovalRecordMapper.class);
        stateQuery = mock(ApplicationStateQueryService.class);
        stateWriter = mock(ApplicationStateWriteService.class);
        batches = mock(ApprovalBatchQueryService.class);
        applications = mock(ApprovalSubmissionApplicationQueryService.class);
        scopes = mock(StudentScopeService.class);
        resources = mock(ApprovalResourceService.class);
        ObjectProvider<ApprovalBatchQueryService> batchProvider = mock(ObjectProvider.class);
        ObjectProvider<ApprovalSubmissionApplicationQueryService> applicationProvider = mock(ObjectProvider.class);
        ObjectProvider<StudentScopeService> scopeProvider = mock(ObjectProvider.class);
        ObjectProvider<ApprovalResourceService> resourceProvider = mock(ObjectProvider.class);
        when(batchProvider.getIfAvailable()).thenReturn(batches);
        when(applicationProvider.getIfAvailable()).thenReturn(applications);
        when(scopeProvider.getIfAvailable()).thenReturn(scopes);
        when(resourceProvider.getIfAvailable()).thenReturn(resources);
        when(submissions.findByRequestId(anyString())).thenReturn(Optional.empty());
        when(submissions.listByScope(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(batches.getRequiredBatch(BatchType.GREEN_CHANNEL, 30L)).thenReturn(batch());
        when(scopes.isCounselorResponsibleFor(99L, 20L)).thenReturn(true);
        service = new ApprovalBatchSubmissionService(
                submissions, records, stateQuery, stateWriter,
                batchProvider, applicationProvider, scopeProvider, resourceProvider,
                Clock.fixed(Instant.parse("2026-07-21T12:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void counselorInitialSubmissionAdvancesOnlyApprovedApplicationsAndWritesOneBatchRecord() {
        ApplicationStateSnapshot approved = snapshot(10L, ApplicationStatus.COUNSELOR_PENDING, 3);
        when(applications.listByBatch(BatchType.GREEN_CHANNEL, 30L)).thenReturn(List.of(approved));
        when(records.findLatestDecision(10L, 1, ApprovalRecordLevel.COUNSELOR))
                .thenReturn(Optional.of(decision(10L, ApprovalAction.APPROVE)));

        var result = service.submitInitial(counselor(), BatchType.GREEN_CHANNEL, 30L, "batch-1");

        assertEquals(SubmissionLevel.COUNSELOR, result.submissionLevel());
        assertEquals(SubmissionType.INITIAL_BATCH, result.submissionType());
        assertEquals(1, result.submittedCount());
        verify(stateWriter).updateState(
                10L, ApplicationStatus.COUNSELOR_PENDING, ApplicationStatus.COLLEGE_PENDING,
                ApprovalLevel.COLLEGE, 3, 99L
        );
        ArgumentCaptor<ApprovalRecordEntity> auditCaptor = ArgumentCaptor.forClass(ApprovalRecordEntity.class);
        verify(records).insert(auditCaptor.capture());
        assertEquals(ApprovalAction.SUBMIT, auditCaptor.getValue().getAction());
        assertEquals(ApplicationStatus.COLLEGE_PENDING, auditCaptor.getValue().getNewStatus());
        assertEquals(48, auditCaptor.getValue().getRequestId().length());
        ArgumentCaptor<ApprovalSubmissionRecordEntity> captor = ArgumentCaptor.forClass(ApprovalSubmissionRecordEntity.class);
        verify(submissions).insert(captor.capture());
        assertEquals(0L, captor.getValue().getApplicationId());
        assertEquals(1, captor.getValue().getSubmittedCount());
        assertEquals(BatchType.GREEN_CHANNEL, captor.getValue().getBatchType());
    }

    @Test
    void pendingApplicationWithoutDecisionBlocksInitialSubmission() {
        when(applications.listByBatch(BatchType.GREEN_CHANNEL, 30L))
                .thenReturn(List.of(snapshot(10L, ApplicationStatus.COUNSELOR_PENDING, 3)));
        when(records.findLatestDecision(10L, 1, ApprovalRecordLevel.COUNSELOR)).thenReturn(Optional.empty());

        ApprovalException exception = assertThrows(ApprovalException.class,
                () -> service.submitInitial(counselor(), BatchType.GREEN_CHANNEL, 30L, "batch-2"));

        assertEquals(ApprovalErrorCode.APPROVAL_UNREVIEWED_EXISTS, exception.getCode());
        verify(stateWriter, never()).updateState(any(), any(), any(), any(), any(), any());
        verify(submissions, never()).insert(any());
    }

    @Test
    void statusReportsUnreviewedAndApprovedCountsWithoutAllowingSubmission() {
        ApplicationStateSnapshot approved = snapshot(10L, ApplicationStatus.COUNSELOR_PENDING, 3);
        ApplicationStateSnapshot undecided = snapshot(11L, ApplicationStatus.COUNSELOR_PENDING, 3);
        when(applications.listByBatch(BatchType.GREEN_CHANNEL, 30L)).thenReturn(List.of(approved, undecided));
        when(scopes.isCounselorResponsibleFor(99L, 20L)).thenReturn(true);
        when(records.findLatestDecision(10L, 1, ApprovalRecordLevel.COUNSELOR))
                .thenReturn(Optional.of(decision(10L, ApprovalAction.APPROVE)));
        when(records.findLatestDecision(11L, 1, ApprovalRecordLevel.COUNSELOR)).thenReturn(Optional.empty());

        var status = service.getStatus(counselor(), BatchType.GREEN_CHANNEL, 30L);

        assertEquals(1, status.pendingReviewCount());
        assertEquals(1, status.approvedWaitingSubmitCount());
        assertFalse(status.canSubmit());
    }

    private LoginUser counselor() {
        return new LoginUser(99L, UserRole.COUNSELOR, null, null);
    }

    private ApprovalBatchQueryService.ApprovalBatchSnapshot batch() {
        return new ApprovalBatchQueryService.ApprovalBatchSnapshot(
                BatchType.GREEN_CHANNEL,
                30L,
                true,
                LocalDateTime.of(2026, 7, 20, 12, 0),
                LocalDateTime.of(2026, 7, 22, 12, 0)
        );
    }

    private ApplicationStateSnapshot snapshot(Long id, ApplicationStatus status, int version) {
        return new ApplicationStateSnapshot(
                id, 20L, BatchType.GREEN_CHANNEL, 30L, ApplicationType.GREEN_CHANNEL,
                status, ApprovalLevel.COUNSELOR, 1, version
        );
    }

    private ApprovalRecordEntity decision(Long applicationId, ApprovalAction action) {
        return ApprovalRecordEntity.builder()
                .applicationId(applicationId)
                .reviewRound(1)
                .approvalLevel(ApprovalRecordLevel.COUNSELOR)
                .action(action)
                .build();
    }
}
