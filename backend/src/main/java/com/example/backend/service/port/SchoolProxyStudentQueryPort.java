package com.example.backend.service.port;

import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;

/**
 * 成员一提供的按学号查学生能力；成员四不得直接查询 student 表。
 *
 * <p>TODO(成员一)：基于真实 student 和组织表实现有效学生查询，并结合
 * CurrentUserProvider 校验学校数据范围；成员四不得用临时学生数据替代。</p>
 */
public interface SchoolProxyStudentQueryPort {
    SchoolProxyStudentVO findEnabledStudentByStudentNo(String studentNo);
}
