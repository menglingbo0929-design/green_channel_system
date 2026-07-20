package com.example.backend.model.vo.schoolproxy;

import lombok.Data;

/** 学校代申请选人页所需的最小学生快照，不暴露身份证号、手机号等敏感信息。 */
@Data
public class SchoolProxyStudentVO {
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String collegeName;
    private String majorName;
    private String gradeName;
    private String className;
}
