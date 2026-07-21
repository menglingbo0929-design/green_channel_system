package com.example.backend.service.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.StudentMapper;
import com.example.backend.model.domain.Student;
import com.example.backend.model.dto.StudentApplicationProfile;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.service.StudentProfileQueryService;
import com.example.backend.service.port.SchoolProxyStudentQueryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 学校代申请的学生查询适配器。
 *
 * <p>页面只提交学号；本适配器先从学生表定位有效学生，再复用成员一已经提供的
 * {@link StudentProfileQueryService} 读取学院、专业、年级和班级名称。这样返回给
 * 6.1.3 页面的是数据库真实数据，不在成员四代码中另外维护组织名称。</p>
 */
@Component
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

        StudentApplicationProfile profile =
                studentProfileQueryService.getRequiredProfile(student.getId());

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
