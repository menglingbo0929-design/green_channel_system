package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.CounselorStudentMapper;
import com.example.backend.mapper.StudentMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.mapper.UserRoleMapper;
import com.example.backend.model.domain.CounselorStudent;
import com.example.backend.model.domain.Student;
import com.example.backend.model.domain.User;
import com.example.backend.service.CounselorStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Maintains the normalized counselor-student relation and the primary-counselor cache together. */
@Service
@RequiredArgsConstructor
public class CounselorStudentServiceImpl implements CounselorStudentService {

    private final CounselorStudentMapper mapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    @Transactional
    public void assign(Long counselorUserId, Long studentId) {
        requireActiveCounselor(counselorUserId);
        Student student = requireActiveStudent(studentId);
        boolean exists = mapper.exists(new LambdaQueryWrapper<CounselorStudent>()
                .eq(CounselorStudent::getCounselorUserId, counselorUserId)
                .eq(CounselorStudent::getStudentId, studentId));
        if (!exists) {
            CounselorStudent relation = new CounselorStudent();
            relation.setCounselorUserId(counselorUserId);
            relation.setStudentId(studentId);
            mapper.insert(relation);
        }

        // The first assigned counselor is the primary one. Later assignments are co-advisers.
        if (student.getCounselorId() == null) {
            student.setCounselorId(counselorUserId);
            studentMapper.updateById(student);
        }
    }

    @Override
    @Transactional
    public void assignBatch(Long counselorUserId, List<Long> studentIds) {
        requireActiveCounselor(counselorUserId);
        if (studentIds == null) return;
        for (Long studentId : studentIds) {
            if (studentId != null) assign(counselorUserId, studentId);
        }
    }

    @Override
    @Transactional
    public void remove(Long counselorUserId, Long studentId) {
        Student student = requireActiveStudent(studentId);
        mapper.delete(new LambdaQueryWrapper<CounselorStudent>()
                .eq(CounselorStudent::getCounselorUserId, counselorUserId)
                .eq(CounselorStudent::getStudentId, studentId));

        if (counselorUserId.equals(student.getCounselorId())) {
            Long replacement = mapper.selectList(new LambdaQueryWrapper<CounselorStudent>()
                            .eq(CounselorStudent::getStudentId, studentId)
                            .orderByAsc(CounselorStudent::getId))
                    .stream().map(CounselorStudent::getCounselorUserId).findFirst().orElse(null);
            student.setCounselorId(replacement);
            studentMapper.updateById(student);
        }
    }

    @Override
    public List<Long> listStudentIdsByCounselor(Long counselorUserId) {
        return mapper.selectList(new LambdaQueryWrapper<CounselorStudent>()
                        .eq(CounselorStudent::getCounselorUserId, counselorUserId))
                .stream().map(CounselorStudent::getStudentId).toList();
    }

    @Override
    public List<Long> listCounselorIdsByStudent(Long studentId) {
        return mapper.selectList(new LambdaQueryWrapper<CounselorStudent>()
                        .eq(CounselorStudent::getStudentId, studentId))
                .stream().map(CounselorStudent::getCounselorUserId).toList();
    }

    private Student requireActiveStudent(Long studentId) {
        if (studentId == null) throw new IllegalArgumentException("studentId is required");
        Student student = studentMapper.selectById(studentId);
        if (student == null || !Long.valueOf(0L).equals(student.getDeleted())
                || !Integer.valueOf(1).equals(student.getEnabled())) {
            throw new IllegalArgumentException("Student does not exist or is disabled: " + studentId);
        }
        return student;
    }

    private void requireActiveCounselor(Long counselorUserId) {
        if (counselorUserId == null) throw new IllegalArgumentException("counselorUserId is required");
        User user = userMapper.selectById(counselorUserId);
        if (user == null || !Long.valueOf(0L).equals(user.getDeleted())
                || !userRoleMapper.selectRoleCodesByUserId(counselorUserId).contains("COUNSELOR")) {
            throw new IllegalArgumentException("Counselor user is invalid: " + counselorUserId);
        }
    }
}
