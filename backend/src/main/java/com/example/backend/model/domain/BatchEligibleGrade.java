package com.example.backend.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("batch_eligible_grade")
public class BatchEligibleGrade {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("batch_id")     private Long batchId;
    @TableField("grade_id")     private Long gradeId;
    @TableField("create_time")  private LocalDateTime createTime;
}
