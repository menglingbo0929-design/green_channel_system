package com.example.backend.model.dto.statistics;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 6.1.6 学校统计筛选条件。
 *
 * <p>该对象仅描述查询条件，不携带用户、角色、学院数据范围等身份信息；这些数据必须由
 * 成员一的可信登录上下文提供。所有字段为空时，表示查询当前学校权限范围内的全部最终状态数据。</p>
 */
@Data
public class StatisticsFilterDTO {
    /** 批次所属体系：GREEN_CHANNEL 或 SUBSIDY；batchId 有值时必填。 */
    private String batchType;
    /** 批次主键；与 batchType 一起定位当前或历史批次。 */
    private Long batchId;
    /** 以下四项为成员一维护的组织主键，只接受 ID，不接受前端名称。 */
    private Long collegeId;
    private Long majorId;
    private Long gradeId;
    private Long classId;
    /** GREEN_CHANNEL、LIVING_SUBSIDY 或 TRAVEL_SUBSIDY。 */
    private String applicationType;
    /** 只允许 APPROVED 或 COMPLETED；为空时同时统计两个最终状态。 */
    private String applicationStatus;
    /** 成员二维护的欠费项目主键。 */
    private Long feeItemId;
    /** 申请创建时间闭区间的起点。 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime applicationStartTime;
    /** 申请创建时间闭区间的终点。 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime applicationEndTime;
}
