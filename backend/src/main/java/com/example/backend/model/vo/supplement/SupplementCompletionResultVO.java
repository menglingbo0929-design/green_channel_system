package com.example.backend.model.vo.supplement;

import lombok.Data;

/**
 * 成员三完成补录自动审核后返回的最小状态快照。
 *
 * <p>成员三只负责审核记录和状态流转，不应被迫组装学生姓名、批次等展示字段，
 * 因此跨模块边界只返回成员三真正拥有的状态、层级和新版本号。</p>
 */
@Data
public class SupplementCompletionResultVO {
    private String status;
    private String currentLevel;
    private Integer version;
}
