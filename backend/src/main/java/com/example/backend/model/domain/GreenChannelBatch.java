package com.example.backend.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("green_channel_batch")
public class GreenChannelBatch {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("batch_code")       private String batchCode;
    @TableField("batch_name")       private String batchName;
    @TableField("academic_year")    private String academicYear;
    @TableField("start_time")       private LocalDateTime startTime;
    @TableField("end_time")         private LocalDateTime endTime;
    @TableField("college_deadline") private LocalDateTime collegeDeadline;
    @TableField("status")           private String status;
    @TableField("enabled")          private Integer enabled;
    @TableField("remark")           private String remark;
    @TableField("create_time")      private LocalDateTime createTime;
    @TableField("update_time")      private LocalDateTime updateTime;

    @TableField("deleted")
    @TableLogic(value = "0", delval = "1")
    private Long deleted;
}
