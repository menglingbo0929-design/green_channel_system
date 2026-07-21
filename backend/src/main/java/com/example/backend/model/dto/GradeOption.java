package com.example.backend.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 年级下拉选项 —— 供成员二/三/四前端下拉框使用
 */
@Data
@Builder
public class GradeOption {
    private Long id;
    private String gradeCode;
    private String gradeName;
}
