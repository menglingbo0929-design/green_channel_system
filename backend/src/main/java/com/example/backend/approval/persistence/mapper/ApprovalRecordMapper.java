package com.example.backend.approval.persistence.mapper;

import com.example.backend.approval.persistence.entity.ApprovalRecordEntity;
import com.example.backend.approval.persistence.type.ApprovalRecordLevel;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ApprovalRecordMapper {

    @Insert("""
            INSERT INTO approval_record (
                application_id, review_round, approval_level, approver_id,
                approver_name_snapshot, action, comment, old_status, new_status,
                modified_fields, request_id
            ) VALUES (
                #{applicationId}, #{reviewRound}, #{approvalLevel}, #{approverId},
                #{approverNameSnapshot}, #{action}, #{comment}, #{oldStatus}, #{newStatus},
                #{modifiedFields}, #{requestId}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ApprovalRecordEntity entity);

    @Select("SELECT * FROM approval_record WHERE id = #{id}")
    @Results(id = "approvalRecordResultMap", value = {
            @Result(column = "application_id", property = "applicationId"),
            @Result(column = "review_round", property = "reviewRound"),
            @Result(column = "approval_level", property = "approvalLevel"),
            @Result(column = "approver_id", property = "approverId"),
            @Result(column = "approver_name_snapshot", property = "approverNameSnapshot"),
            @Result(column = "old_status", property = "oldStatus"),
            @Result(column = "new_status", property = "newStatus"),
            @Result(column = "modified_fields", property = "modifiedFields"),
            @Result(column = "request_id", property = "requestId"),
            @Result(column = "create_time", property = "createTime")
    })
    Optional<ApprovalRecordEntity> findById(Long id);

    @Select("SELECT * FROM approval_record WHERE request_id = #{requestId}")
    @ResultMap("approvalRecordResultMap")
    Optional<ApprovalRecordEntity> findByRequestId(String requestId);

    @Select("""
            SELECT * FROM approval_record
            WHERE application_id = #{applicationId}
            ORDER BY create_time ASC, id ASC
            """)
    @ResultMap("approvalRecordResultMap")
    List<ApprovalRecordEntity> listByApplicationId(Long applicationId);

    @Select("""
            SELECT * FROM approval_record
            WHERE application_id = #{applicationId}
              AND review_round = #{reviewRound}
              AND approval_level = #{approvalLevel}
              AND action IN ('APPROVE', 'RETURN', 'REJECT')
            ORDER BY create_time DESC, id DESC
            LIMIT 1
            """)
    @ResultMap("approvalRecordResultMap")
    Optional<ApprovalRecordEntity> findLatestDecision(
            @Param("applicationId") Long applicationId,
            @Param("reviewRound") Integer reviewRound,
            @Param("approvalLevel") ApprovalRecordLevel approvalLevel
    );
}
