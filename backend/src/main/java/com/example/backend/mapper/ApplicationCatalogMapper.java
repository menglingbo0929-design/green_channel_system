package com.example.backend.mapper;

import com.example.backend.model.dto.CatalogItemView;
import com.example.backend.model.dto.FeeAmountOptionView;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 成员二拥有的费用/礼包基础配置表访问层。
 * 不读取学生、组织、批次等其他成员维护的数据。
 */
@Mapper
public interface ApplicationCatalogMapper {
    @Select("SELECT id, item_name AS name, enabled FROM fee_item WHERE deleted=0 "
            + "AND (#{includeDisabled}=true OR enabled=1) ORDER BY id")
    List<CatalogItemView> findFeeItems(@Param("includeDisabled") boolean includeDisabled);

    @Select("SELECT id, item_name AS name, enabled FROM fee_item WHERE id=#{id} AND deleted=0")
    CatalogItemView findFeeItem(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM fee_item WHERE item_name=#{name} AND deleted=0 "
            + "AND (#{excludedId} IS NULL OR id<>#{excludedId})")
    int countFeeItemsByName(@Param("name") String name, @Param("excludedId") Long excludedId);

    @Insert("INSERT INTO fee_item(item_name, enabled) VALUES(#{name}, #{enabled})")
    int insertFeeItem(@Param("name") String name, @Param("enabled") boolean enabled);

    @Select("SELECT LAST_INSERT_ID()")
    Long lastInsertId();

    @Update("UPDATE fee_item SET item_name=#{name}, enabled=#{enabled} WHERE id=#{id} AND deleted=0")
    int updateFeeItem(@Param("id") Long id, @Param("name") String name, @Param("enabled") boolean enabled);

    @Select("SELECT COUNT(*) FROM arrears_application WHERE fee_item_id=#{id} AND deleted=0")
    int countActiveArrearsByFeeItemId(@Param("id") Long id);

    @Update("UPDATE fee_item SET deleted=id WHERE id=#{id} AND deleted=0")
    int deleteFeeItem(@Param("id") Long id);

    @Select("SELECT id, fee_item_id AS feeItemId, amount, enabled FROM fee_amount_option "
            + "WHERE deleted=0 AND (#{feeItemId} IS NULL OR fee_item_id=#{feeItemId}) "
            + "AND (#{includeDisabled}=true OR enabled=1) ORDER BY fee_item_id, amount, id")
    List<FeeAmountOptionView> findFeeAmountOptions(@Param("feeItemId") Long feeItemId,
                                                    @Param("includeDisabled") boolean includeDisabled);

    @Select("SELECT id, fee_item_id AS feeItemId, amount, enabled FROM fee_amount_option WHERE id=#{id} AND deleted=0")
    FeeAmountOptionView findFeeAmountOption(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM fee_amount_option WHERE fee_item_id=#{feeItemId} AND amount=#{amount} "
            + "AND deleted=0 AND (#{excludedId} IS NULL OR id<>#{excludedId})")
    int countFeeAmountOptions(@Param("feeItemId") Long feeItemId, @Param("amount") BigDecimal amount,
                              @Param("excludedId") Long excludedId);

    @Insert("INSERT INTO fee_amount_option(fee_item_id, amount, enabled) VALUES(#{feeItemId}, #{amount}, #{enabled})")
    int insertFeeAmountOption(@Param("feeItemId") Long feeItemId, @Param("amount") BigDecimal amount,
                              @Param("enabled") boolean enabled);

    @Update("UPDATE fee_amount_option SET fee_item_id=#{feeItemId}, amount=#{amount}, enabled=#{enabled} "
            + "WHERE id=#{id} AND deleted=0")
    int updateFeeAmountOption(@Param("id") Long id, @Param("feeItemId") Long feeItemId,
                              @Param("amount") BigDecimal amount, @Param("enabled") boolean enabled);

    @Update("UPDATE fee_amount_option SET deleted=id WHERE id=#{id} AND deleted=0")
    int deleteFeeAmountOption(@Param("id") Long id);

    @Update("UPDATE fee_amount_option SET fee_item_id=#{targetFeeItemId} WHERE id=#{id} AND deleted=0")
    int moveFeeAmountOption(@Param("id") Long id, @Param("targetFeeItemId") Long targetFeeItemId);

    @Select("SELECT id, item_name AS name, enabled, image_url AS imageUrl, item_type AS itemType, item_size AS itemSize, description, unit_price AS unitPrice, gender_restriction AS genderRestriction, required_flag AS required FROM gift_item WHERE deleted=0 "
            + "AND (#{includeDisabled}=true OR enabled=1) ORDER BY id")
    List<com.example.backend.model.dto.GiftItemView> findGiftItems(@Param("includeDisabled") boolean includeDisabled);

    @Select("SELECT id, item_name AS name, enabled, image_url AS imageUrl, item_type AS itemType, item_size AS itemSize, description, unit_price AS unitPrice, gender_restriction AS genderRestriction, required_flag AS required FROM gift_item WHERE id=#{id} AND deleted=0")
    com.example.backend.model.dto.GiftItemView findGiftItem(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM gift_item WHERE item_name=#{name} AND deleted=0 "
            + "AND (#{excludedId} IS NULL OR id<>#{excludedId})")
    int countGiftItemsByName(@Param("name") String name, @Param("excludedId") Long excludedId);

    @Insert("INSERT INTO gift_item(item_name, enabled, image_url, item_type, item_size, description, unit_price, gender_restriction, required_flag) VALUES(#{name}, #{enabled}, #{imageUrl}, #{itemType}, #{itemSize}, #{description}, #{unitPrice}, #{genderRestriction}, #{required})")
    int insertGiftItem(@Param("name") String name, @Param("enabled") boolean enabled, @Param("imageUrl") String imageUrl, @Param("itemType") String itemType, @Param("itemSize") String itemSize, @Param("description") String description, @Param("unitPrice") BigDecimal unitPrice, @Param("genderRestriction") String genderRestriction, @Param("required") boolean required);

    @Update("UPDATE gift_item SET item_name=#{name}, enabled=#{enabled}, image_url=#{imageUrl}, item_type=#{itemType}, item_size=#{itemSize}, description=#{description}, unit_price=#{unitPrice}, gender_restriction=#{genderRestriction}, required_flag=#{required} WHERE id=#{id} AND deleted=0")
    int updateGiftItem(@Param("id") Long id, @Param("name") String name, @Param("enabled") boolean enabled, @Param("imageUrl") String imageUrl, @Param("itemType") String itemType, @Param("itemSize") String itemSize, @Param("description") String description, @Param("unitPrice") BigDecimal unitPrice, @Param("genderRestriction") String genderRestriction, @Param("required") boolean required);

    @Select("SELECT COUNT(*) FROM batch_gift_item WHERE gift_item_id=#{id} AND deleted=0")
    int countActiveBatchGiftItemsByGiftItemId(@Param("id") Long id);

    @Update("UPDATE gift_item SET deleted=id WHERE id=#{id} AND deleted=0")
    int deleteGiftItem(@Param("id") Long id);
}
