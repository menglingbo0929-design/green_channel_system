package com.example.backend.approval.integration;

import com.example.backend.approval.api.ApprovalCompletionService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ArrearsConfirmationCompletionPortAdapterTest {

    @Test
    void delegatesTheConfirmationTransactionArgumentsToTheApprovalService() {
        ApprovalCompletionService completionService = mock(ApprovalCompletionService.class);
        ArrearsConfirmationCompletionPortAdapter adapter =
                new ArrearsConfirmationCompletionPortAdapter(completionService);

        adapter.completeAfterConfirmation(10L, 3, "confirm-10", 99L);

        verify(completionService).completeAfterConfirmation(10L, 3, "confirm-10", 99L);
    }
}
