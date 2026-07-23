package com.example.backend.mapper;

import com.example.backend.model.dto.*;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.*;

/** 只访问成员二拥有的礼包库存和配额表。组织、年级、批次由服务层委托成员一查询。 */
@Mapper
public interface ApplicationResourceConfigMapper {
    @Select("SELECT b.id,b.batch_id AS batchId,b.gift_item_id AS giftItemId,g.item_name AS giftItemName," +
            "b.stock_total AS stockTotal,b.reserved_count AS reservedCount,b.used_count AS usedCount," +
            "b.per_student_limit AS perStudentLimit,b.version FROM batch_gift_item b JOIN gift_item g ON g.id=b.gift_item_id " +
            "WHERE b.batch_id=#{batchId} AND b.deleted=0 ORDER BY b.id")
    List<BatchGiftItemView> findBatchGiftItems(Long batchId);
    @Select("SELECT b.id,b.batch_id AS batchId,b.gift_item_id AS giftItemId,g.item_name AS giftItemName," +
            "b.stock_total AS stockTotal,b.reserved_count AS reservedCount,b.used_count AS usedCount," +
            "b.per_student_limit AS perStudentLimit,b.version FROM batch_gift_item b JOIN gift_item g ON g.id=b.gift_item_id " +
            "WHERE b.id=#{id} AND b.deleted=0")
    BatchGiftItemView findBatchGiftItem(Long id);
    @Select("SELECT COUNT(*) FROM batch_gift_item WHERE batch_id=#{batchId} AND gift_item_id=#{giftItemId} AND deleted=0")
    int countBatchGiftItem(Long batchId, Long giftItemId);
    @Insert("INSERT INTO batch_gift_item(batch_id,gift_item_id,stock_total,per_student_limit) VALUES(#{batchId},#{giftItemId},#{stockTotal},#{perStudentLimit})")
    int insertBatchGiftItem(@Param("batchId") Long batchId, @Param("giftItemId") Long giftItemId, @Param("stockTotal") Integer stockTotal, @Param("perStudentLimit") Integer perStudentLimit);
    @Select("SELECT LAST_INSERT_ID()") Long lastInsertId();
    @Update("UPDATE batch_gift_item SET stock_total=#{stockTotal},per_student_limit=#{perStudentLimit},version=version+1 " +
            "WHERE id=#{id} AND version=#{version} AND reserved_count<=#{stockTotal} AND deleted=0")
    int updateBatchGiftItem(@Param("id") Long id, @Param("stockTotal") Integer stockTotal, @Param("perStudentLimit") Integer perStudentLimit, @Param("version") Integer version);
    @Select("SELECT COUNT(*) FROM gift_application_item i JOIN gift_application a ON a.id=i.gift_application_id " +
            "WHERE i.batch_gift_item_id=#{id} AND i.deleted=0 AND a.deleted=0")
    int countGiftApplicationItems(Long id);
    @Update("UPDATE batch_gift_item SET deleted=id WHERE id=#{id} AND reserved_count=0 AND used_count=0 AND deleted=0")
    int deleteBatchGiftItem(Long id);

    @Select("SELECT id,batch_id AS batchId,NULL AS scope,college_id AS targetId,NULL AS targetName,quota_total AS quotaTotal,reserved_count AS reservedCount,used_count AS usedCount,version " +
            "FROM college_gift_quota WHERE batch_id=#{batchId} AND deleted=0 ORDER BY id")
    List<GiftQuotaView> findCollegeGiftQuotas(Long batchId);
    @Select("SELECT id,batch_id AS batchId,NULL AS scope,grade_id AS targetId,NULL AS targetName,quota_total AS quotaTotal,reserved_count AS reservedCount,used_count AS usedCount,version " +
            "FROM grade_gift_quota WHERE batch_id=#{batchId} AND deleted=0 ORDER BY id")
    List<GiftQuotaView> findGradeGiftQuotas(Long batchId);
    @Select("SELECT id,batch_id AS batchId,NULL AS scope,college_id AS targetId,NULL AS targetName,quota_total AS quotaTotal,reserved_count AS reservedCount,used_count AS usedCount,version FROM college_gift_quota WHERE id=#{id} AND deleted=0") GiftQuotaView findCollegeGiftQuota(Long id);
    @Select("SELECT id,batch_id AS batchId,NULL AS scope,grade_id AS targetId,NULL AS targetName,quota_total AS quotaTotal,reserved_count AS reservedCount,used_count AS usedCount,version FROM grade_gift_quota WHERE id=#{id} AND deleted=0") GiftQuotaView findGradeGiftQuota(Long id);
    @Select("SELECT COUNT(*) FROM college_gift_quota WHERE batch_id=#{batchId} AND college_id=#{targetId} AND deleted=0") int countCollegeGiftQuota(Long batchId, Long targetId);
    @Select("SELECT COUNT(*) FROM grade_gift_quota WHERE batch_id=#{batchId} AND grade_id=#{targetId} AND deleted=0") int countGradeGiftQuota(Long batchId, Long targetId);
    @Insert("INSERT INTO college_gift_quota(batch_id,college_id,quota_total) VALUES(#{batchId},#{targetId},#{quotaTotal})") int insertCollegeGiftQuota(@Param("batchId") Long batchId,@Param("targetId") Long targetId,@Param("quotaTotal") Integer quotaTotal);
    @Insert("INSERT INTO grade_gift_quota(batch_id,grade_id,quota_total) VALUES(#{batchId},#{targetId},#{quotaTotal})") int insertGradeGiftQuota(@Param("batchId") Long batchId,@Param("targetId") Long targetId,@Param("quotaTotal") Integer quotaTotal);
    @Update("UPDATE college_gift_quota SET quota_total=#{quotaTotal},version=version+1 WHERE id=#{id} AND version=#{version} AND reserved_count<=#{quotaTotal} AND deleted=0") int updateCollegeGiftQuota(@Param("id") Long id,@Param("quotaTotal") Integer quotaTotal,@Param("version") Integer version);
    @Update("UPDATE grade_gift_quota SET quota_total=#{quotaTotal},version=version+1 WHERE id=#{id} AND version=#{version} AND reserved_count<=#{quotaTotal} AND deleted=0") int updateGradeGiftQuota(@Param("id") Long id,@Param("quotaTotal") Integer quotaTotal,@Param("version") Integer version);
    @Update("UPDATE college_gift_quota SET deleted=id WHERE id=#{id} AND reserved_count=0 AND used_count=0 AND deleted=0") int deleteCollegeGiftQuota(Long id);
    @Update("UPDATE grade_gift_quota SET deleted=id WHERE id=#{id} AND reserved_count=0 AND used_count=0 AND deleted=0") int deleteGradeGiftQuota(Long id);

