package com.example.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateBatchRequest {
    @NotBlank
    private String batchName;
    @NotBlank
    private String academicYear;
    @NotNull
    private LocalDateTime startTime;
    @NotNull
    private LocalDateTime endTime;
    @NotNull
    private LocalDateTime collegeDeadline;
    private String remark;
    @Pattern(regexp = "DRAFT|OPEN|CLOSED", message = "批次状态必须为 DRAFT、OPEN 或 CLOSED")
    private String status;
    private List<Long> eligibleGradeIds;
    private List<String> fundingSourceCodes;
}
