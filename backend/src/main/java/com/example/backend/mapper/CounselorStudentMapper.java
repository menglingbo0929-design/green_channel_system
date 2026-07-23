package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.domain.CounselorStudent;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CounselorStudentMapper extends BaseMapper<CounselorStudent> {
    @Select("SELECT DISTINCT counselor_user_id FROM counselor_student WHERE student_id=#{studentId}")
    List<Long> findCounselorUserIds(@Param("studentId") Long studentId);
}
