package com.example.backend.approval.service;

import com.example.backend.approval.api.SystemMessageService;
import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApplicationType;
import com.example.backend.approval.domain.ApprovalAction;
import com.example.backend.approval.domain.ApprovalErrorCode;
import com.example.backend.approval.domain.ApprovalException;
import com.example.backend.approval.domain.ApprovalLevel;
import com.example.backend.approval.persistence.entity.ApprovalRecordEntity;
import com.example.backend.approval.persistence.mapper.ApprovalRecordMapper;
import com.example.backend.approval.persistence.type.ApprovalRecordLevel;
import com.example.backend.approval.persistence.type.BatchType;
import com.example.backend.approval.port.ApprovalMessageRecipientResolver;
import com.example.backend.approval.port.ApprovalResourceService;
import com.example.backend.approval.port.ApplicationStateQueryService;
import com.example.backend.approval.port.ApplicationStateSnapshot;
import com.example.backend.approval.port.ApplicationStateWriteService;
import com.example.backend.approval.port.LoginUser;
import com.example.backend.approval.port.StudentScopeService;
import com.example.backend.approval.port.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalReviewServiceTest {

    private ApplicationStateQueryService query;
    private ApplicationStateWriteService writer;
    private ApprovalRecordMapper records;
    private ObjectProvider<ApprovalResourceService> resources;
    private ObjectProvider<com.example.backend.application.port.ApplicationDetailService> details;
    private ObjectProvider<ApprovalMessageRecipientResolver> recipients;
    private ObjectProvider<SystemMessageService> messages;
    private ObjectProvider<StudentScopeService> scopes;
    private StudentScopeService scopeService;
    private ApprovalReviewService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        query = mock(ApplicationStateQueryService.class);
        writer = mock(ApplicationStateWriteService.class);
        records = mock(ApprovalRecordMapper.class);
        resources = mock(ObjectProvider.class);
        details = mock(ObjectProvider.class);
        recipients = mock(ObjectProvider.class);
        messages = mock(ObjectProvider.class);
        scopes = mock(ObjectProvider.class);
        scopeService = mock(StudentScopeService.class);
        when(records.findByRequestId(anyString())).thenReturn(Optional.empty());
        when(query.getRequiredState(10L)).thenReturn(snapshot());
        when(scopes.getIfAvailable()).thenReturn(scopeService);
        when(scopeService.isCounselorResponsibleFor(99L, 20L)).thenReturn(true);
        service = new ApprovalReviewService(
                query, writer, records, resources, details, recipients, messages, scopes
        );
    }

    @Test
    void counselorApprovalWritesDecisionWithoutAdvancingState() {
        var result = service.review(
                counselor(), 10L, command(ApprovalAction.APPROVE, "材料完整", "review-10")
        );

        assertEquals(ApplicationStatus.COUNSELOR_PENDING, result.status());
        assertEquals(3, result.version());
        ArgumentCaptor<ApprovalRecordEntity> captor = ArgumentCaptor.forClass(ApprovalRecordEntity.class);
        verify(records).insert(captor.capture());
        assertEquals(ApprovalAction.APPROVE, captor.getValue().getAction());
        assertEquals(ApplicationStatus.COUNSELOR_PENDING, captor.getValue().getNewStatus());
        verify(writer, never()).updateState(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void repeatedApprovalWithoutStateChangeReturnsTheOriginalVersion() {
        ApprovalRecordEntity previous = ApprovalRecordEntity.builder()
                .applicationId(10L)
                .reviewRound(1)
                .approvalLevel(ApprovalRecordLevel.COUNSELOR)
                .action(ApprovalAction.APPROVE)
                .oldStatus(ApplicationStatus.COUNSELOR_PENDING)
                .newStatus(ApplicationStatus.COUNSELOR_PENDING)
                .requestId("same-review")
                .build();
        when(records.findByRequestId("same-review")).thenReturn(Optional.of(previous));

        var result = service.review(
                counselor(), 10L, command(ApprovalAction.APPROVE, "材料完整", "same-review")
        );

        assertEquals(3, result.version());
        verify(query, never()).getRequiredState(any());
        verify(records, never()).insert(any());
    }

    @Test
    void rejectsModifyActionFromTheDecisionEndpoint() {
        ApprovalException exception = assertThrows(ApprovalException.class, () -> service.review(
                counselor(), 10L, command(ApprovalAction.MODIFY, "修改字段", "modify-review")
        ));

        assertEquals(ApprovalErrorCode.APPROVAL_ACTION_REQUIRED, exception.getCode());
        verify(query, never()).getRequiredState(any());
        verify(records, never()).insert(any());
    }

    @Test
    void rejectsAReusedRequestIdWithDifferentDecision() {
        ApprovalRecordEntity previous = ApprovalRecordEntity.builder()
                .applicationId(10L)
                .reviewRound(1)
                .approvalLevel(ApprovalRecordLevel.COUNSELOR)
                .action(ApprovalAction.APPROVE)
                .oldStatus(ApplicationStatus.COUNSELOR_PENDING)
                .newStatus(ApplicationStatus.COUNSELOR_PENDING)
                .requestId("reused-review")
                .build();
        when(records.findByRequestId("reused-review")).thenReturn(Optional.of(previous));

        ApprovalException exception = assertThrows(ApprovalException.class, () -> service.review(
                counselor(), 10L, command(ApprovalAction.RETURN, "请补材料", "reused-review")
        ));

        assertEquals(ApprovalErrorCode.APPROVAL_ALREADY_PROCESSED, exception.getCode());
        verify(query, never()).getRequiredState(any());
    }

    @Test
    void returnRequiresAReason() {
        ApprovalException exception = assertThrows(ApprovalException.class, () -> service.review(
                counselor(), 10L, command(ApprovalAction.RETURN, " ", "return-without-reason")
        ));

        assertEquals(ApprovalErrorCode.APPROVAL_COMMENT_REQUIRED, exception.getCode());
        verify(query, never()).getRequiredState(any());
    }

    @Test
    void rejectsAUserOutsideTheCounselorScope() {
        when(scopeService.isCounselorResponsibleFor(99L, 20L)).thenReturn(false);

        ApprovalException exception = assertThrows(ApprovalException.class, () -> service.review(
                counselor(), 10L, command(ApprovalAction.APPROVE, "材料完整", "out-of-scope")
        ));

        assertEquals(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, exception.getCode());
        verify(records, never()).insert(any());
    }

    private LoginUser counselor() {
        return new LoginUser(99L, UserRole.COUNSELOR, null, null);
    }

    private ApprovalReviewService.ReviewCommand command(
            ApprovalAction action,
            String comment,
            String requestId
    ) {
        return new ApprovalReviewService.ReviewCommand(
                10L, ApprovalRecordLevel.COUNSELOR, action, comment, null, 3, requestId
        );
    }

    private ApplicationStateSnapshot snapshot() {
        return new ApplicationStateSnapshot(
                10L, 20L, BatchType.GREEN_CHANNEL, 30L, ApplicationType.GREEN_CHANNEL,
                ApplicationStatus.COUNSELOR_PENDING, ApprovalLevel.COUNSELOR, 1, 3
        );
    }
}
