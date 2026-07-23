package com.example.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class CreateSubsidyBatchRequest {
    @NotBlank private String batchCode;
    @NotBlank private String batchName;
    @NotBlank private String academicYear;
    @NotBlank @Pattern(regexp = "LIVING_SUBSIDY|TRAVEL_SUBSIDY") private String batchType;
    @NotNull private LocalDateTime startTime;
    @NotNull private LocalDateTime endTime;
    @Pattern(regexp = "DRAFT|OPEN|CLOSED") private String status;
    private String remark;
    private List<Long> eligibleGradeIds;
}
