package com.example.backend.application.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface ApplicationOperationMapper {
    @Select("SELECT application_id FROM application_operation_record WHERE request_id=#{requestId}") Long findApplicationIdByRequestId(String requestId);
    @Select("SELECT COUNT(*) FROM application_operation_record WHERE application_id=#{applicationId} AND operation_type=#{operationType} AND request_id=#{requestId}")
    int countByApplicationOperationRequest(@Param("applicationId") Long applicationId, @Param("operationType") String operationType, @Param("requestId") String requestId);
    @Insert("INSERT INTO application_operation_record(application_id,operation_type,request_id,operator_id) VALUES(#{applicationId},#{operationType},#{requestId},#{operatorId})")
    int insert(Long applicationId, String operationType, String requestId, Long operatorId);
}
