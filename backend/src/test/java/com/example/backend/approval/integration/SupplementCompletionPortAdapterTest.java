package com.example.backend.approval.integration;

import com.example.backend.approval.api.ApplicationStatusResult;
import com.example.backend.approval.api.ApprovalTransitionService;
import com.example.backend.application.domain.ApplicationStatus;
import com.example.backend.application.domain.ApprovalLevel;
import com.example.backend.model.vo.supplement.SupplementCompletionResultVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplementCompletionPortAdapterTest {

    @Test
    void delegatesTheAutomaticReviewAndMapsTheOwnedStatusSnapshot() {
        ApprovalTransitionService transitionService = mock(ApprovalTransitionService.class);
        when(transitionService.completeSupplementReview(10L, true, 3, "supplement-10", 99L))
                .thenReturn(new ApplicationStatusResult(
                        10L,
                        ApplicationStatus.CONFIRM_PENDING,
                        ApprovalLevel.CONFIRMATION,
                        4
                ));
        SupplementCompletionPortAdapter adapter = new SupplementCompletionPortAdapter(transitionService);

        SupplementCompletionResultVO result =
                adapter.completeSupplementReview(10L, true, 3, "supplement-10", 99L);

        verify(transitionService).completeSupplementReview(10L, true, 3, "supplement-10", 99L);
        assertEquals("CONFIRM_PENDING", result.getStatus());
        assertEquals("CONFIRMATION", result.getCurrentLevel());
        assertEquals(4, result.getVersion());
    }
}
