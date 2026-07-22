package com.example.backend.service.port;

import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;

import java.util.Collection;
import java.util.List;

/**
 * 成员一按学号/ID 查询学生快照；成员四不得直接查询 student 表。
 */
public interface SchoolProxyStudentQueryPort {

    /** 按学号查单个学生 */
    SchoolProxyStudentVO findEnabledStudentByStudentNo(String studentNo);

    /** 按学生 ID 查 */
    default SchoolProxyStudentVO findEnabledStudentById(Long studentId) {
        throw new UnsupportedOperationException("按学生 ID 查询尚未实现");
    }

    /** 批量查 */
    default List<SchoolProxyStudentVO> findEnabledStudentsByIds(Collection<Long> studentIds) {
        throw new UnsupportedOperationException("批量学生查询尚未实现");
    }
}
