package com.example.backend.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.backend.application.domain.*;
import com.example.backend.application.dto.ApplicationDraftCommand;
import com.example.backend.application.dto.ArrearsItemCommand;
import com.example.backend.application.dto.GiftApplicationItemCommand;
import com.example.backend.application.exception.ApplicationException;
import com.example.backend.application.mapper.*;
import com.example.backend.application.dto.ArrearsItemSnapshot;
import com.example.backend.mapper.StudentMapper;
import com.example.backend.model.domain.Student;
import com.example.backend.service.BatchQueryService;
import java.util.UUID;
import java.util.List;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ApplicationServiceTest {
    private final ApplicationMapper applications = mock(ApplicationMapper.class);
    private final ApplicationOperationMapper operations = mock(ApplicationOperationMapper.class);
    private final ArrearsApplicationMapper arrears = mock(ArrearsApplicationMapper.class);
    private final ApplicationResourceMapper resources = mock(ApplicationResourceMapper.class);
    private final StudentMapper students = mock(StudentMapper.class);
    private final RecommendationService recommendations = mock(RecommendationService.class);
    private final BatchQueryService batches = mock(BatchQueryService.class);
    private final ApplicationService service = new ApplicationService(applications, operations, arrears, resources, students, recommendations, batches);

    @Test
    void createsGreenChannelDraftAndRecordsIdempotency() {
        when(operations.findApplicationIdByRequestId(anyString())).thenReturn(null);
        Student student = new Student(); student.setId(10L); student.setGradeId(4L); student.setEnabled(1); student.setDeleted(0L);
        when(students.selectById(10L)).thenReturn(student);
        when(applications.findActiveByUnique(10L, ApplicationType.GREEN_CHANNEL, BatchType.GREEN_CHANNEL, 20L)).thenReturn(null);
        doAnswer(invocation -> { invocation.<Application>getArgument(0).setId(30L); return 1; }).when(applications).insert(any(Application.class));

        var result = service.create(10L, 11L, new ApplicationDraftCommand(ApplicationType.GREEN_CHANNEL,
                BatchType.GREEN_CHANNEL, 20L, UUID.randomUUID().toString(), "家庭困难"));

        assertEquals(30L, result.applicationId());
        assertEquals(ApplicationStatus.DRAFT, result.status());
        assertEquals(20L, result.batchId());
        verify(operations).insert(eq(30L), eq("CREATE_DRAFT"), anyString(), eq(11L));
    }

    @Test
    void rejectsGiftDetailsForNonGreenChannelApplications() {
        Application application = new Application();
        application.setId(30L);
        application.setApplicationType(ApplicationType.LIVING_SUBSIDY);
        application.setStatus(ApplicationStatus.DRAFT);
        application.setVersion(0);
        when(applications.findRequired(30L)).thenReturn(application);

        var exception = assertThrows(ApplicationException.class, () -> service.replaceGiftItems(30L, 0,
                List.of(new GiftApplicationItemCommand(9L, 1)), 11L));

        assertEquals("APPLICATION_INVALID_STATUS", exception.getCode());
        verifyNoInteractions(resources);
    }

    @Test
    void derivesArrearsReasonOnServerAndIgnoresClientReasonCode() {
        Application application = new Application();
        Student student = new Student(); student.setId(10L); student.setOriginLoan(0); student.setCampusLoan(0);
        application.setId(30L); application.setApplicationType(ApplicationType.GREEN_CHANNEL);
        application.setStudentId(10L); application.setStatus(ApplicationStatus.DRAFT); application.setVersion(0);
        when(applications.findRequired(30L)).thenReturn(application);
        when(students.selectById(10L)).thenReturn(student);
        when(arrears.findItemsByApplicationId(30L)).thenReturn(List.of(
                new ArrearsItemSnapshot(30L, 9L, "学费", new BigDecimal("100.00"), "OTHER")));
        when(applications.updateDraft(30L, null, 0, 11L)).thenReturn(1);

        service.replaceArrearsItems(30L, 0,
                List.of(new ArrearsItemCommand(9L, new BigDecimal("100.00"), "UNTRUSTED_VALUE")), 11L);

        verify(arrears).deleteActiveByApplicationId(30L);
        verify(arrears).insert(30L, 9L, new BigDecimal("100.00"), "OTHER");
    }
}
