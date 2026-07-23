package com.example.backend.model.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubsidyBatchVO {
    private Long id;
    private String batchCode;
    private String batchName;
    private String academicYear;
    private String batchType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer enabled;
    private String remark;
    private List<Long> eligibleGradeIds;
    private LocalDateTime createTime;
}
