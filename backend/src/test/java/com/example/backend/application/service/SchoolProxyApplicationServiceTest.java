package com.example.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.example.backend.model.dto.schoolproxy.SchoolProxyArrearsItemDTO;
import com.example.backend.model.dto.schoolproxy.SchoolProxyDraftDTO;
import com.example.backend.model.dto.schoolproxy.SchoolProxyGiftItemDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.service.port.SchoolProxyStudentQueryPort;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class SchoolProxyApplicationServiceTest {
    private final ApplicationService applications = mock(ApplicationService.class);
    private final ApplicationMapper mapper = mock(ApplicationMapper.class);
    private final ApplicationOperationMapper operations = mock(ApplicationOperationMapper.class);
    private final ApplicationResourceMapper resources = mock(ApplicationResourceMapper.class);
    @SuppressWarnings("unchecked") private final ObjectProvider<SchoolProxyStudentQueryPort> students = mock(ObjectProvider.class);
    private final SchoolProxyApplicationService service = new SchoolProxyApplicationService(applications, mapper, operations, resources, students);

    @Test
    void returnsDependencyUnavailableWhenStudentPortIsNotInstalled() {
        SchoolProxyDraftDTO command = draft();
        when(operations.findApplicationIdByRequestId(command.getRequestId())).thenReturn(null);
        when(students.getIfAvailable()).thenReturn(null);

        ApplicationException exception = assertThrows(ApplicationException.class, () -> service.createDraft(command, 7L));

        assertEquals("DEPENDENCY_UNAVAILABLE", exception.getCode());
    }

    @Test
    void createsProxyDraftAndWritesArrearsAndGiftDetails() {
        SchoolProxyDraftDTO command = draft();
        SchoolProxyStudentVO student = new SchoolProxyStudentVO(); student.setStudentId(8L);
        Application stored = proxyApplication(31L, 2);
        when(operations.findApplicationIdByRequestId(command.getRequestId())).thenReturn(null);
        when(students.getIfAvailable()).thenReturn(studentNo -> student);
        when(applications.createSchoolProxyApplication(anyLong(), anyLong(), any())).thenReturn(snapshot(31L, 0));
        when(applications.getRequiredState(31L)).thenReturn(snapshot(31L, 1));
        when(resources.findBatchGiftItemId(9L, 4L)).thenReturn(41L);
        when(mapper.findBySource(31L, ApplicationSource.SCHOOL_PROXY)).thenReturn(stored);

        var result = service.createDraft(command, 7L);

        assertEquals(31L, result.getApplicationId());
        assertEquals("SCHOOL_PROXY", result.getSource());
        verify(applications).replaceArrearsItems(anyLong(), anyInt(), any(), anyLong());
        verify(applications).replaceGiftItems(anyLong(), anyInt(), any(), anyLong());
        verify(resources).findBatchGiftItemId(9L, 4L);
    }

    @Test
    void refusesSubmissionUntilAttachmentStorageAndResourceReservationAreAvailable() {
        when(mapper.findBySource(31L, ApplicationSource.SCHOOL_PROXY)).thenReturn(proxyApplication(31L, 2));

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> service.submit(31L, 2, "proxy-submit-1", 7L));

        assertEquals("SCHOOL_PROXY_SUBMISSION_UNAVAILABLE", exception.getCode());
    }

    private SchoolProxyDraftDTO draft() {
        SchoolProxyArrearsItemDTO arrears = new SchoolProxyArrearsItemDTO();
        arrears.setFeeItemId(3L); arrears.setDeclaredAmount(new BigDecimal("1200.00")); arrears.setArrearsReasonCode("FAMILY_EMERGENCY");
        SchoolProxyGiftItemDTO gift = new SchoolProxyGiftItemDTO(); gift.setGiftItemId(4L); gift.setQuantity(1);
        SchoolProxyDraftDTO command = new SchoolProxyDraftDTO();
        command.setStudentNo("20260001"); command.setBatchType("GREEN_CHANNEL"); command.setBatchId(9L);
        command.setRequestId("proxy-create-1"); command.setArrearsItems(List.of(arrears)); command.setGiftItems(List.of(gift));
        return command;
    }

    private ApplicationStateSnapshot snapshot(Long id, int version) {
        return new ApplicationStateSnapshot(id, 8L, BatchType.GREEN_CHANNEL, 9L, ApplicationType.GREEN_CHANNEL,
                ApplicationStatus.DRAFT, ApprovalLevel.STUDENT, 0, version);
    }

    private Application proxyApplication(Long id, int version) {
        Application application = new Application(); application.setId(id); application.setApplicationNo("GC2026000001");
        application.setSource(ApplicationSource.SCHOOL_PROXY); application.setStatus(ApplicationStatus.DRAFT); application.setVersion(version);
        return application;
    }
}
