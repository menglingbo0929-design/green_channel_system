package com.example.backend.model.vo.supplement;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线下补录创建、详情和历史列表共用的返回对象。
 *
 * <p>字段与 collaboration-rules.md 第 17 节完全一致。这样前端不需要为
 * 创建结果、详情结果和列表记录维护三套名称不同但含义相同的数据结构。</p>
 */
@Data
public class SupplementApplicationVO {
    private Long applicationId;
    private String applicationNo;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String applicationType;
    private String batchType;
    private Long batchId;
    private String source;
    private String status;
    private String currentLevel;
    private Integer version;
    private Boolean containsArrears;
    private Long supplementUserId;
    private LocalDateTime supplementedAt;
    private String supplementReason;
}
