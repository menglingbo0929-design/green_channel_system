package com.example.backend.application.mapper;

import com.example.backend.application.domain.*;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ApplicationMapper {
    @Insert("INSERT INTO application(application_no,student_id,application_type,source,batch_type,green_channel_batch_id,subsidy_batch_id,status,current_level,review_round,version,application_reason,create_by,update_by) " +
            "VALUES(#{applicationNo},#{studentId},#{applicationType},#{source},#{batchType},#{greenChannelBatchId},#{subsidyBatchId},#{status},#{currentLevel},#{reviewRound},#{version},#{applicationReason},#{createBy},#{updateBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id") int insert(Application application);

    @Select("SELECT * FROM application WHERE id=#{id} AND deleted=0") Application findRequired(Long id);
    @Select("SELECT * FROM application WHERE student_id=#{studentId} AND application_type=#{type} AND deleted=0 AND ((#{batchType}='GREEN_CHANNEL' AND green_channel_batch_id=#{batchId}) OR (#{batchType}='SUBSIDY' AND subsidy_batch_id=#{batchId})) LIMIT 1")
    Application findActiveByUnique(Long studentId, ApplicationType type, BatchType batchType, Long batchId);
    @Select("SELECT * FROM application WHERE student_id=#{studentId} AND deleted=0 ORDER BY create_time DESC") List<Application> findMine(Long studentId);
    @Update("UPDATE application SET application_reason=#{reason}, version=version+1, update_by=#{operatorId} WHERE id=#{id} AND version=#{expectedVersion} AND status IN ('DRAFT','COUNSELOR_RETURNED','COLLEGE_RETURNED','SCHOOL_RETURNED') AND deleted=0")
    int updateDraft(Long id, String reason, Integer expectedVersion, Long operatorId);
    @Update("UPDATE application SET deleted=id, update_by=#{operatorId} WHERE id=#{id} AND status='DRAFT' AND version=#{expectedVersion} AND deleted=0")
    int deleteDraft(Long id, Integer expectedVersion, Long operatorId);
    @Update("UPDATE application SET status=#{targetStatus},current_level=#{targetLevel},version=version+1,update_by=#{operatorId} WHERE id=#{id} AND status=#{expectedStatus} AND version=#{expectedVersion} AND deleted=0")
    int updateState(Long id, ApplicationStatus expectedStatus, ApplicationStatus targetStatus, ApprovalLevel targetLevel, Integer expectedVersion, Long operatorId);
    @Update("UPDATE application SET status=#{targetStatus},current_level=#{targetLevel},review_round=review_round+1,version=version+1,update_by=#{operatorId} WHERE id=#{id} AND status=#{expectedStatus} AND version=#{expectedVersion} AND deleted=0")
    int incrementReviewRoundAndUpdateState(Long id, ApplicationStatus expectedStatus, ApplicationStatus targetStatus, ApprovalLevel targetLevel, Integer expectedVersion, Long operatorId);
    @Update("UPDATE application SET supplement_reason=#{reason}, supplemented_at=#{handledAt} WHERE id=#{id} AND source='SUPPLEMENT' AND deleted=0")
    int updateSupplementMetadata(@Param("id") Long id, @Param("reason") String reason, @Param("handledAt") java.time.LocalDateTime handledAt);
    @Select("SELECT * FROM application WHERE id=#{id} AND source=#{source} AND deleted=0")
    Application findBySource(@Param("id") Long id, @Param("source") ApplicationSource source);
}
