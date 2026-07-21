package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.CollegeMapper;
import com.example.backend.mapper.GradeMapper;
import com.example.backend.mapper.MajorMapper;
import com.example.backend.model.domain.College;
import com.example.backend.model.domain.Grade;
import com.example.backend.model.domain.Major;
import com.example.backend.model.dto.CollegeOption;
import com.example.backend.model.dto.GradeOption;
import com.example.backend.model.dto.MajorOption;
import com.example.backend.service.OrganizationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 组织机构查询服务实现
 */
@Service
@RequiredArgsConstructor
public class OrganizationQueryServiceImpl implements OrganizationQueryService {

    private final CollegeMapper collegeMapper;
    private final GradeMapper gradeMapper;
    private final MajorMapper majorMapper;

    @Override
    public List<CollegeOption> listColleges() {
        List<College> colleges = collegeMapper.selectList(
                new LambdaQueryWrapper<College>()
                        .eq(College::getEnabled, 1)
                        .eq(College::getDeleted, 0));
        return colleges.stream()
                .map(c -> CollegeOption.builder()
                        .id(c.getId())
                        .collegeCode(c.getCollegeCode())
                        .collegeName(c.getCollegeName())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<GradeOption> listGrades() {
        List<Grade> grades = gradeMapper.selectList(
                new LambdaQueryWrapper<Grade>()
                        .eq(Grade::getEnabled, 1)
                        .eq(Grade::getDeleted, 0));
        return grades.stream()
                .map(g -> GradeOption.builder()
                        .id(g.getId())
                        .gradeCode(g.getGradeCode())
                        .gradeName(g.getGradeName())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<MajorOption> listMajorsByCollege(Long collegeId) {
        List<Major> majors = majorMapper.selectList(
                new LambdaQueryWrapper<Major>()
                        .eq(Major::getCollegeId, collegeId)
                        .eq(Major::getEnabled, 1)
                        .eq(Major::getDeleted, 0));
        return majors.stream()
                .map(m -> MajorOption.builder()
                        .id(m.getId())
                        .majorCode(m.getMajorCode())
                        .majorName(m.getMajorName())
                        .build())
                .collect(Collectors.toList());
    }
}
