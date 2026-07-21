package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.CounselorStudentMapper;
import com.example.backend.mapper.StudentMapper;
import com.example.backend.model.domain.CounselorStudent;
import com.example.backend.model.domain.Student;
import com.example.backend.service.StudentScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 学生数据范围校验服务实现
 */
@Service
@RequiredArgsConstructor
public class StudentScopeServiceImpl implements StudentScopeService {

    private final StudentMapper studentMapper;
    private final CounselorStudentMapper counselorStudentMapper;

    @Override
    public boolean isStudentSelf(Long userId, Long studentId) {
        Student s = studentMapper.selectById(studentId);
        return s != null && s.getDeleted() == 0 && userId.equals(s.getUserId());
    }

    @Override
    public boolean isCounselorResponsibleFor(Long counselorUserId, Long studentId) {
        // 方式一：查 counselor_student 关联表
        boolean inMapping = counselorStudentMapper.exists(
                new LambdaQueryWrapper<CounselorStudent>()
                        .eq(CounselorStudent::getCounselorUserId, counselorUserId)
                        .eq(CounselorStudent::getStudentId, studentId));
        if (inMapping) return true;

        // 方式二：查 student.counselor_id 冗余字段
        Student s = studentMapper.selectById(studentId);
        return s != null && s.getDeleted() == 0 && counselorUserId.equals(s.getCounselorId());
    }

    @Override
    public boolean isStudentInCollege(Long studentId, Long collegeId) {
        Student s = studentMapper.selectById(studentId);
        return s != null && s.getDeleted() == 0 && collegeId.equals(s.getCollegeId());
    }
}
