package com.example.backend.model.dto.confirmation;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 学校管理员进行欠费最终确认时接收的 JSON 参数。
 *
 * <p>写法与 demo 的 UserDTO 相同：DTO 仅接收前端可填写的参数，
 * 不允许前端传入确认人、确认时间、单据编号等应由后端控制的字段。</p>
 */
@Data
@Accessors(chain = true)
public class ConfirmArrearsDTO {

    /**
     * 学校最终确认的实际欠费金额。
     * Service 会校验：金额必须大于 0，且不得超过成员二提供的学生申报金额。
     */
    @NotNull(message = "实际确认金额不能为空")
    @Positive(message = "实际确认金额必须大于 0")
    private BigDecimal confirmedAmount;

    /**
     * 前端读取详情时获得的 application.version。
     *
     * <p>它不属于确认记录本身，而是交给成员三的状态流转 Service 做乐观锁校验。
     * 如果另一个管理员已经处理过申请，后端返回版本冲突，前端必须重新加载详情。</p>
     */
    @NotNull(message = "缺少申请版本号")
    @Min(value = 0, message = "申请版本号不合法")
    private Integer version;

    /**
     * 本次写操作的唯一请求号（建议 UUID）。
     *
     * <p>确认表会保存该值并建立唯一约束。网络重试携带同一个 requestId 时，
     * 后端可识别为同一次确认，避免重复生成确认记录或重复调用状态流转。</p>
     */
    @NotBlank(message = "缺少幂等请求号")
    @Size(max = 64, message = "幂等请求号长度不能超过 64")
    private String requestId;
}
