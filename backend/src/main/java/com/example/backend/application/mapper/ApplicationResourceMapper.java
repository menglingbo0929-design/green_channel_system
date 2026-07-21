package com.example.backend.application.mapper;

import com.example.backend.application.dto.GiftApplicationItemSnapshot;
import com.example.backend.application.dto.SubsidyApplicationSnapshot;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ApplicationResourceMapper {
    @Select("SELECT i.batch_gift_item_id AS batchGiftItemId, g.item_name AS itemName, i.quantity " +
            "FROM gift_application_item i JOIN gift_application a ON a.id=i.gift_application_id " +
            "JOIN batch_gift_item b ON b.id=i.batch_gift_item_id JOIN gift_item g ON g.id=b.gift_item_id " +
            "WHERE a.application_id=#{applicationId} AND a.deleted=0 AND i.deleted=0 ORDER BY i.id")
    List<GiftApplicationItemSnapshot> findGiftItems(Long applicationId);

    @Select("SELECT id FROM gift_application WHERE application_id=#{applicationId} AND deleted=0")
    Long findGiftApplicationId(Long applicationId);

    @Insert("INSERT INTO gift_application(application_id) VALUES(#{applicationId})")
    int insertGiftApplication(Long applicationId);

    @Select("SELECT LAST_INSERT_ID()")
    Long lastInsertId();

    @Update("UPDATE gift_application_item SET deleted=id WHERE gift_application_id=#{giftApplicationId} AND deleted=0")
    int deleteGiftItems(Long giftApplicationId);

    @Insert("INSERT INTO gift_application_item(gift_application_id,batch_gift_item_id,quantity) VALUES(#{giftApplicationId},#{batchGiftItemId},#{quantity})")
    int insertGiftItem(@Param("giftApplicationId") Long giftApplicationId, @Param("batchGiftItemId") Long batchGiftItemId, @Param("quantity") Integer quantity);

    @Select("SELECT COUNT(*) FROM batch_gift_item WHERE id=#{id} AND batch_id=#{batchId} AND deleted=0 " +
            "AND per_student_limit>=#{quantity} AND stock_total-reserved_count>=#{quantity}")
    int countValidBatchGiftItem(@Param("id") Long id, @Param("batchId") Long batchId, @Param("quantity") Integer quantity);
    @Select("SELECT id FROM batch_gift_item WHERE batch_id=#{batchId} AND gift_item_id=#{giftItemId} AND deleted=0 LIMIT 1")
    Long findBatchGiftItemId(@Param("batchId") Long batchId, @Param("giftItemId") Long giftItemId);

    @Select("SELECT expected_amount AS expectedAmount, final_amount AS finalAmount FROM subsidy_application WHERE application_id=#{applicationId} AND deleted=0")
    SubsidyApplicationSnapshot findSubsidy(Long applicationId);

    @Insert("INSERT INTO subsidy_application(application_id,expected_amount) VALUES(#{applicationId},#{expectedAmount})")
    int insertSubsidy(@Param("applicationId") Long applicationId, @Param("expectedAmount") BigDecimal expectedAmount);

    @Update("UPDATE subsidy_application SET expected_amount=#{expectedAmount}, final_amount=NULL WHERE application_id=#{applicationId} AND deleted=0")
    int updateSubsidy(@Param("applicationId") Long applicationId, @Param("expectedAmount") BigDecimal expectedAmount);
}
