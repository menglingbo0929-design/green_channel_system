package com.example.backend.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("grade")
public class Grade {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("grade_code")
    private String gradeCode;
    @TableField("grade_name")
    private String gradeName;
    @TableField("enabled")
    private Integer enabled;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
    @TableField("deleted")
    @TableLogic(value = "0", delval = "1")
    private Long deleted;
}
