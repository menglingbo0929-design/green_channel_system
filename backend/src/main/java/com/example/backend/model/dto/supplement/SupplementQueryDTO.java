package com.example.backend.model.dto.supplement;

import lombok.Data;

/**
 * 6.1.4 补录历史的可选筛选条件。
 *
 * <p>分页参数继续使用项目统一的 PageDTO，避免同一个项目出现两套
 * page/pageSize 和 pageNo/pageSize 命名。</p>
 */
@Data
public class SupplementQueryDTO {

    /** 精确学号；为空表示不过滤学生。 */
    private String studentNo;

    /** GREEN_CHANNEL、LIVING_SUBSIDY 或 TRAVEL_SUBSIDY。 */
    private String applicationType;

    /** 绿色通道批次或补助批次 ID。 */
    private Long batchId;

    /** 只允许补录可能出现的 CONFIRM_PENDING 或 COMPLETED。 */
    private String status;
}
