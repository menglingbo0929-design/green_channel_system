package com.example.backend.model.dto.schoolproxy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 学校管理员代学生创建绿色通道申请草稿的请求体。 */
@Data
public class SchoolProxyDraftDTO {
    @NotBlank(message = "学号不能为空")
    private String studentNo;
    /** 6.1.3 固定为 GREEN_CHANNEL，防止误创建补助申请。 */
    @NotBlank(message = "批次类型不能为空")
    private String batchType;
    @NotNull(message = "批次 ID 不能为空")
    @Positive(message = "批次 ID 不合法")
    private Long batchId;
    private String applicationReason;
    @Valid
    private List<SchoolProxyArrearsItemDTO> arrearsItems = new ArrayList<>();
    @Valid
    private List<SchoolProxyGiftItemDTO> giftItems = new ArrayList<>();
    @NotBlank(message = "缺少幂等请求号")
    private String requestId;
}
