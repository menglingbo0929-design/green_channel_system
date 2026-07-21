package com.example.backend.approval.service;

import com.example.backend.approval.api.ApprovalListQuery;
import com.example.backend.approval.api.ApprovalPage;
import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApplicationType;
import com.example.backend.approval.domain.ApprovalLevel;
import com.example.backend.approval.persistence.mapper.ApprovalRecordMapper;
import com.example.backend.approval.persistence.type.ApprovalRecordLevel;
import com.example.backend.approval.persistence.type.BatchType;
import com.example.backend.approval.port.ApprovalApplicationQueryPort;
import com.example.backend.approval.port.ApprovalApplicationSnapshot;
import com.example.backend.approval.port.ApplicationStateQueryService;
import com.example.backend.approval.port.LoginUser;
import com.example.backend.approval.port.StudentScopeService;
import com.example.backend.approval.port.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultApprovalWorkbenchQueryServiceTest {

    @Test
    void pendingQueryFixesTheStatusToTheCurrentReviewLevel() {
        ApplicationStateQueryService states = mock(ApplicationStateQueryService.class);
        StudentScopeService scopes = mock(StudentScopeService.class);
        ApprovalApplicationQueryPort applications = mock(ApprovalApplicationQueryPort.class);
        ApprovalRecordMapper records = mock(ApprovalRecordMapper.class);
        when(applications.pagePending(any(), any())).thenReturn(new ApprovalPage<>(List.of(snapshot()), 1, 1, 10));
        when(records.findLatestDecision(10L, 2, ApprovalRecordLevel.COUNSELOR)).thenReturn(Optional.empty());

        DefaultApprovalWorkbenchQueryService service = new DefaultApprovalWorkbenchQueryService(
                states, scopes, applications, records
        );

        var page = service.pagePending(
                new LoginUser(99L, UserRole.COUNSELOR, null, null), query()
        );

        assertEquals(1, page.total());
        assertEquals(ApplicationStatus.COUNSELOR_PENDING, page.records().getFirst().status());
        assertEquals("待辅导员审核", page.records().getFirst().statusName());
        ArgumentCaptor<ApprovalListQuery> queryCaptor = ArgumentCaptor.forClass(ApprovalListQuery.class);
        verify(applications).pagePending(any(), queryCaptor.capture());
        assertEquals(ApplicationStatus.COUNSELOR_PENDING, queryCaptor.getValue().status());
    }

    @Test
    void processedQueryStartsFromMemberThreeDecisionRecords() {
        ApplicationStateQueryService states = mock(ApplicationStateQueryService.class);
        StudentScopeService scopes = mock(StudentScopeService.class);
        ApprovalApplicationQueryPort applications = mock(ApprovalApplicationQueryPort.class);
        ApprovalRecordMapper records = mock(ApprovalRecordMapper.class);
        when(records.listProcessedApplicationIds(ApprovalRecordLevel.COLLEGE, 77L)).thenReturn(List.of(10L));
        when(applications.pageByApplicationIds(any(), any(), org.mockito.ArgumentMatchers.eq(List.of(10L))))
                .thenReturn(new ApprovalPage<>(List.of(snapshot()), 1, 1, 10));
        when(records.findLatestDecision(10L, 2, ApprovalRecordLevel.COLLEGE)).thenReturn(Optional.empty());

        DefaultApprovalWorkbenchQueryService service = new DefaultApprovalWorkbenchQueryService(
                states, scopes, applications, records
        );

        var page = service.pageProcessed(new LoginUser(77L, UserRole.COLLEGE, null, 8L), query());

        assertEquals(1, page.records().size());
        verify(records).listProcessedApplicationIds(ApprovalRecordLevel.COLLEGE, 77L);
    }

    private ApprovalListQuery query() {
        return new ApprovalListQuery(1, 10, null, null, null, null, null, null, null, null);
    }

    private ApprovalApplicationSnapshot snapshot() {
        return new ApprovalApplicationSnapshot(
                10L, "GC202607210001", ApplicationType.GREEN_CHANNEL, BatchType.GREEN_CHANNEL, 3L,
                20L, "20260001", "张三", 8L, "计算机学院", "2026级",
                ApplicationStatus.COUNSELOR_PENDING, ApprovalLevel.COUNSELOR, 2,
                LocalDateTime.of(2026, 7, 21, 9, 0), 4
        );
    }
}
