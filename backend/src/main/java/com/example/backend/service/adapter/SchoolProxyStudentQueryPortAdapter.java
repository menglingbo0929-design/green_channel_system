package com.example.backend.service.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.StudentMapper;
import com.example.backend.model.domain.Student;
import com.example.backend.model.dto.StudentApplicationProfile;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.service.StudentProfileQueryService;
import com.example.backend.service.port.SchoolProxyStudentQueryPort;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 学校代申请的学生查询适配器。
 *
 * <p>页面只提交学号；本适配器先从学生表定位有效学生，再复用成员一已经提供的
 * {@link StudentProfileQueryService} 读取学院、专业、年级和班级名称。这样返回给
 * 6.1.3 页面的是数据库真实数据，不在成员四代码中另外维护组织名称。</p>
 */
/**
 * Historical local adapter retained for source migration only.
 * Member one now supplies {@code SchoolProxyStudentQueryServiceImpl} as the
 * formal injectable port with by-student-id and batch-query support. Keeping
 * this adapter as a Spring bean would produce duplicate candidates.
 */
@Deprecated(forRemoval = false)
public class SchoolProxyStudentQueryPortAdapter implements SchoolProxyStudentQueryPort {

    /** 按视频项目原有风格使用字段注入。 */
    @Autowired
    private StudentMapper studentMapper;

    /** 复用成员一维护的学生画像查询服务。 */
    @Autowired
    private StudentProfileQueryService studentProfileQueryService;

    /**
     * 按学号查询启用状态的学生，并转换为学校代申请页面所需的最小快照。
     *
     * @param studentNo 页面输入的学生学号
     * @return 学生及其组织归属信息
     */
    @Override
    public SchoolProxyStudentVO findEnabledStudentByStudentNo(String studentNo) {
        Student student = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getStudentNo, studentNo)
                        .eq(Student::getEnabled, 1)
                        .last("LIMIT 1")
        );

        return toView(student.getId());
    }

    /**
     * 历史适配器仍保持完整的 Port 方法签名，以便依赖该接口的历史代码可编译。
     * 正式 Spring Bean 仍由成员一的 `SchoolProxyStudentQueryServiceImpl` 提供。
     */
    @Override
    public SchoolProxyStudentVO findEnabledStudentById(Long studentId) {
        Student student = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getId, studentId)
                        .eq(Student::getEnabled, 1)
                        .last("LIMIT 1")
        );
        return student == null ? null : toView(student.getId());
    }

    /** 保留批量查询签名，不注册为 Spring Bean，因而不会与成员一正式实现冲突。 */
    @Override
    public List<SchoolProxyStudentVO> findEnabledStudentsByIds(Collection<Long> studentIds) {
        List<SchoolProxyStudentVO> result = new ArrayList<>();
        if (studentIds == null || studentIds.isEmpty()) {
            return result;
        }
        for (Long studentId : studentIds) {
            SchoolProxyStudentVO student = findEnabledStudentById(studentId);
            if (student != null) {
                result.add(student);
            }
        }
        return result;
    }

    /** 统一复用成员一提供的学生组织快照组装结果。 */
    private SchoolProxyStudentVO toView(Long studentId) {
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
