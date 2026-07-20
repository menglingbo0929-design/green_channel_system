package com.example.backend.model.dto.confirmation;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * “待确认欠费申请列表”的查询条件。
 *
 * <p>这个 DTO 专门服务 {@code /api/confirm/list}，因此只放待确认申请可以使用的字段。
 * 单据编号和单据状态属于“已经确认”的数据，不应混进待确认列表。</p>
 */
@Data
@Accessors(chain = true)
public class ArrearsConfirmationQueryDTO {

    /** 按申请编号模糊查询，例如 GC2026。 */
    private String applicationNo;

    /** 按学生学号模糊查询。 */
    private String studentNo;

    /** 按学生姓名模糊查询。 */
    private String studentName;
}
