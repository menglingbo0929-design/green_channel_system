package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.*;
import com.example.backend.model.domain.*;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.service.port.SchoolProxyStudentQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 学校代申请学生查询实现 —— 成员一
 */
@Service
@RequiredArgsConstructor
public class SchoolProxyStudentQueryServiceImpl implements SchoolProxyStudentQueryPort {

    private final StudentMapper studentMapper;
    private final CollegeMapper collegeMapper;
    private final MajorMapper majorMapper;
    private final GradeMapper gradeMapper;
    private final ClassInfoMapper classInfoMapper;

    @Override
    public SchoolProxyStudentVO findEnabledStudentByStudentNo(String studentNo) {
        Student s = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getStudentNo, studentNo)
                        .eq(Student::getDeleted, 0));
        if (s == null || s.getEnabled() == 0) return null;
        return toVO(s);
    }

    @Override
    public SchoolProxyStudentVO findEnabledStudentById(Long studentId) {
        Student s = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getId, studentId)
                        .eq(Student::getDeleted, 0));
        if (s == null || s.getEnabled() == 0) return null;
        return toVO(s);
    }

    @Override
    public List<SchoolProxyStudentVO> findEnabledStudentsByIds(Collection<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) return List.of();
        return studentMapper.selectList(
                new LambdaQueryWrapper<Student>()
                        .in(Student::getId, studentIds)
                        .eq(Student::getDeleted, 0)
                        .eq(Student::getEnabled, 1))
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private SchoolProxyStudentVO toVO(Student s) {
        SchoolProxyStudentVO vo = new SchoolProxyStudentVO();
        vo.setStudentId(s.getId());
        vo.setStudentNo(s.getStudentNo());
        vo.setStudentName(s.getStudentName());
        vo.setCollegeId(s.getCollegeId());
        vo.setMajorId(s.getMajorId());
        vo.setGradeId(s.getGradeId());
        vo.setClassId(s.getClassId());
        if (s.getCollegeId() != null) {
            College c = collegeMapper.selectById(s.getCollegeId());
            if (c != null) vo.setCollegeName(c.getCollegeName());
        }
        if (s.getMajorId() != null) {
            Major m = majorMapper.selectById(s.getMajorId());
            if (m != null) vo.setMajorName(m.getMajorName());
        }
        if (s.getGradeId() != null) {
            Grade g = gradeMapper.selectById(s.getGradeId());
            if (g != null) vo.setGradeName(g.getGradeName());
        }
        if (s.getClassId() != null) {
            ClassInfo ci = classInfoMapper.selectById(s.getClassId());
            if (ci != null) vo.setClassName(ci.getClassName());
        }
        return vo;
    }
}
