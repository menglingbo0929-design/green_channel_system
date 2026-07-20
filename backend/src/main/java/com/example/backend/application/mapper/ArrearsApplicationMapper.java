package com.example.backend.application.mapper;

import com.example.backend.application.dto.ArrearsItemSnapshot;
import com.example.backend.application.dto.PendingArrearsApplication;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ArrearsApplicationMapper {
    @Select("SELECT COUNT(1) FROM arrears_application WHERE application_id=#{applicationId} AND deleted=0")
    int countActiveByApplicationId(Long applicationId);

    @Select("SELECT a.id application_id, a.application_no, a.version, a.student_id, COALESCE(SUM(aa.declared_amount), 0) applied_amount " +
            "FROM application a JOIN arrears_application aa ON aa.application_id=a.id AND aa.deleted=0 " +
            "WHERE a.deleted=0 AND a.status='CONFIRM_PENDING' GROUP BY a.id,a.application_no,a.version,a.student_id ORDER BY a.id DESC LIMIT #{limit} OFFSET #{offset}")
    List<PendingArrearsApplication> pagePending(int limit, int offset);
    @Select("SELECT COUNT(DISTINCT a.id) FROM application a JOIN arrears_application aa ON aa.application_id=a.id AND aa.deleted=0 WHERE a.deleted=0 AND a.status='CONFIRM_PENDING'")
    long countPending();
    @SelectProvider(type = ArrearsApplicationSql.class, method = "itemsByApplicationIds")
    List<ArrearsItemSnapshot> findItemsByApplicationIds(@Param("applicationIds") List<Long> applicationIds);
}
