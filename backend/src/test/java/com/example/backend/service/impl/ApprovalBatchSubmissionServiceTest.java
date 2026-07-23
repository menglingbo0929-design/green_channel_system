package com.example.backend.service.impl;

import com.example.backend.model.domain.ApplicationStatus;
import com.example.backend.model.domain.ApplicationType;
import com.example.backend.model.domain.ApprovalAction;
import com.example.backend.common.exception.ApprovalErrorCode;
import com.example.backend.common.exception.ApprovalException;
import com.example.backend.model.domain.ApprovalLevel;
import com.example.backend.model.domain.ApprovalRecordEntity;
import com.example.backend.model.domain.ApprovalSubmissionRecordEntity;
import com.example.backend.mapper.ApprovalRecordMapper;
import com.example.backend.mapper.ApprovalSubmissionRecordMapper;
import com.example.backend.model.domain.ApprovalRecordLevel;
import com.example.backend.model.domain.BatchType;
import com.example.backend.model.domain.SubmissionLevel;
import com.example.backend.model.domain.SubmissionScopeType;
import com.example.backend.model.domain.SubmissionStatus;
import com.example.backend.model.domain.SubmissionType;
import com.example.backend.service.ApprovalBatchQueryService;
import com.example.backend.service.ApprovalResourceService;
import com.example.backend.service.ApprovalSubmissionApplicationQueryService;
import com.example.backend.service.ApplicationStateQueryService;
import com.example.backend.model.dto.ApplicationStateSnapshot;
import com.example.backend.service.ApplicationStateWriteService;
import com.example.backend.model.dto.LoginUser;
import com.example.backend.service.StudentScopeService;
import com.example.backend.model.domain.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
    void setUp() {
        submissions = mock(ApprovalSubmissionRecordMapper.class);
        records = mock(ApprovalRecordMapper.class);
        stateQuery = mock(ApplicationStateQueryService.class);
        stateWriter = mock(ApplicationStateWriteService.class);
        batches = mock(ApprovalBatchQueryService.class);
        applications = mock(ApprovalSubmissionApplicationQueryService.class);
        scopes = mock(StudentScopeService.class);
        resources = mock(ApprovalResourceService.class);
        when(submissions.findByRequestId(anyString())).thenReturn(Optional.empty());
        when(submissions.listByScope(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(batches.getRequiredBatch(BatchType.GREEN_CHANNEL, 30L)).thenReturn(batch());
        when(scopes.isCounselorResponsibleFor(99L, 20L)).thenReturn(true);
        service = new ApprovalBatchSubmissionService(
                submissions, records, stateQuery, stateWriter,
                batches, applications, scopes, resources,
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

    @Test
    void collegeInitialSubmissionValidatesResourcesAndAdvancesToSchool() {
        ApplicationStateSnapshot approved = new ApplicationStateSnapshot(
                10L, 20L, BatchType.GREEN_CHANNEL, 30L, ApplicationType.GREEN_CHANNEL,
                ApplicationStatus.COLLEGE_PENDING, ApprovalLevel.COLLEGE, 1, 3
        );
        when(applications.listByBatch(BatchType.GREEN_CHANNEL, 30L)).thenReturn(List.of(approved));
        when(scopes.isStudentInCollege(20L, 8L)).thenReturn(true);
        when(records.findLatestDecision(10L, 1, ApprovalRecordLevel.COLLEGE))
                .thenReturn(Optional.of(decision(10L, ApprovalAction.APPROVE)));

        var result = service.submitInitial(
                new LoginUser(77L, UserRole.COLLEGE, null, 8L),
                BatchType.GREEN_CHANNEL,
                30L,
                "college-batch"
        );

        assertEquals(SubmissionLevel.COLLEGE, result.submissionLevel());
        verify(resources).validateCollegeApproval(10L);
        verify(stateWriter).updateState(
                10L, ApplicationStatus.COLLEGE_PENDING, ApplicationStatus.SCHOOL_PENDING,
                ApprovalLevel.SCHOOL, 3, 77L
        );
    }

    @Test
    void returnResubmissionAdvancesOneApprovedApplicationAfterInitialSubmission() {
        ApplicationStateSnapshot approved = snapshot(10L, ApplicationStatus.COUNSELOR_PENDING, 3);
        when(stateQuery.getRequiredState(10L)).thenReturn(approved);
        when(records.findLatestDecision(10L, 1, ApprovalRecordLevel.COUNSELOR))
                .thenReturn(Optional.of(decision(10L, ApprovalAction.APPROVE)));
        when(submissions.listByScope(any(), any(), any(), any(), any()))
                .thenReturn(List.of(initialSubmission("first-batch")));

        var result = service.submitReturnResubmit(counselor(), 10L, 3, "return-submit");

        assertEquals(SubmissionType.RETURN_RESUBMIT, result.submissionType());
        assertEquals(10L, result.applicationId());
        verify(stateWriter).updateState(
                10L, ApplicationStatus.COUNSELOR_PENDING, ApplicationStatus.COLLEGE_PENDING,
                ApprovalLevel.COLLEGE, 3, 99L
        );
    }

    @Test
    void repeatedInitialSubmissionReturnsTheStoredResultWithoutSideEffects() {
        ApprovalSubmissionRecordEntity existing = initialSubmission("same-batch");
        when(submissions.findByRequestId("same-batch")).thenReturn(Optional.of(existing));

        var result = service.submitInitial(counselor(), BatchType.GREEN_CHANNEL, 30L, "same-batch");

        assertEquals(2, result.submittedCount());
        assertEquals(existing.getSubmitTime(), result.submittedAt());
        verify(applications, never()).listByBatch(any(), any());
        verify(stateWriter, never()).updateState(any(), any(), any(), any(), any(), any());
        verify(submissions, never()).insert(any());
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

    private ApprovalSubmissionRecordEntity initialSubmission(String requestId) {
        return ApprovalSubmissionRecordEntity.builder()
                .batchType(BatchType.GREEN_CHANNEL)
                .greenChannelBatchId(30L)
                .submissionLevel(SubmissionLevel.COUNSELOR)
                .submissionType(SubmissionType.INITIAL_BATCH)
                .scopeType(SubmissionScopeType.COUNSELOR)
                .scopeId(99L)
                .applicationId(0L)
                .reviewRound(0)
                .submitterId(99L)
                .submittedCount(2)
                .status(SubmissionStatus.SUBMITTED)
                .requestId(requestId)
                .submitTime(LocalDateTime.of(2026, 7, 21, 12, 0))
                .build();
    }
}
