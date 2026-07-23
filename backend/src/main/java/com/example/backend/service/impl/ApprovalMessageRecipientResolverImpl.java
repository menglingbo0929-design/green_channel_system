package com.example.backend.service.impl;

import com.example.backend.service.ApprovalMessageRecipientResolver;
import com.example.backend.mapper.StudentMapper;
import com.example.backend.model.domain.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 消息收件人解析实现 —— 成员一
 *
 * 成员三发送审核消息时需要将 studentId 转为登录用户 ID。
 * 成员一通过 student 表完成转换，成员三不直接读 student 表。
 */
@Service
@RequiredArgsConstructor
public class ApprovalMessageRecipientResolverImpl implements ApprovalMessageRecipientResolver {

    private final StudentMapper studentMapper;

    @Override
    public Long getStudentUserId(Long studentId) {
        Student s = studentMapper.selectById(studentId);
        if (s == null || s.getDeleted() != 0) {
            throw new IllegalArgumentException("学生不存在: id=" + studentId);
        }
        if (s.getUserId() == null) {
            throw new IllegalStateException("该学生未关联登录账号: id=" + studentId);
        }
        return s.getUserId();
    }
}
