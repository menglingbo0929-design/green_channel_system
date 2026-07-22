package com.example.backend.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 欠费最终确认领域对象。
 *
 * <p>该类遵循 demo 的 MyBatis-Plus Entity 写法：
 * {@code @TableName} 指定表名、{@code @TableId} 指定主键、{@code @TableField} 指定列名，
 * Lombok 的 {@code @Data} 自动生成 getter/setter。</p>
 *
 * <p>该表是成员四独占维护的表。字段只覆盖 6.1.1 已明确的内容：申请关联、申报金额快照、
 * 实际确认金额、确认人、确认时间和单据编号；不加入未获确认的“单据状态”“备注”等字段。</p>
 */
@Data
@Accessors(chain = true)
@TableName("arrears_confirmation")
public class ArrearsConfirmation implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据库自增主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联统一申请主表 application 的 ID；同一申请只能有一条确认记录。 */
    @TableField("application_id")
    private Long applicationId;

    /** 学校最终确认后生成的唯一欠费单据编号。 */
    @TableField("voucher_no")
    private String voucherNo;

    /** 学生申请时的申报金额快照。 */
    @TableField("applied_amount")
    private BigDecimal appliedAmount;

    /** 学校核验后的实际欠费金额。 */
    @TableField("confirmed_amount")
    private BigDecimal confirmedAmount;

    /** 执行确认操作的学校管理员用户 ID。 */
    @TableField("confirm_user_id")
    private Long confirmUserId;

    /** 本次确认的幂等请求号；重复请求不得再写入第二条确认记录。 */
    @TableField("request_id")
    private String requestId;

    /** 最终确认的完成时间。 */
    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;

    /** 数据库格式规范要求的创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 数据库格式规范要求的更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 数据库格式规范要求的逻辑删除标记：0 表示有效；删除时写入本行 id。
     * Long 对应 MySQL 的 BIGINT UNSIGNED。
     */
    @TableField("deleted")
    @TableLogic
    private Long deleted;
}
