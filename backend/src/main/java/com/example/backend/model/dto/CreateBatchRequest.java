package com.example.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateBatchRequest {
    @NotBlank
    private String batchCode;
    @NotBlank
    private String batchName;
    @NotNull
    private LocalDateTime startTime;
    @NotNull
    private LocalDateTime endTime;
    @NotNull
    private LocalDateTime collegeDeadline;
    private String remark;
    private List<Long> eligibleGradeIds;
}
