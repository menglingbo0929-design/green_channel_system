package com.example.backend.service;

import com.example.backend.model.dto.BatchSnapshot;

import java.util.List;

/**
 * 批次查询服务 —— 成员一向成员二、三、四提供
 *
 * 提供批次基本信息和年级资格校验，供申请创建、审核上报和统计筛选使用。
 */
public interface BatchQueryService {

    /**
     * 查询当前开放的绿色通道批次
     *
     * @return 当前 OPEN 状态的绿通批次
     * @throws IllegalStateException 没有开放的批次
     */
    BatchSnapshot getCurrentOpenGreenChannelBatch();

    /**
     * 查询批次详情（含适用年级列表）
     *
     * @param batchId 批次 ID
     * @return 批次快照
     * @throws IllegalArgumentException 批次不存在
     */
    BatchSnapshot getRequiredBatch(Long batchId);

    /** 按批次体系查询详情，避免绿色通道和补助批次 ID 重号时取错数据。 */
    BatchSnapshot getRequiredBatch(String batchType, Long batchId);

    /**
     * 判断某年级是否在批次的适用范围内
     */
    boolean isGradeEligible(Long batchId, Long gradeId);

    /** 查询所有启用且开放的批次（供前端下拉框） */
    List<BatchSnapshot> listOpenBatches();

    /** 按批次类型查询启用且开放的批次，供审批工作台筛选。 */
    List<BatchSnapshot> listOpenBatches(String batchType);

    /** 查询当前学生在申请时间内、且年级符合范围的可申请批次。 */
    List<BatchSnapshot> listAvailableBatches(String applicationType, Long gradeId);

    /** 创建学生申请前执行存在性、开放时间和年级资格的最终校验。 */
    void validateStudentEligibility(String applicationType, Long batchId, Long gradeId);
}
