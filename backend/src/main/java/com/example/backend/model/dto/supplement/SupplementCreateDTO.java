package com.example.backend.model.dto.supplement;

import com.example.backend.model.dto.schoolproxy.SchoolProxyArrearsItemDTO;
import com.example.backend.model.dto.schoolproxy.SchoolProxyGiftItemDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 6.1.4 学校端线下补录请求。
 *
 * <p>该 DTO 只接收学校操作员可以填写的业务数据。申请来源、批次体系、
 * 目标状态、当前审核层级和操作人都由后端确定，禁止前端自行指定。</p>
 */
@Data
public class SupplementCreateDTO {

    /** 被补录学生的学号；后端通过成员一的学生查询能力转换为 studentId。 */
    @NotBlank(message = "学号不能为空")
    private String studentNo;

    /** 只允许 GREEN_CHANNEL、LIVING_SUBSIDY、TRAVEL_SUBSIDY。 */
    @NotBlank(message = "申请类型不能为空")
    private String applicationType;

    /** 对应绿色通道批次或补助批次，具体体系由 applicationType 唯一确定。 */
    @NotNull(message = "批次 ID 不能为空")
    @Positive(message = "批次 ID 必须大于 0")
    private Long batchId;

    /** 原申请原因，可为空；该字段进入成员二负责的 application.application_reason。 */
    private String applicationReason;

    /**
     * 为什么没有走线上申请而需要补录。该字段必须保留，便于学校审计补录行为。
     */
    @NotBlank(message = "补录原因不能为空")
    @Size(max = 500, message = "补录原因不能超过 500 个字符")
    private String supplementReason;

    /** 实际在线下完成办理的时间，不得晚于服务器当前时间。 */
    @NotNull(message = "线下办理时间不能为空")
    @PastOrPresent(message = "线下办理时间不能晚于当前时间")
    private LocalDateTime handledAt;

    /** 绿色通道欠费明细；补助申请必须保持为空。 */
    @Valid
    private List<SchoolProxyArrearsItemDTO> arrearsItems = new ArrayList<>();

    /** 绿色通道礼包明细；补助申请必须保持为空。 */
    @Valid
    private List<SchoolProxyGiftItemDTO> giftItems = new ArrayList<>();

    /** 生活补助或路费补助金额；绿色通道申请不得填写。 */
    private BigDecimal subsidyAmount;

    /** 创建补录和自动审核共同使用的幂等请求号。 */
    @NotBlank(message = "缺少幂等请求号")
    @Size(max = 64, message = "幂等请求号不能超过 64 个字符")
    private String requestId;
}
