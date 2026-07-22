package com.example.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.application.dto.CatalogItemCommand;
import com.example.backend.application.dto.CatalogItemView;
import com.example.backend.application.dto.FeeAmountOptionCommand;
import com.example.backend.application.dto.FeeAmountOptionView;
import com.example.backend.application.exception.ApplicationException;
import com.example.backend.application.mapper.ApplicationCatalogMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class ApplicationCatalogServiceTest {
    private final ApplicationCatalogMapper mapper = org.mockito.Mockito.mock(ApplicationCatalogMapper.class);
    private final ApplicationCatalogService service = new ApplicationCatalogService(mapper);

    @Test
    void createsFeeItemAndReturnsTheGeneratedRecord() {
        when(mapper.countFeeItemsByName("学费", null)).thenReturn(0);
        when(mapper.lastInsertId()).thenReturn(11L);
        when(mapper.findFeeItem(11L)).thenReturn(new CatalogItemView(11L, "学费", true));

        CatalogItemView result = service.createFeeItem(new CatalogItemCommand("  学费  ", true));

        assertEquals(11L, result.id());
        verify(mapper).insertFeeItem("学费", true);
    }

    @Test
    void rejectsDuplicateFeeItemNameBeforeWriting() {
        when(mapper.countFeeItemsByName("学费", null)).thenReturn(1);

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> service.createFeeItem(new CatalogItemCommand("学费", true)));

        assertEquals("FEE_ITEM_NAME_EXISTS", exception.getCode());
        verify(mapper, never()).insertFeeItem(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean());
    }

    @Test
    void rejectsAmountOptionForDisabledFeeItem() {
        when(mapper.findFeeItem(3L)).thenReturn(new CatalogItemView(3L, "住宿费", false));

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> service.createFeeAmountOption(new FeeAmountOptionCommand(3L, new BigDecimal("1000"), true)));

        assertEquals("FEE_ITEM_DISABLED", exception.getCode());
        verify(mapper, never()).insertFeeAmountOption(ArgumentMatchers.anyLong(), ArgumentMatchers.any(), ArgumentMatchers.anyBoolean());
    }

    @Test
    void refusesToDeleteFeeItemReferencedByAnApplication() {
        when(mapper.findFeeItem(7L)).thenReturn(new CatalogItemView(7L, "教材费", true));
        when(mapper.countActiveArrearsByFeeItemId(7L)).thenReturn(1);

        ApplicationException exception = assertThrows(ApplicationException.class, () -> service.deleteFeeItem(7L));

        assertEquals("FEE_ITEM_IN_USE", exception.getCode());
        verify(mapper, never()).deleteFeeItem(7L);
    }

    @Test
    void refusesToDeleteGiftItemReferencedByBatchConfiguration() {
        when(mapper.findGiftItem(9L)).thenReturn(new CatalogItemView(9L, "被子", true));
        when(mapper.countActiveBatchGiftItemsByGiftItemId(9L)).thenReturn(1);

        ApplicationException exception = assertThrows(ApplicationException.class, () -> service.deleteGiftItem(9L));

        assertEquals("GIFT_ITEM_IN_USE", exception.getCode());
        verify(mapper, never()).deleteGiftItem(9L);
    }

    @Test
    void mergesUnusedFeeItemAndMovesItsUniqueAmountOptions() {
        when(mapper.findFeeItem(7L)).thenReturn(new CatalogItemView(7L, "tuition", true));
        when(mapper.findFeeItem(8L)).thenReturn(new CatalogItemView(8L, "2026 tuition", true));
        when(mapper.countActiveArrearsByFeeItemId(7L)).thenReturn(0);
        when(mapper.findFeeAmountOptions(8L, true)).thenReturn(List.of(
                new FeeAmountOptionView(20L, 8L, new BigDecimal("1000"), true)
        ));
        when(mapper.findFeeAmountOptions(7L, true)).thenReturn(List.of(
                new FeeAmountOptionView(10L, 7L, new BigDecimal("1000"), true),
                new FeeAmountOptionView(11L, 7L, new BigDecimal("1200"), true)
        ));
        when(mapper.deleteFeeItem(7L)).thenReturn(1);

        CatalogItemView result = service.mergeFeeItem(7L, 8L);

        assertEquals(8L, result.id());
        verify(mapper).deleteFeeAmountOption(10L);
        verify(mapper).moveFeeAmountOption(11L, 8L);
        verify(mapper).deleteFeeItem(7L);
    }

    @Test
    void createsAmountOptionForEnabledFeeItem() {
        when(mapper.findFeeItem(3L)).thenReturn(new CatalogItemView(3L, "住宿费", true));
        when(mapper.countFeeAmountOptions(3L, new BigDecimal("1200.00"), null)).thenReturn(0);
        when(mapper.lastInsertId()).thenReturn(20L);
        when(mapper.findFeeAmountOption(20L)).thenReturn(new FeeAmountOptionView(20L, 3L, new BigDecimal("1200.00"), true));

        FeeAmountOptionView result = service.createFeeAmountOption(
                new FeeAmountOptionCommand(3L, new BigDecimal("1200.00"), true));

        assertEquals(20L, result.id());
        verify(mapper).insertFeeAmountOption(3L, new BigDecimal("1200.00"), true);
    }
}
