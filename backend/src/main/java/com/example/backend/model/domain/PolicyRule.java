package com.example.backend.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("policy_rule")
public class PolicyRule {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("rule_code")    private String ruleCode;
    @TableField("rule_name")    private String ruleName;
    @TableField("rule_content") private String ruleContent;
    @TableField("batch_type")   private String batchType;
    @TableField("sort_order")   private Integer sortOrder;
    @TableField("enabled")      private Integer enabled;
    @TableField("create_time")  private LocalDateTime createTime;
    @TableField("update_time")  private LocalDateTime updateTime;

    @TableField("deleted")
    @TableLogic(value = "0", delval = "1")
    private Long deleted;
}
