package com.example.backend.approval.integration;

import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApplicationType;
import com.example.backend.approval.domain.ApprovalLevel;
import com.example.backend.approval.persistence.type.BatchType;
import com.example.backend.approval.port.ApplicationStateSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationStateServiceAdapterTest {

    private final com.example.backend.application.port.ApplicationStateQueryService queryService =
            mock(com.example.backend.application.port.ApplicationStateQueryService.class);
    private final com.example.backend.application.port.ApplicationStateWriteService writeService =
            mock(com.example.backend.application.port.ApplicationStateWriteService.class);
    private final ApplicationStateServiceAdapter adapter =
            new ApplicationStateServiceAdapter(queryService, writeService);

    @Test
    void convertsApplicationSnapshotToApprovalContract() {
        when(queryService.getRequiredState(10L)).thenReturn(applicationSnapshot(
                com.example.backend.application.domain.ApplicationStatus.SCHOOL_RETURNED,
                com.example.backend.application.domain.ApprovalLevel.STUDENT,
                4
        ));

        ApplicationStateSnapshot result = adapter.getRequiredState(10L);

        assertEquals(ApplicationStatus.SCHOOL_RETURNED, result.status());
        assertEquals(ApprovalLevel.STUDENT, result.currentLevel());
        assertEquals(BatchType.GREEN_CHANNEL, result.batchType());
        assertEquals(ApplicationType.GREEN_CHANNEL, result.applicationType());
        assertEquals(4, result.version());
    }

    @Test
    void delegatesStateWriteUsingApplicationModuleEnums() {
        when(writeService.updateState(
                10L,
                com.example.backend.application.domain.ApplicationStatus.CONFIRM_PENDING,
                com.example.backend.application.domain.ApplicationStatus.COMPLETED,
                com.example.backend.application.domain.ApprovalLevel.FINISHED,
                4,
                99L
        )).thenReturn(applicationSnapshot(
                com.example.backend.application.domain.ApplicationStatus.COMPLETED,
                com.example.backend.application.domain.ApprovalLevel.FINISHED,
                5
        ));

        ApplicationStateSnapshot result = adapter.updateState(
                10L,
                ApplicationStatus.CONFIRM_PENDING,
                ApplicationStatus.COMPLETED,
                ApprovalLevel.FINISHED,
                4,
                99L
        );

        assertEquals(ApplicationStatus.COMPLETED, result.status());
        assertEquals(ApprovalLevel.FINISHED, result.currentLevel());
        assertEquals(5, result.version());
        verify(writeService).updateState(
                10L,
                com.example.backend.application.domain.ApplicationStatus.CONFIRM_PENDING,
                com.example.backend.application.domain.ApplicationStatus.COMPLETED,
                com.example.backend.application.domain.ApprovalLevel.FINISHED,
                4,
                99L
        );
    }

    private com.example.backend.application.dto.ApplicationStateSnapshot applicationSnapshot(
            com.example.backend.application.domain.ApplicationStatus status,
            com.example.backend.application.domain.ApprovalLevel level,
            int version
    ) {
        return new com.example.backend.application.dto.ApplicationStateSnapshot(
                10L,
                20L,
                com.example.backend.application.domain.BatchType.GREEN_CHANNEL,
                30L,
                com.example.backend.application.domain.ApplicationType.GREEN_CHANNEL,
                status,
                level,
                2,
                version
        );
    }
}
