package com.example.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.backend.application.domain.QuotaScope;
import com.example.backend.application.dto.BatchGiftItemCommand;
import com.example.backend.application.dto.GiftQuotaCommand;
import com.example.backend.application.dto.GiftItemView;
import com.example.backend.application.exception.ApplicationException;
import com.example.backend.application.mapper.ApplicationCatalogMapper;
import com.example.backend.application.mapper.ApplicationResourceConfigMapper;
import com.example.backend.model.dto.CollegeOption;
import com.example.backend.service.BatchQueryService;
import com.example.backend.service.OrganizationQueryService;
import java.util.List;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ApplicationResourceConfigServiceTest {
    private final ApplicationResourceConfigMapper mapper = mock(ApplicationResourceConfigMapper.class);
    private final ApplicationCatalogMapper catalogMapper = mock(ApplicationCatalogMapper.class);
    private final BatchQueryService batches = mock(BatchQueryService.class);
    private final OrganizationQueryService organizations = mock(OrganizationQueryService.class);
    private final ApplicationResourceConfigService service = new ApplicationResourceConfigService(mapper, catalogMapper, batches, organizations);

    @Test
    void rejectsDuplicateBatchGiftItemBeforeInsert() {
        when(catalogMapper.findGiftItem(2L)).thenReturn(new GiftItemView(2L, "书包", true, null, null, null, null, BigDecimal.ZERO, "ALL", false));
        when(mapper.countBatchGiftItem(1L, 2L)).thenReturn(1);
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> service.createBatchGiftItem(new BatchGiftItemCommand(1L, 2L, 10, 1)));
        assertEquals("BATCH_GIFT_ITEM_EXISTS", exception.getCode());
        verify(mapper, never()).insertBatchGiftItem(any(), any(), any(), any());
    }

    @Test
    void validatesCollegeThroughOrganizationServiceBeforeCreatingQuota() {
        CollegeOption college = CollegeOption.builder().id(8L).collegeName("计算机学院").build();
        when(organizations.listColleges()).thenReturn(List.of(college));
        when(mapper.countCollegeGiftQuota(3L, 8L)).thenReturn(0);
        when(mapper.lastInsertId()).thenReturn(9L);
        when(mapper.findCollegeGiftQuota(9L)).thenReturn(new com.example.backend.application.dto.GiftQuotaView(9L, 3L, null, 8L, null, 10, 0, 0, 0));

        var result = service.createGiftQuota(new GiftQuotaCommand(3L, QuotaScope.COLLEGE, 8L, 10));

        assertEquals("计算机学院", result.targetName());
        verify(mapper).insertCollegeGiftQuota(3L, 8L, 10);
        verifyNoInteractions(catalogMapper);
    }
}
