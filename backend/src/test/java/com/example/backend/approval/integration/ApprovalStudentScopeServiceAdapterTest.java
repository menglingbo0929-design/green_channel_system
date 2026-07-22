package com.example.backend.approval.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalStudentScopeServiceAdapterTest {

    private final com.example.backend.service.StudentScopeService delegate =
            mock(com.example.backend.service.StudentScopeService.class);
    private final ApprovalStudentScopeServiceAdapter adapter =
            new ApprovalStudentScopeServiceAdapter(delegate);

    @Test
    void delegatesCounselorAndCollegeScopeChecks() {
        when(delegate.isCounselorResponsibleFor(10L, 20L)).thenReturn(true);
        when(delegate.isStudentInCollege(20L, 30L)).thenReturn(false);

        assertTrue(adapter.isCounselorResponsibleFor(10L, 20L));
        assertFalse(adapter.isStudentInCollege(20L, 30L));
        verify(delegate).isCounselorResponsibleFor(10L, 20L);
        verify(delegate).isStudentInCollege(20L, 30L);
    }
}
