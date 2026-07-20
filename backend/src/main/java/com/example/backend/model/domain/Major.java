package com.example.backend.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("major")
public class Major {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("major_code")
    private String majorCode;
    @TableField("major_name")
    private String majorName;
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
