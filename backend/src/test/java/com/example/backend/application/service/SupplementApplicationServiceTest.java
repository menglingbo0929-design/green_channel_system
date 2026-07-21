package com.example.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.approval.api.ApprovalTransitionService;
import com.example.backend.application.domain.Application;
import com.example.backend.application.domain.ApplicationSource;
import com.example.backend.application.domain.ApplicationStatus;
import com.example.backend.application.domain.ApprovalLevel;
import com.example.backend.application.domain.ApplicationType;
import com.example.backend.application.domain.BatchType;
import com.example.backend.application.dto.ApplicationStateSnapshot;
import com.example.backend.application.exception.ApplicationException;
import com.example.backend.application.mapper.ApplicationMapper;
import com.example.backend.application.mapper.ApplicationOperationMapper;
import com.example.backend.application.mapper.ApplicationResourceMapper;
import com.example.backend.model.dto.supplement.SupplementCreateDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class SupplementApplicationServiceTest {
    private final ApplicationService applications = mock(ApplicationService.class);
    private final ApplicationMapper mapper = mock(ApplicationMapper.class);
    private final ApplicationOperationMapper operations = mock(ApplicationOperationMapper.class);
    private final ApplicationResourceMapper resources = mock(ApplicationResourceMapper.class);
    @SuppressWarnings("unchecked") private final ObjectProvider<ApprovalTransitionService> transitions = mock(ObjectProvider.class);
    private final SupplementApplicationService service = new SupplementApplicationService(applications, mapper, operations, resources, transitions);

    @Test
    void createsSubsidySupplementAndCompletesTheAutomaticReview() {
        SupplementCreateDTO command = subsidyCommand();
        SchoolProxyStudentVO student = student();
        ApprovalTransitionService transition = mock(ApprovalTransitionService.class);
        when(operations.findApplicationIdByRequestId(command.getRequestId())).thenReturn(null);
        when(applications.createSupplementApplication(anyLong(), anyLong(), any())).thenReturn(snapshot(42L, 0));
        when(applications.getRequiredState(42L)).thenReturn(snapshot(42L, 1));
        when(transitions.getIfAvailable()).thenReturn(transition);
        when(mapper.findBySource(42L, ApplicationSource.SUPPLEMENT)).thenReturn(supplementApplication(42L));
        when(applications.containsArrears(42L)).thenReturn(false);

        var result = service.createSupplementDraft(command, student, "ignored", 7L);

        assertEquals("SUPPLEMENT", result.getSource());
        assertEquals("LIVING_SUBSIDY", result.getApplicationType());
        verify(applications).replaceSubsidy(42L, 0, new BigDecimal("300.00"), 7L);
        verify(transition).completeSupplementReview(42L, false, 1, "SUPPLEMENT_COMPLETE_42", 7L);
    }

    @Test
    void rejectsGreenChannelSupplementWithoutAnyDetail() {
        SupplementCreateDTO command = subsidyCommand();
        command.setApplicationType("GREEN_CHANNEL"); command.setSubsidyAmount(null);
        when(operations.findApplicationIdByRequestId(command.getRequestId())).thenReturn(null);

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> service.createSupplementDraft(command, student(), "ignored", 7L));

        assertEquals("SUPPLEMENT_DETAIL_REQUIRED", exception.getCode());
    }

    private SupplementCreateDTO subsidyCommand() {
        SupplementCreateDTO command = new SupplementCreateDTO();
        command.setStudentNo("20260001"); command.setApplicationType("LIVING_SUBSIDY"); command.setBatchId(9L);
        command.setSupplementReason("线下窗口办理"); command.setHandledAt(LocalDateTime.now().minusMinutes(1));
        command.setSubsidyAmount(new BigDecimal("300.00")); command.setRequestId("supplement-create-1");
        return command;
    }

    private SchoolProxyStudentVO student() {
        SchoolProxyStudentVO student = new SchoolProxyStudentVO(); student.setStudentId(8L);
        student.setStudentNo("20260001"); student.setStudentName("张三"); return student;
    }

    private ApplicationStateSnapshot snapshot(Long id, int version) {
        return new ApplicationStateSnapshot(id, 8L, BatchType.SUBSIDY, 9L, ApplicationType.LIVING_SUBSIDY,
                ApplicationStatus.DRAFT, ApprovalLevel.STUDENT, 0, version);
    }

    private Application supplementApplication(Long id) {
        Application application = new Application(); application.setId(id); application.setApplicationNo("GC2026000002");
        application.setStudentId(8L); application.setApplicationType(ApplicationType.LIVING_SUBSIDY); application.setBatchType(BatchType.SUBSIDY);
        application.setSubsidyBatchId(9L); application.setSource(ApplicationSource.SUPPLEMENT); application.setStatus(ApplicationStatus.COMPLETED);
        application.setCurrentLevel(ApprovalLevel.FINISHED); application.setVersion(2); application.setCreateBy(7L);
        application.setSupplementReason("线下窗口办理"); application.setSupplementedAt(LocalDateTime.now().minusMinutes(1)); return application;
    }
}
