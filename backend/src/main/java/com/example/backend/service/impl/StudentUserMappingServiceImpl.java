package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.StudentMapper;
import com.example.backend.model.domain.Student;
import com.example.backend.service.StudentUserMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Centralizes user-to-student resolution used when JWT claims are issued. */
@Service
@RequiredArgsConstructor
public class StudentUserMappingServiceImpl implements StudentUserMappingService {

    private final StudentMapper studentMapper;

    @Override
    public Student findActiveStudentByUserId(Long userId) {
        if (userId == null) return null;
        List<Student> students = studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .eq(Student::getUserId, userId)
                .eq(Student::getDeleted, 0)
                .eq(Student::getEnabled, 1));
        if (students.size() > 1) {
            throw new IllegalStateException("A user may be linked to only one active student profile: " + userId);
        }
        return students.isEmpty() ? null : students.getFirst();
    }
}
