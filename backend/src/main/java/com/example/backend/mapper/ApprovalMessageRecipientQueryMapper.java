package com.example.backend.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Select;

/** Read-only recipient queries used by the member-three message integration adapter. */
public interface ApprovalMessageRecipientQueryMapper {

    @Select("""
            SELECT DISTINCT cs.counselor_user_id
            FROM counselor_student cs
            JOIN sys_user u ON u.id = cs.counselor_user_id AND u.deleted = 0
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role r ON r.id = ur.role_id AND r.role_code = 'COUNSELOR'
            WHERE cs.student_id = #{studentId}
            ORDER BY cs.counselor_user_id
            """)
    List<Long> findCounselorUserIds(Long studentId);

    @Select("""
            SELECT DISTINCT ucs.user_id
            FROM student s
            JOIN user_college_scope ucs ON ucs.college_id = s.college_id
            JOIN sys_user u ON u.id = ucs.user_id AND u.deleted = 0
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role r ON r.id = ur.role_id AND r.role_code = 'COLLEGE'
            WHERE s.id = #{studentId} AND s.deleted = 0
            ORDER BY ucs.user_id
            """)
    List<Long> findCollegeUserIds(Long studentId);

    @Select("""
            SELECT DISTINCT u.id
            FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role r ON r.id = ur.role_id AND r.role_code = 'SCHOOL'
            WHERE u.deleted = 0
            ORDER BY u.id
            """)
    List<Long> findSchoolUserIds();
}
