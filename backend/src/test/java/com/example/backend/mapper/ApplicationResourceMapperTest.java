package com.example.backend.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.backend.mapper.ApplicationResourceConfigMapper;
import com.example.backend.mapper.ApplicationResourceMapper;
import com.example.backend.mapper.ArrearsApplicationMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ApplicationResourceMapperTest {
    @Autowired private ApplicationResourceMapper resourceMapper;
    @Autowired private ApplicationResourceConfigMapper configMapper;
    @Autowired private ArrearsApplicationMapper arrearsMapper;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void storesAndReadsGiftAndSubsidyDetails() {
        jdbc.update("INSERT INTO gift_item(item_name,enabled) VALUES ('棉被',TRUE)");
        Long itemId = jdbc.queryForObject("SELECT MAX(id) FROM gift_item", Long.class);
        jdbc.update("INSERT INTO batch_gift_item(batch_id,gift_item_id,stock_total,per_student_limit) VALUES (7,?,20,2)", itemId);
        Long batchGiftItemId = jdbc.queryForObject("SELECT MAX(id) FROM batch_gift_item", Long.class);
        assertEquals(1, resourceMapper.insertGiftApplication(101L));
        Long giftApplicationId = resourceMapper.findGiftApplicationId(101L);
        assertEquals(1, resourceMapper.insertGiftItem(giftApplicationId, batchGiftItemId, 2));
        assertEquals("棉被", resourceMapper.findGiftItems(101L).getFirst().itemName());
        assertEquals(2, resourceMapper.findGiftItems(101L).getFirst().quantity());

        assertEquals(1, resourceMapper.insertSubsidy(102L, new BigDecimal("500.00")));
        assertEquals(new BigDecimal("500.00"), resourceMapper.findSubsidy(102L).expectedAmount());
        assertEquals(1, resourceMapper.updateSubsidy(102L, new BigDecimal("650.00")));
        assertEquals(new BigDecimal("650.00"), resourceMapper.findSubsidy(102L).expectedAmount());
        assertNull(resourceMapper.findSubsidy(999L));
    }

    @Test
    void readsAndOptimisticallyUpdatesResourceConfiguration() {
        jdbc.update("INSERT INTO gift_item(item_name,enabled) VALUES ('书包',TRUE)");
        Long giftItemId = jdbc.queryForObject("SELECT MAX(id) FROM gift_item", Long.class);
        assertEquals(1, configMapper.insertBatchGiftItem(8L, giftItemId, 10, 1));
        Long batchGiftId = jdbc.queryForObject("SELECT MAX(id) FROM batch_gift_item", Long.class);
        assertEquals(1, configMapper.updateBatchGiftItem(batchGiftId, 12, 2, 0));
        assertEquals(2, configMapper.findBatchGiftItems(8L).getFirst().perStudentLimit());
        assertEquals(0, configMapper.updateBatchGiftItem(batchGiftId, 15, 2, 0));

        assertEquals(1, configMapper.insertCollegeGiftQuota(8L, 11L, 20));
        assertEquals(1, configMapper.findCollegeGiftQuotas(8L).size());
        assertEquals(1, configMapper.insertGradeSubsidyQuota(8L, 22L, new BigDecimal("1000.00")));
        assertEquals(new BigDecimal("1000.00"), configMapper.findGradeSubsidyQuotas(8L).getFirst().quotaAmount());
    }

    @Test
    void storesTheFixedArrearsReasonCodeInH2() {
        jdbc.update("INSERT INTO fee_item(item_name,enabled) VALUES ('学费',TRUE)");
        Long feeItemId = jdbc.queryForObject("SELECT MAX(id) FROM fee_item", Long.class);

        assertEquals(1, arrearsMapper.insert(55L, feeItemId, new BigDecimal("800.00"), "MAJOR_ILLNESS"));
        assertEquals("MAJOR_ILLNESS", jdbc.queryForObject(
                "SELECT arrears_reason_code FROM arrears_application WHERE application_id=55", String.class));
        assertEquals("MAJOR_ILLNESS", arrearsMapper.findItemsByApplicationId(55L).getFirst().arrearsReasonCode());
    }
}
