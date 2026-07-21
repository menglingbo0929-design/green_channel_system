package com.example.backend.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BatchVO {
    private Long id;
    private String batchCode;
    private String batchName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime collegeDeadline;
    private String status;
    private Integer enabled;
    private String remark;
    private List<Long> eligibleGradeIds;
    private LocalDateTime createTime;
}