    @Select("SELECT id,batch_id AS batchId,NULL AS scope,college_id AS targetId,NULL AS targetName,quota_amount AS quotaAmount,reserved_amount AS reservedAmount,used_amount AS usedAmount,version FROM college_subsidy_quota WHERE batch_id=#{batchId} AND deleted=0 ORDER BY id") List<SubsidyQuotaView> findCollegeSubsidyQuotas(Long batchId);
    @Select("SELECT id,batch_id AS batchId,NULL AS scope,grade_id AS targetId,NULL AS targetName,quota_amount AS quotaAmount,reserved_amount AS reservedAmount,used_amount AS usedAmount,version FROM grade_subsidy_quota WHERE batch_id=#{batchId} AND deleted=0 ORDER BY id") List<SubsidyQuotaView> findGradeSubsidyQuotas(Long batchId);
    @Select("SELECT id,batch_id AS batchId,NULL AS scope,college_id AS targetId,NULL AS targetName,quota_amount AS quotaAmount,reserved_amount AS reservedAmount,used_amount AS usedAmount,version FROM college_subsidy_quota WHERE id=#{id} AND deleted=0") SubsidyQuotaView findCollegeSubsidyQuota(Long id);
    @Select("SELECT id,batch_id AS batchId,NULL AS scope,grade_id AS targetId,NULL AS targetName,quota_amount AS quotaAmount,reserved_amount AS reservedAmount,used_amount AS usedAmount,version FROM grade_subsidy_quota WHERE id=#{id} AND deleted=0") SubsidyQuotaView findGradeSubsidyQuota(Long id);
    @Select("SELECT COUNT(*) FROM college_subsidy_quota WHERE batch_id=#{batchId} AND college_id=#{targetId} AND deleted=0") int countCollegeSubsidyQuota(Long batchId, Long targetId);
    @Select("SELECT COUNT(*) FROM grade_subsidy_quota WHERE batch_id=#{batchId} AND grade_id=#{targetId} AND deleted=0") int countGradeSubsidyQuota(Long batchId, Long targetId);
    @Insert("INSERT INTO college_subsidy_quota(batch_id,college_id,quota_amount) VALUES(#{batchId},#{targetId},#{quotaAmount})") int insertCollegeSubsidyQuota(@Param("batchId") Long batchId,@Param("targetId") Long targetId,@Param("quotaAmount") BigDecimal quotaAmount);
    @Insert("INSERT INTO grade_subsidy_quota(batch_id,grade_id,quota_amount) VALUES(#{batchId},#{targetId},#{quotaAmount})") int insertGradeSubsidyQuota(@Param("batchId") Long batchId,@Param("targetId") Long targetId,@Param("quotaAmount") BigDecimal quotaAmount);
    @Update("UPDATE college_subsidy_quota SET quota_amount=#{quotaAmount},version=version+1 WHERE id=#{id} AND version=#{version} AND reserved_amount<=#{quotaAmount} AND deleted=0") int updateCollegeSubsidyQuota(@Param("id") Long id,@Param("quotaAmount") BigDecimal quotaAmount,@Param("version") Integer version);
    @Update("UPDATE grade_subsidy_quota SET quota_amount=#{quotaAmount},version=version+1 WHERE id=#{id} AND version=#{version} AND reserved_amount<=#{quotaAmount} AND deleted=0") int updateGradeSubsidyQuota(@Param("id") Long id,@Param("quotaAmount") BigDecimal quotaAmount,@Param("version") Integer version);
    @Update("UPDATE college_subsidy_quota SET deleted=id WHERE id=#{id} AND reserved_amount=0 AND used_amount=0 AND deleted=0") int deleteCollegeSubsidyQuota(Long id);
    @Update("UPDATE grade_subsidy_quota SET deleted=id WHERE id=#{id} AND reserved_amount=0 AND used_amount=0 AND deleted=0") int deleteGradeSubsidyQuota(Long id);
}
