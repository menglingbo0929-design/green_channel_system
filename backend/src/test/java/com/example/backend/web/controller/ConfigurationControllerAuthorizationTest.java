package com.example.backend.web.controller;

import com.example.backend.model.dto.LoginUser;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.service.GreenChannelBatchService;
import com.example.backend.service.PolicyRuleService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigurationControllerAuthorizationTest {

    private final ICurrentUserProvider currentUsers = mock(ICurrentUserProvider.class);

    @Test
    void collegeManagerCanLoadBatchAndPolicyConfiguration() {
        when(currentUsers.getRequiredUser()).thenReturn(
                new LoginUser(7L, "college_demo", List.of("COLLEGE"), null, 3L)
        );
        GreenChannelBatchService batchService = mock(GreenChannelBatchService.class);
        PolicyRuleService policyService = mock(PolicyRuleService.class);
        when(batchService.list()).thenReturn(List.of());
        when(policyService.listAllRules()).thenReturn(List.of());

        var batchController = new GreenChannelBatchController(batchService, currentUsers);
        var policyController = new PolicyRuleController(policyService, currentUsers);

        assertDoesNotThrow(batchController::list);
        assertDoesNotThrow(policyController::list);
    }

    @Test
    void studentCannotMaintainConfiguration() {
        when(currentUsers.getRequiredUser()).thenReturn(
                new LoginUser(8L, "student_demo", List.of("STUDENT"), 12L, null)
        );
        var batchController = new GreenChannelBatchController(mock(GreenChannelBatchService.class), currentUsers);
        var policyController = new PolicyRuleController(mock(PolicyRuleService.class), currentUsers);

        assertThrows(SecurityException.class, batchController::list);
        assertThrows(SecurityException.class, policyController::list);
    }
}
