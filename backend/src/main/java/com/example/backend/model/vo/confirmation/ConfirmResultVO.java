package com.example.backend.model.vo.confirmation;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 最终确认操作成功后的返回数据。
 *
 * <p>它属于“欠费最终确认”模块：用于告诉学校管理员本次确认是否完成、
 * 实际确认金额是多少、由谁确认以及生成了哪个单据编号。
 * 它不是单据查询/打印对象；完整单据展示将由后续独立的 Voucher 模块负责。</p>
 */
@Data
@Accessors(chain = true)
public class ConfirmResultVO {

    /** 新生成的欠费确认记录主键。 */
    private Long confirmationId;

    /** 已完成确认的申请主表 ID。 */
    private Long applicationId;

    /** 学生原始申报金额。 */
    private BigDecimal appliedAmount;

    /** 学校最终确认金额。 */
    private BigDecimal confirmedAmount;

    /** 本次确认生成的单据编号；这里只反馈编号，不负责单据详情展示。 */
    private String voucherNo;

    /** 执行确认的学校管理员用户 ID。 */
    private Long confirmUserId;

    /** 确认完成时间。 */
    private LocalDateTime confirmedAt;

    /** 申请确认后的流程状态，固定应为 COMPLETED。 */
    private String applicationStatus;
}
