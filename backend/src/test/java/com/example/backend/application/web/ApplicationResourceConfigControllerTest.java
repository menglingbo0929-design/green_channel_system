package com.example.backend.application.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.application.domain.QuotaScope;
import com.example.backend.application.dto.BatchGiftItemView;
import com.example.backend.application.service.ApplicationResourceConfigService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ApplicationResourceConfigControllerTest {
    @Test
    void exposesBatchGiftItemsForSpecifiedBatch() throws Exception {
        ApplicationResourceConfigService service = mock(ApplicationResourceConfigService.class);
        when(service.findBatchGiftItems(6L)).thenReturn(List.of(new BatchGiftItemView(1L, 6L, 2L, "书包", 10, 0, 0, 1, 0)));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ApplicationResourceConfigController(service)).build();

        mvc.perform(get("/api/application-resources/batch-gift-items").param("batchId", "6"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].giftItemName").value("书包"));
        verify(service).findBatchGiftItems(6L);
    }

    @Test
    void rejectsMissingQuotaScopeAtControllerBoundary() throws Exception {
        ApplicationResourceConfigService service = mock(ApplicationResourceConfigService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ApplicationResourceConfigController(service)).build();
        mvc.perform(get("/api/application-resources/gift-quotas").param("batchId", "6"))
                .andExpect(status().isBadRequest());
        verify(service, never()).findGiftQuotas(anyLong(), any(QuotaScope.class));
    }
}
