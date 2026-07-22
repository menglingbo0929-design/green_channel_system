package com.example.backend.approval.persistence.mapper;

import com.example.backend.approval.persistence.entity.ApprovalSubmissionRecordEntity;
import com.example.backend.application.domain.BatchType;
import com.example.backend.approval.persistence.type.SubmissionLevel;
import com.example.backend.approval.persistence.type.SubmissionScopeType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ApprovalSubmissionRecordMapper {

    @Insert("""
            INSERT INTO approval_submission_record (
                batch_type, green_channel_batch_id, subsidy_batch_id,
                submission_level, submission_type, scope_type, scope_id,
                application_id, review_round, submitter_id, submitted_count,
                status, request_id, submit_time
            ) VALUES (
                #{batchType}, #{greenChannelBatchId}, #{subsidyBatchId},
                #{submissionLevel}, #{submissionType}, #{scopeType}, #{scopeId},
                #{applicationId}, #{reviewRound}, #{submitterId}, #{submittedCount},
                #{status}, #{requestId}, #{submitTime}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ApprovalSubmissionRecordEntity entity);

    @Select("SELECT * FROM approval_submission_record WHERE id = #{id}")
    @Results(id = "approvalSubmissionResultMap", value = {
            @Result(column = "batch_type", property = "batchType"),
            @Result(column = "green_channel_batch_id", property = "greenChannelBatchId"),
            @Result(column = "subsidy_batch_id", property = "subsidyBatchId"),
            @Result(column = "submission_level", property = "submissionLevel"),
            @Result(column = "submission_type", property = "submissionType"),
            @Result(column = "scope_type", property = "scopeType"),
            @Result(column = "scope_id", property = "scopeId"),
            @Result(column = "application_id", property = "applicationId"),
            @Result(column = "review_round", property = "reviewRound"),
            @Result(column = "submitter_id", property = "submitterId"),
            @Result(column = "submitted_count", property = "submittedCount"),
            @Result(column = "request_id", property = "requestId"),
            @Result(column = "submit_time", property = "submitTime"),
            @Result(column = "create_time", property = "createTime")
    })
    Optional<ApprovalSubmissionRecordEntity> findById(Long id);

    @Select("SELECT * FROM approval_submission_record WHERE request_id = #{requestId}")
    @ResultMap("approvalSubmissionResultMap")
    Optional<ApprovalSubmissionRecordEntity> findByRequestId(String requestId);

    @Select("""
            SELECT * FROM approval_submission_record
            WHERE batch_type = #{batchType}
              AND (
                    (#{batchType} = 'GREEN_CHANNEL' AND green_channel_batch_id = #{batchId})
                    OR
                    (#{batchType} = 'SUBSIDY' AND subsidy_batch_id = #{batchId})
              )
              AND submission_level = #{submissionLevel}
              AND scope_type = #{scopeType}
              AND scope_id = #{scopeId}
            ORDER BY submit_time ASC, id ASC
            """)
    @ResultMap("approvalSubmissionResultMap")
    List<ApprovalSubmissionRecordEntity> listByScope(
            @Param("batchType") BatchType batchType,
            @Param("batchId") Long batchId,
            @Param("submissionLevel") SubmissionLevel submissionLevel,
            @Param("scopeType") SubmissionScopeType scopeType,
            @Param("scopeId") Long scopeId
    );
}
