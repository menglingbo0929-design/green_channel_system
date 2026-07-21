package com.example.backend.service;

import com.example.backend.model.dto.CollegeOption;
import com.example.backend.model.dto.GradeOption;
import com.example.backend.model.dto.MajorOption;

import java.util.List;

/**
 * 组织机构查询服务 —— 成员一向成员二、三、四提供
 *
 * 提供学院、年级、专业的下拉选项，供前端表单使用。
 * 只返回启用且未删除的数据。
 */
public interface OrganizationQueryService {

    /** 查询所有启用的学院（供下拉选项使用） */
    List<CollegeOption> listColleges();

    /** 查询所有启用的年级 */
    List<GradeOption> listGrades();

    /** 查询某学院下所有启用的专业 */
    List<MajorOption> listMajorsByCollege(Long collegeId);
}
