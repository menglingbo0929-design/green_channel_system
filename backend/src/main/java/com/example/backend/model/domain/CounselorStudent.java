package com.example.backend.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("counselor_student")
public class CounselorStudent {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("counselor_user_id") private Long counselorUserId;
    @TableField("student_id")        private Long studentId;
    @TableField("create_time")       private LocalDateTime createTime;
}
