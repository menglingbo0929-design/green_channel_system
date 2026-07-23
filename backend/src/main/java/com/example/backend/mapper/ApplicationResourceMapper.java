package com.example.backend.mapper;

import com.example.backend.model.dto.GiftApplicationItemSnapshot;
import com.example.backend.model.dto.SubsidyApplicationSnapshot;
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
    @Update("UPDATE subsidy_application SET final_amount=#{finalAmount} WHERE application_id=#{applicationId} AND deleted=0")
    int updateSubsidyFinalAmount(@Param("applicationId") Long applicationId, @Param("finalAmount") BigDecimal finalAmount);

    @Select("SELECT COUNT(*) FROM application_attachment WHERE application_id=#{applicationId} AND deleted=0")
    int countActiveAttachments(Long applicationId);

    @Insert("INSERT INTO application_attachment(application_id,file_id,original_filename,content_type,file_size) VALUES(#{applicationId},#{fileId},#{originalFilename},#{contentType},#{fileSize})")
    int insertAttachment(@Param("applicationId") Long applicationId, @Param("fileId") String fileId,
                         @Param("originalFilename") String originalFilename, @Param("contentType") String contentType,
                         @Param("fileSize") long fileSize);
    @Select("SELECT id, application_id AS applicationId, file_id AS fileId, original_filename AS originalFilename, content_type AS contentType, file_size AS fileSize FROM application_attachment WHERE id=#{attachmentId} AND application_id=#{applicationId} AND deleted=0")
    com.example.backend.model.dto.ApplicationAttachmentSnapshot findAttachment(@Param("applicationId") Long applicationId, @Param("attachmentId") Long attachmentId);

    @Update("UPDATE batch_gift_item SET reserved_count=reserved_count+#{quantity} WHERE id=#{batchGiftItemId} AND deleted=0 AND stock_total-reserved_count>=#{quantity}")
    int reserveGiftStock(@Param("batchGiftItemId") Long batchGiftItemId, @Param("quantity") int quantity);
    @Update("UPDATE college_gift_quota SET reserved_count=reserved_count+#{quantity} WHERE batch_id=#{batchId} AND college_id=#{collegeId} AND deleted=0 AND quota_total-reserved_count>=#{quantity}")
    int reserveCollegeGiftQuota(@Param("batchId") Long batchId, @Param("collegeId") Long collegeId, @Param("quantity") int quantity);
    @Update("UPDATE grade_gift_quota SET reserved_count=reserved_count+#{quantity} WHERE batch_id=#{batchId} AND grade_id=#{gradeId} AND deleted=0 AND quota_total-reserved_count>=#{quantity}")
    int reserveGradeGiftQuota(@Param("batchId") Long batchId, @Param("gradeId") Long gradeId, @Param("quantity") int quantity);
    @Update("UPDATE batch_gift_item SET reserved_count=reserved_count-#{quantity} WHERE id=#{batchGiftItemId} AND deleted=0 AND reserved_count>=#{quantity}")
    int releaseGiftStockReservation(@Param("batchGiftItemId") Long batchGiftItemId, @Param("quantity") int quantity);
    @Update("UPDATE college_gift_quota SET reserved_count=reserved_count-#{quantity} WHERE batch_id=#{batchId} AND college_id=#{collegeId} AND deleted=0 AND reserved_count>=#{quantity}")
    int releaseCollegeGiftQuotaReservation(@Param("batchId") Long batchId, @Param("collegeId") Long collegeId, @Param("quantity") int quantity);
    @Update("UPDATE grade_gift_quota SET reserved_count=reserved_count-#{quantity} WHERE batch_id=#{batchId} AND grade_id=#{gradeId} AND deleted=0 AND reserved_count>=#{quantity}")
    int releaseGradeGiftQuotaReservation(@Param("batchId") Long batchId, @Param("gradeId") Long gradeId, @Param("quantity") int quantity);
    @Update("UPDATE batch_gift_item SET reserved_count=reserved_count-#{quantity}, used_count=used_count+#{quantity} WHERE id=#{batchGiftItemId} AND deleted=0 AND reserved_count>=#{quantity}")
    int confirmGiftStock(@Param("batchGiftItemId") Long batchGiftItemId, @Param("quantity") int quantity);
    @Update("UPDATE college_gift_quota SET reserved_count=reserved_count-#{quantity}, used_count=used_count+#{quantity} WHERE batch_id=#{batchId} AND college_id=#{collegeId} AND deleted=0 AND reserved_count>=#{quantity}")
    int confirmCollegeGiftQuota(@Param("batchId") Long batchId, @Param("collegeId") Long collegeId, @Param("quantity") int quantity);
    @Update("UPDATE grade_gift_quota SET reserved_count=reserved_count-#{quantity}, used_count=used_count+#{quantity} WHERE batch_id=#{batchId} AND grade_id=#{gradeId} AND deleted=0 AND reserved_count>=#{quantity}")
    int confirmGradeGiftQuota(@Param("batchId") Long batchId, @Param("gradeId") Long gradeId, @Param("quantity") int quantity);
    @Update("UPDATE batch_gift_item SET used_count=used_count-#{quantity} WHERE id=#{batchGiftItemId} AND deleted=0 AND used_count>=#{quantity}")
    int releaseGiftStockUsage(@Param("batchGiftItemId") Long batchGiftItemId, @Param("quantity") int quantity);
    @Update("UPDATE college_gift_quota SET used_count=used_count-#{quantity} WHERE batch_id=#{batchId} AND college_id=#{collegeId} AND deleted=0 AND used_count>=#{quantity}")
    int releaseCollegeGiftQuotaUsage(@Param("batchId") Long batchId, @Param("collegeId") Long collegeId, @Param("quantity") int quantity);
    @Update("UPDATE grade_gift_quota SET used_count=used_count-#{quantity} WHERE batch_id=#{batchId} AND grade_id=#{gradeId} AND deleted=0 AND used_count>=#{quantity}")
    int releaseGradeGiftQuotaUsage(@Param("batchId") Long batchId, @Param("gradeId") Long gradeId, @Param("quantity") int quantity);

    @Update("UPDATE college_subsidy_quota SET reserved_amount=reserved_amount+#{amount} WHERE batch_id=#{batchId} AND college_id=#{collegeId} AND deleted=0 AND quota_amount-reserved_amount>=#{amount}")
    int reserveCollegeSubsidyQuota(@Param("batchId") Long batchId, @Param("collegeId") Long collegeId, @Param("amount") BigDecimal amount);
    @Update("UPDATE grade_subsidy_quota SET reserved_amount=reserved_amount+#{amount} WHERE batch_id=#{batchId} AND grade_id=#{gradeId} AND deleted=0 AND quota_amount-reserved_amount>=#{amount}")
    int reserveGradeSubsidyQuota(@Param("batchId") Long batchId, @Param("gradeId") Long gradeId, @Param("amount") BigDecimal amount);
    @Update("UPDATE college_subsidy_quota SET reserved_amount=reserved_amount-#{amount} WHERE batch_id=#{batchId} AND college_id=#{collegeId} AND deleted=0 AND reserved_amount>=#{amount}")
    int releaseCollegeSubsidyReservation(@Param("batchId") Long batchId, @Param("collegeId") Long collegeId, @Param("amount") BigDecimal amount);
    @Update("UPDATE grade_subsidy_quota SET reserved_amount=reserved_amount-#{amount} WHERE batch_id=#{batchId} AND grade_id=#{gradeId} AND deleted=0 AND reserved_amount>=#{amount}")
    int releaseGradeSubsidyReservation(@Param("batchId") Long batchId, @Param("gradeId") Long gradeId, @Param("amount") BigDecimal amount);
    @Update("UPDATE college_subsidy_quota SET reserved_amount=reserved_amount-#{amount}, used_amount=used_amount+#{amount} WHERE batch_id=#{batchId} AND college_id=#{collegeId} AND deleted=0 AND reserved_amount>=#{amount}")
    int confirmCollegeSubsidyQuota(@Param("batchId") Long batchId, @Param("collegeId") Long collegeId, @Param("amount") BigDecimal amount);
    @Update("UPDATE grade_subsidy_quota SET reserved_amount=reserved_amount-#{amount}, used_amount=used_amount+#{amount} WHERE batch_id=#{batchId} AND grade_id=#{gradeId} AND deleted=0 AND reserved_amount>=#{amount}")
    int confirmGradeSubsidyQuota(@Param("batchId") Long batchId, @Param("gradeId") Long gradeId, @Param("amount") BigDecimal amount);
    @Update("UPDATE college_subsidy_quota SET used_amount=used_amount-#{amount} WHERE batch_id=#{batchId} AND college_id=#{collegeId} AND deleted=0 AND used_amount>=#{amount}")
    int releaseCollegeSubsidyUsage(@Param("batchId") Long batchId, @Param("collegeId") Long collegeId, @Param("amount") BigDecimal amount);
    @Update("UPDATE grade_subsidy_quota SET used_amount=used_amount-#{amount} WHERE batch_id=#{batchId} AND grade_id=#{gradeId} AND deleted=0 AND used_amount>=#{amount}")
    int releaseGradeSubsidyUsage(@Param("batchId") Long batchId, @Param("gradeId") Long gradeId, @Param("amount") BigDecimal amount);
}
