package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.CounselorStudentMapper;
import com.example.backend.model.domain.CounselorStudent;
import com.example.backend.service.CounselorStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 辅导员-学生关联管理服务实现
 */
@Service
@RequiredArgsConstructor
public class CounselorStudentServiceImpl implements CounselorStudentService {

    private final CounselorStudentMapper mapper;

    @Override
    @Transactional
    public void assign(Long counselorUserId, Long studentId) {
        // 已存在则幂等跳过
        boolean exists = mapper.exists(new LambdaQueryWrapper<CounselorStudent>()
                .eq(CounselorStudent::getCounselorUserId, counselorUserId)
                .eq(CounselorStudent::getStudentId, studentId));
        if (exists) return;

        CounselorStudent cs = new CounselorStudent();
        cs.setCounselorUserId(counselorUserId);
        cs.setStudentId(studentId);
        mapper.insert(cs);
    }

    @Override
    @Transactional
    public void assignBatch(Long counselorUserId, List<Long> studentIds) {
        for (Long studentId : studentIds) {
            assign(counselorUserId, studentId);
        }
    }

    @Override
    @Transactional
    public void remove(Long counselorUserId, Long studentId) {
        mapper.delete(new LambdaQueryWrapper<CounselorStudent>()
                .eq(CounselorStudent::getCounselorUserId, counselorUserId)
                .eq(CounselorStudent::getStudentId, studentId));
    }

    @Override
    public List<Long> listStudentIdsByCounselor(Long counselorUserId) {
        return mapper.selectList(new LambdaQueryWrapper<CounselorStudent>()
                        .eq(CounselorStudent::getCounselorUserId, counselorUserId))
                .stream()
                .map(CounselorStudent::getStudentId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> listCounselorIdsByStudent(Long studentId) {
        return mapper.selectList(new LambdaQueryWrapper<CounselorStudent>()
                        .eq(CounselorStudent::getStudentId, studentId))
                .stream()
                .map(CounselorStudent::getCounselorUserId)
                .collect(Collectors.toList());
    }
}
