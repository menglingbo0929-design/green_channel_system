package com.example.backend.model.vo.schoolproxy;

import lombok.Data;

/** 学校代申请选人页所需的最小学生快照，不暴露身份证号等敏感信息。 */
@Data
public class SchoolProxyStudentVO {
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Long collegeId;
    private String collegeName;
    private Long majorId;
    private String majorName;
    private Long gradeId;
    private String gradeName;
    private Long classId;
    private String className;
}
