package com.example.backend.service.impl;

import com.example.backend.mapper.ClassInfoMapper;
import com.example.backend.mapper.CollegeMapper;
import com.example.backend.mapper.GradeMapper;
import com.example.backend.mapper.MajorMapper;
import com.example.backend.mapper.StudentMapper;
import com.example.backend.model.domain.ClassInfo;
import com.example.backend.model.domain.College;
import com.example.backend.model.domain.Grade;
import com.example.backend.model.domain.Major;
import com.example.backend.model.domain.Student;
import com.example.backend.model.dto.StudentApplicationProfile;
import com.example.backend.service.StudentProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 学生信息查询服务实现
 *
 * 成员二创建申请时调用 getRequiredProfile，获取可信的学生画像数据写入申请快照。
 * 所有组织名称均从数据库实时查询，避免前端传入伪造的学院/专业名称。
 */
@Service
@RequiredArgsConstructor
public class StudentProfileQueryServiceImpl implements StudentProfileQueryService {

    private final StudentMapper studentMapper;
    private final CollegeMapper collegeMapper;
    private final MajorMapper majorMapper;
    private final GradeMapper gradeMapper;
    private final ClassInfoMapper classInfoMapper;

    @Override
    public StudentApplicationProfile getRequiredProfile(Long studentId) {
        // 1. 查学生基本信息
        Student s = studentMapper.selectById(studentId);
        if (s == null || s.getDeleted() != 0) {
            throw new IllegalArgumentException("学生不存在: id=" + studentId);
        }
        if (s.getEnabled() == null || s.getEnabled() == 0) {
            throw new IllegalArgumentException("学生已停用: id=" + studentId);
        }

        // 2. 查询组织名称
        String collegeName = lookupName(collegeMapper.selectById(s.getCollegeId()), s.getCollegeId(), "学院");
        String majorName   = lookupName(majorMapper.selectById(s.getMajorId()),     s.getMajorId(),   "专业");
        String gradeName   = lookupName(gradeMapper.selectById(s.getGradeId()),     s.getGradeId(),   "年级");
        String className   = lookupName(classInfoMapper.selectById(s.getClassId()), s.getClassId(),   "班级");

        // 3. 组装返回
        return StudentApplicationProfile.builder()
                .studentId(s.getId())
                .studentNo(s.getStudentNo())
                .studentName(s.getStudentName())
                .phone(s.getPhone())
                .collegeId(s.getCollegeId())
                .collegeName(collegeName)
                .majorId(s.getMajorId())
                .majorName(majorName)
                .gradeId(s.getGradeId())
                .gradeName(gradeName)
                .classId(s.getClassId())
                .className(className)
                .originLoan(s.getOriginLoan())
                .campusLoan(s.getCampusLoan())
                .subsidyLevel(s.getSubsidyLevel())
                .difficultyLevel(s.getDifficultyLevel())
                .infoComplete(s.getInfoComplete())
                .build();
    }

    /** 安全取值：记录查不到时抛明确错误 */
    private String lookupName(Object entity, Long id, String label) {
        if (entity == null) {
            throw new IllegalArgumentException(label + "不存在: id=" + id);
        }
        // 利用多态获取 name 字段
        if (entity instanceof College c)    return c.getCollegeName();
        if (entity instanceof Major m)      return m.getMajorName();
        if (entity instanceof Grade g)      return g.getGradeName();
        if (entity instanceof ClassInfo c)  return c.getClassName();
        throw new IllegalArgumentException("未知实体类型: " + entity.getClass());
    }
}
