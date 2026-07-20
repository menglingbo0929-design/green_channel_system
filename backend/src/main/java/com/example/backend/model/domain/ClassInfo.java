package com.example.backend.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("class_info")
public class ClassInfo {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("class_code")
    private String classCode;
    @TableField("class_name")
    private String className;
    @TableField("major_id")
    private Long majorId;
    @TableField("grade_id")
    private Long gradeId;
    @TableField("college_id")
    private Long collegeId;
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
