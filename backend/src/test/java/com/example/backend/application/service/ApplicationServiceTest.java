package com.example.backend.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.backend.application.domain.*;
import com.example.backend.application.dto.ApplicationDraftCommand;
import com.example.backend.application.exception.ApplicationException;
import com.example.backend.application.mapper.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplicationServiceTest {
    private final ApplicationMapper applications = mock(ApplicationMapper.class);
    private final ApplicationOperationMapper operations = mock(ApplicationOperationMapper.class);
    private final ArrearsApplicationMapper arrears = mock(ArrearsApplicationMapper.class);
    private final ApplicationService service = new ApplicationService(applications, operations, arrears);

    @Test
    void createsGreenChannelDraftAndRecordsIdempotency() {
        when(operations.findApplicationIdByRequestId(anyString())).thenReturn(null);
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
    void rejectsMismatchedApplicationAndBatchTypes() {
        when(operations.findApplicationIdByRequestId(anyString())).thenReturn(null);
        var exception = assertThrows(ApplicationException.class, () -> service.create(10L, 11L,
                new ApplicationDraftCommand(ApplicationType.GREEN_CHANNEL, BatchType.SUBSIDY, 20L, UUID.randomUUID().toString(), "原因")));
        assertEquals("APPLICATION_BATCH_TYPE_INVALID", exception.getCode());
        verifyNoInteractions(applications);
        verify(operations).findApplicationIdByRequestId(anyString());
        verifyNoMoreInteractions(operations);
    }
}
