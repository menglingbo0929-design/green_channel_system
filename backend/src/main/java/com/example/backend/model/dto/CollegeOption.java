package com.example.backend.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 学院下拉选项 —— 供成员二/三/四前端下拉框使用
 */
@Data
@Builder
public class CollegeOption {
    private Long id;
    private String collegeCode;
    private String collegeName;
}
