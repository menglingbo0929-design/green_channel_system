package com.example.backend.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("subsidy_batch_eligible_grade")
public class SubsidyBatchEligibleGrade {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("batch_id")
    private Long batchId;
    @TableField("grade_id")
    private Long gradeId;
    @TableField("create_time")
    private LocalDateTime createTime;
}
