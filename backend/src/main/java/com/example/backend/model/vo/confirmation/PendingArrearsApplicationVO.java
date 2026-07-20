package com.example.backend.model.vo.confirmation;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 学校端待确认列表的一行数据。
 *
 * <p>VO 是“返回给前端展示的对象”。它只包含确认页面需要的申请摘要，
 * 不把申请实体、附件路径或其他内部字段直接暴露出去。</p>
 */
@Data
@Accessors(chain = true)
public class PendingArrearsApplicationVO {
    /** 统一申请主表 ID，确认、详情和状态流转都使用该值。 */
    private Long applicationId;
    /** 申请业务编号，用于学校管理员核对，不作为主键。 */
    private String applicationNo;
    /** 当前 application.version；POST 确认时以 JSON 字段 version 原样回传。 */
    private Integer version;
    /** 学生主键，仅用于权限和关联，不在页面上作为敏感标识展示。 */
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String collegeName;
    private String majorName;
    private String gradeName;
    private String className;
    /** 学生申报欠费金额快照；前端只能展示，不能修改。 */
    private BigDecimal appliedAmount;
    /** 仅应为 CONFIRM_PENDING；Port 读取层必须过滤其他状态。 */
    private String status;
}
