package com.example.backend.model.vo.schoolproxy;

import lombok.Data;

/** 学校代申请选人页所需的最小学生快照，不暴露身份证号等敏感信息。 */
@Data
public class SchoolProxyStudentVO {
    private Long studentId;
    private String studentNo;
    private String studentName;
    /** 成员二资源预占学院额度所需组织主键。 */
    private Long collegeId;
    private String collegeName;
    private Long majorId;
    private String majorName;
    /** 成员二资源预占年级额度所需组织主键。 */
    private Long gradeId;
    private String gradeName;
    private Long classId;
    private String className;
}
