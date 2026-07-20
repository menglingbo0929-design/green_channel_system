package com.example.backend.application.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArrearsApplicationMapper {
    @Select("SELECT COUNT(1) FROM arrears_application WHERE application_id=#{applicationId} AND deleted=0")
    int countActiveByApplicationId(Long applicationId);
}
