package com.example.backend.approval.service;

import com.example.backend.approval.api.SystemMessageService;
import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApplicationType;
import com.example.backend.approval.domain.ApprovalAction;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalReviewServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void counselorApprovalWritesDecisionWithoutAdvancingState() {
        ApplicationStateQueryService query = mock(ApplicationStateQueryService.class);
        ApplicationStateWriteService writer = mock(ApplicationStateWriteService.class);
        ApprovalRecordMapper records = mock(ApprovalRecordMapper.class);
        ObjectProvider<ApprovalResourceService> resources = mock(ObjectProvider.class);
        ObjectProvider<com.example.backend.application.port.ApplicationDetailService> details = mock(ObjectProvider.class);
        ObjectProvider<ApprovalMessageRecipientResolver> recipients = mock(ObjectProvider.class);
        ObjectProvider<SystemMessageService> messages = mock(ObjectProvider.class);
        ObjectProvider<StudentScopeService> scopes = mock(ObjectProvider.class);
        StudentScopeService scopeService = mock(StudentScopeService.class);

        when(records.findByRequestId(anyString())).thenReturn(Optional.empty());
        when(query.getRequiredState(10L)).thenReturn(snapshot());
        when(scopes.getIfAvailable()).thenReturn(scopeService);
        when(scopeService.isCounselorResponsibleFor(99L, 20L)).thenReturn(true);

        ApprovalReviewService service = new ApprovalReviewService(
                query, writer, records, resources, details, recipients, messages, scopes
        );

        var result = service.review(
                new LoginUser(99L, UserRole.COUNSELOR, null, null),
                10L,
                new ApprovalReviewService.ReviewCommand(
                        10L, ApprovalRecordLevel.COUNSELOR, ApprovalAction.APPROVE,
                        "材料完整", null, 3, "review-10"
                )
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

    private ApplicationStateSnapshot snapshot() {
        return new ApplicationStateSnapshot(
                10L, 20L, BatchType.GREEN_CHANNEL, 30L, ApplicationType.GREEN_CHANNEL,
                ApplicationStatus.COUNSELOR_PENDING, ApprovalLevel.COUNSELOR, 1, 3
        );
    }
}
