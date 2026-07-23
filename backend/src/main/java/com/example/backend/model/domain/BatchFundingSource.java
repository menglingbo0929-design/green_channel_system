package com.example.backend.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("batch_funding_source")
public class BatchFundingSource {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("batch_id")
    private Long batchId;
    @TableField("source_code")
    private String sourceCode;
    @TableField("create_time")
    private LocalDateTime createTime;
}
