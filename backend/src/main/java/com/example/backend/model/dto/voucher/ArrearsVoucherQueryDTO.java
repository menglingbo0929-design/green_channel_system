package com.example.backend.model.dto.voucher;

import lombok.Data;

/** 学校端确认单列表的最小筛选条件；学生/组织筛选由成员二快照接口后续扩展。 */
@Data
public class ArrearsVoucherQueryDTO {
    /** 单据编号模糊查询，例如 GC2026。 */
    private String voucherNo;
}
