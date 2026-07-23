package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.model.domain.BatchType;
import com.example.backend.service.ApprovalBatchQueryService;
import com.example.backend.mapper.GreenChannelBatchMapper;
import com.example.backend.mapper.SubsidyBatchMapper;
import com.example.backend.model.domain.GreenChannelBatch;
import com.example.backend.model.domain.SubsidyBatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 审批批次查询实现 —— 成员一
 *
 * 支持 GREEN_CHANNEL 和 SUBSIDY 两种批次类型的校验和快照查询。
 * 成员三通过此服务获取批次时间信息，不直接读批次表。
 */
@Service
@RequiredArgsConstructor
public class ApprovalBatchQueryServiceImpl implements ApprovalBatchQueryService {

    private final GreenChannelBatchMapper greenChannelBatchMapper;
    private final SubsidyBatchMapper subsidyBatchMapper;

    @Override
    public ApprovalBatchSnapshot getRequiredBatch(BatchType batchType, Long batchId) {
        return switch (batchType) {
            case GREEN_CHANNEL -> fromGreenChannel(batchId);
            case SUBSIDY -> fromSubsidy(batchId);
        };
    }

    private ApprovalBatchSnapshot fromGreenChannel(Long batchId) {
        GreenChannelBatch b = greenChannelBatchMapper.selectById(batchId);
        if (b == null || b.getDeleted() != 0) {
            throw new IllegalArgumentException("绿通批次不存在: id=" + batchId);
        }
        return new ApprovalBatchSnapshot(
                BatchType.GREEN_CHANNEL,
                b.getId(),
                "OPEN".equals(b.getStatus()) && b.getEnabled() == 1,
                b.getEndTime(),
                b.getCollegeDeadline()
        );
    }

    private ApprovalBatchSnapshot fromSubsidy(Long batchId) {
        SubsidyBatch b = subsidyBatchMapper.selectById(batchId);
        if (b == null || b.getDeleted() != 0) {
            throw new IllegalArgumentException("补助批次不存在: id=" + batchId);
        }
        return new ApprovalBatchSnapshot(
                BatchType.SUBSIDY,
                b.getId(),
                "OPEN".equals(b.getStatus()) && b.getEnabled() == 1,
                b.getEndTime(),
                null  // 补助批次无学院截止时间
        );
    }
}
