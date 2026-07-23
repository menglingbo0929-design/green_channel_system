package com.example.backend.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 学生本人允许维护的资料；学号、姓名和组织归属不接受客户端修改。 */
public record UpdateStudentProfileRequest(
        @NotBlank(message = "联系电话不能为空")
        @Size(max = 20, message = "联系电话不能超过 20 个字符")
        String phone,
        @Min(value = 0, message = "生源地贷款标记只能为 0 或 1")
        @Max(value = 1, message = "生源地贷款标记只能为 0 或 1")
        Integer originLoan,
        @Min(value = 0, message = "校园地贷款标记只能为 0 或 1")
        @Max(value = 1, message = "校园地贷款标记只能为 0 或 1")
        Integer campusLoan,
        @Pattern(
                regexp = "^$|SPECIAL_DIFFICULTY|DIFFICULTY|GENERAL_DIFFICULTY|NONE|特别困难|困难|一般困难|不困难",
                message = "困难等级取值无效"
        )
        String difficultyLevel
) {
    /** 兼容历史中文值和旧版客户端，数据库统一保存稳定的枚举编码。 */
    public String normalizedDifficultyLevel() {
        if (difficultyLevel == null || difficultyLevel.isBlank()) return null;
        return switch (difficultyLevel) {
            case "特别困难" -> "SPECIAL_DIFFICULTY";
            case "困难" -> "DIFFICULTY";
            case "一般困难" -> "GENERAL_DIFFICULTY";
            case "不困难" -> "NONE";
            default -> difficultyLevel;
        };
    }
}
