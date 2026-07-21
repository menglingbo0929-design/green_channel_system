package com.example.backend.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 专业下拉选项 —— 供成员二/三/四前端下拉框使用
 */
@Data
@Builder
public class MajorOption {
    private Long id;
    private String majorCode;
    private String majorName;
}
