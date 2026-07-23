package com.example.backend.model.dto;

/** 成员一负责的只读学生与组织快照；成员二只消费，绝不创建 student/organization Mapper。 */
public record StudentOrganizationSnapshot(Long studentId, String studentNo, String studentName, String collegeName,
                                          String majorName, String gradeName, String className) { }
