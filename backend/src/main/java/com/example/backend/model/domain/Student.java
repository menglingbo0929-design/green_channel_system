package com.example.backend.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student")
public class Student {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("student_no")    private String studentNo;
    @TableField("student_name")  private String studentName;
    @TableField("college_id")    private Long collegeId;
    @TableField("major_id")      private Long majorId;
    @TableField("grade_id")      private Long gradeId;
    @TableField("class_id")      private Long classId;
    @TableField("phone")         private String phone;
    @TableField("origin_loan")   private Integer originLoan;
    @TableField("campus_loan")   private Integer campusLoan;
    @TableField("subsidy_level") private String subsidyLevel;
    @TableField("difficulty_level") private String difficultyLevel;
    @TableField("info_complete") private Integer infoComplete;
    @TableField("user_id")       private Long userId;
    @TableField("counselor_id")  private Long counselorId;
    @TableField("enabled")       private Integer enabled;
    @TableField("remark")        private String remark;
    @TableField("create_time")   private LocalDateTime createTime;
    @TableField("update_time")   private LocalDateTime updateTime;

    @TableField("deleted")
    @TableLogic(value = "0", delval = "1")
    private Long deleted;
}
