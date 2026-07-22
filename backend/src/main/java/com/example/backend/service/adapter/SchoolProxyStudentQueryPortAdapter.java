package com.example.backend.service.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.StudentMapper;
import com.example.backend.model.domain.Student;
import com.example.backend.model.dto.StudentApplicationProfile;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.service.StudentProfileQueryService;
import com.example.backend.service.port.SchoolProxyStudentQueryPort;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;

/**
 * Historical local adapter retained for source migration only.
 *
 * <p>Member one now supplies {@code SchoolProxyStudentQueryServiceImpl} as the
 * formal injectable port. This class must remain a non-Spring bean to avoid
 * duplicate candidates during migration.</p>
 */
@Deprecated(forRemoval = false)
public class SchoolProxyStudentQueryPortAdapter implements SchoolProxyStudentQueryPort {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private StudentProfileQueryService studentProfileQueryService;

    @Override
    public SchoolProxyStudentVO findEnabledStudentByStudentNo(String studentNo) {
        Student student = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getStudentNo, studentNo)
                        .eq(Student::getEnabled, 1)
                        .last("LIMIT 1")
        );
        return student == null ? null : toSnapshot(student.getId());
    }

    @Override
    public SchoolProxyStudentVO findEnabledStudentById(Long studentId) {
        Student student = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getId, studentId)
                        .eq(Student::getEnabled, 1)
                        .last("LIMIT 1")
        );
        return student == null ? null : toSnapshot(student.getId());
    }

    @Override
    public List<SchoolProxyStudentVO> findEnabledStudentsByIds(Collection<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }
        return studentMapper.selectList(
                        new LambdaQueryWrapper<Student>()
                                .in(Student::getId, studentIds)
                                .eq(Student::getEnabled, 1)
                ).stream()
                .map(Student::getId)
                .map(this::toSnapshot)
                .toList();
    }

    private SchoolProxyStudentVO toSnapshot(Long studentId) {
        StudentApplicationProfile profile =
                studentProfileQueryService.getRequiredProfile(studentId);

        SchoolProxyStudentVO result = new SchoolProxyStudentVO();
        result.setStudentId(profile.getStudentId());
        result.setStudentNo(profile.getStudentNo());
        result.setStudentName(profile.getStudentName());
        result.setCollegeId(profile.getCollegeId());
        result.setCollegeName(profile.getCollegeName());
        result.setMajorId(profile.getMajorId());
        result.setMajorName(profile.getMajorName());
        result.setGradeId(profile.getGradeId());
        result.setGradeName(profile.getGradeName());
        result.setClassId(profile.getClassId());
        result.setClassName(profile.getClassName());
        return result;
    }
}
