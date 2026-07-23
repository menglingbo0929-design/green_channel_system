package com.example.backend.service.impl;

import com.example.backend.model.dto.ApprovalListQuery;
import com.example.backend.model.dto.ApprovalPage;
import com.example.backend.model.dto.ApprovalLevelCount;
import com.example.backend.model.domain.ApplicationStatus;
import com.example.backend.model.domain.ApplicationType;
import com.example.backend.common.exception.ApprovalErrorCode;
import com.example.backend.common.exception.ApprovalException;
import com.example.backend.model.domain.ApprovalLevel;
import com.example.backend.mapper.ApprovalRecordMapper;
import com.example.backend.model.domain.ApprovalRecordLevel;
import com.example.backend.model.domain.BatchType;
import com.example.backend.service.ApprovalApplicationQueryPort;
import com.example.backend.model.dto.ApprovalApplicationDetail;
import com.example.backend.model.dto.ApprovalApplicationSnapshot;
import com.example.backend.model.dto.ApprovalDashboardData;
import com.example.backend.model.dto.ApplicationStateSnapshot;
import com.example.backend.service.ApplicationStateQueryService;
import com.example.backend.model.dto.ApplicationStateSnapshot;
import com.example.backend.model.dto.LoginUser;
import com.example.backend.service.StudentScopeService;
import com.example.backend.model.domain.UserRole;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertEquals(new BigDecimal("1200.00"), page.records().getFirst().declaredAmount());
        assertEquals("GC-2026-01", page.records().getFirst().batchCode());
        assertEquals("2026 年新生绿色通道", page.records().getFirst().batchName());
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

    @Test
    void studentCannotOpenTheReviewerWorkbench() {
        DefaultApprovalWorkbenchQueryService service = serviceWithMocks();

        ApprovalException exception = assertThrows(ApprovalException.class, () -> service.pagePending(
                new LoginUser(30L, UserRole.STUDENT, 20L, null), query()
        ));

        assertEquals(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, exception.getCode());
    }

    @Test
    void studentCanReadOnlyTheirOwnDetailWithoutReviewActions() {
        ApplicationStateQueryService states = mock(ApplicationStateQueryService.class);
        StudentScopeService scopes = mock(StudentScopeService.class);
        ApprovalApplicationQueryPort applications = mock(ApprovalApplicationQueryPort.class);
        ApprovalRecordMapper records = mock(ApprovalRecordMapper.class);
        when(states.getRequiredState(10L)).thenReturn(stateSnapshot());
        when(applications.getRequiredApprovalDetail(10L)).thenReturn(new ApprovalApplicationDetail(
                Map.of("applicationId", 10L), null, null, null, List.of(), 4
        ));
        when(records.listByApplicationId(10L)).thenReturn(List.of());
        DefaultApprovalWorkbenchQueryService service = new DefaultApprovalWorkbenchQueryService(
                states, scopes, applications, records
        );

        var detail = service.getDetail(new LoginUser(30L, UserRole.STUDENT, 20L, null), 10L);

        assertEquals(4, detail.version());
        assertEquals(List.of(), detail.allowedActions());
        assertEquals(List.of(), detail.editableFields());
    }

    @Test
    void activeCounselorReceivesTheGreenChannelFieldWhitelist() {
        ApplicationStateQueryService states = mock(ApplicationStateQueryService.class);
        StudentScopeService scopes = mock(StudentScopeService.class);
        ApprovalApplicationQueryPort applications = mock(ApprovalApplicationQueryPort.class);
        ApprovalRecordMapper records = mock(ApprovalRecordMapper.class);
        when(states.getRequiredState(10L)).thenReturn(stateSnapshot());
        when(scopes.isCounselorResponsibleFor(99L, 20L)).thenReturn(true);
        when(applications.getRequiredApprovalDetail(10L)).thenReturn(new ApprovalApplicationDetail(
                Map.of("applicationId", 10L), null, null, null, List.of(), 4
        ));
        when(records.listByApplicationId(10L)).thenReturn(List.of());
        DefaultApprovalWorkbenchQueryService service = new DefaultApprovalWorkbenchQueryService(
                states, scopes, applications, records
        );

        var detail = service.getDetail(new LoginUser(99L, UserRole.COUNSELOR, null, null), 10L);

        assertEquals(List.of("applicationReason", "arrearsItems", "giftItems"), detail.editableFields());
    }

    @Test
    void dashboardKeepsApplicationAggregatesWhenThereAreNoScopedDecisions() {
        ApplicationStateQueryService states = mock(ApplicationStateQueryService.class);
        StudentScopeService scopes = mock(StudentScopeService.class);
        ApprovalApplicationQueryPort applications = mock(ApprovalApplicationQueryPort.class);
        ApprovalRecordMapper records = mock(ApprovalRecordMapper.class);
        when(applications.getDashboard(any(), any())).thenReturn(new ApprovalDashboardData(
                List.of(new ApprovalLevelCount("COUNSELOR", 2)), List.of(), List.of()
        ));
        when(applications.listScopedApplicationIds(any(), any())).thenReturn(List.of());
        DefaultApprovalWorkbenchQueryService service = new DefaultApprovalWorkbenchQueryService(
                states, scopes, applications, records
        );

        var dashboard = service.getDashboard(
                new LoginUser(99L, UserRole.COUNSELOR, null, null), query()
        );

        assertEquals(2, dashboard.pendingByLevel().getFirst().count());
        assertEquals(List.of(), dashboard.decisionDistribution());
    }

    private DefaultApprovalWorkbenchQueryService serviceWithMocks() {
        return new DefaultApprovalWorkbenchQueryService(
                mock(ApplicationStateQueryService.class),
                mock(StudentScopeService.class),
                mock(ApprovalApplicationQueryPort.class),
                mock(ApprovalRecordMapper.class)
        );
    }

    private ApprovalListQuery query() {
        return new ApprovalListQuery(1, 10, null, null, null, null, null, null, null, null);
    }

    private ApprovalApplicationSnapshot snapshot() {
        return new ApprovalApplicationSnapshot(
                10L, "GC202607210001", ApplicationType.GREEN_CHANNEL, BatchType.GREEN_CHANNEL, 3L,
                "GC-2026-01", "2026 年新生绿色通道",
                20L, "20260001", "张三", 8L, "计算机学院", "2026级", new BigDecimal("1200.00"),
                ApplicationStatus.COUNSELOR_PENDING, ApprovalLevel.COUNSELOR, 2,
                LocalDateTime.of(2026, 7, 21, 9, 0), 4
        );
    }

    private ApplicationStateSnapshot stateSnapshot() {
        return new ApplicationStateSnapshot(
                10L, 20L, BatchType.GREEN_CHANNEL, 3L, ApplicationType.GREEN_CHANNEL,
                ApplicationStatus.COUNSELOR_PENDING, ApprovalLevel.COUNSELOR, 2, 4
        );
    }
}
