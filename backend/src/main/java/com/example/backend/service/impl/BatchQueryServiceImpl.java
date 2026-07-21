package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.BatchEligibleGradeMapper;
import com.example.backend.mapper.GreenChannelBatchMapper;
import com.example.backend.model.domain.BatchEligibleGrade;
import com.example.backend.model.domain.GreenChannelBatch;
import com.example.backend.model.dto.BatchSnapshot;
import com.example.backend.service.BatchQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 批次查询服务实现
 */
@Service
@RequiredArgsConstructor
public class BatchQueryServiceImpl implements BatchQueryService {

    private final GreenChannelBatchMapper greenChannelBatchMapper;
    private final BatchEligibleGradeMapper batchEligibleGradeMapper;

    @Override
    public BatchSnapshot getCurrentOpenGreenChannelBatch() {
        GreenChannelBatch batch = greenChannelBatchMapper.selectOne(
                new LambdaQueryWrapper<GreenChannelBatch>()
                        .eq(GreenChannelBatch::getStatus, "OPEN")
                        .eq(GreenChannelBatch::getEnabled, 1)
                        .eq(GreenChannelBatch::getDeleted, 0)
                        .orderByDesc(GreenChannelBatch::getCreateTime)
                        .last("LIMIT 1"));
        if (batch == null) {
            throw new IllegalStateException("当前没有开放的绿色通道批次");
        }
        return toSnapshot(batch);
    }

    @Override
    public BatchSnapshot getRequiredBatch(Long batchId) {
        GreenChannelBatch batch = greenChannelBatchMapper.selectById(batchId);
        if (batch == null || batch.getDeleted() != 0) {
            throw new IllegalArgumentException("批次不存在: id=" + batchId);
        }
        return toSnapshot(batch);
    }

    @Override
    public List<BatchSnapshot> listOpenBatches() {
        return greenChannelBatchMapper.selectList(
                new LambdaQueryWrapper<GreenChannelBatch>()
                        .eq(GreenChannelBatch::getStatus, "OPEN")
                        .eq(GreenChannelBatch::getEnabled, 1)
                        .eq(GreenChannelBatch::getDeleted, 0)
                        .orderByDesc(GreenChannelBatch::getCreateTime))
                .stream()
                .map(this::toSnapshot)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isGradeEligible(Long batchId, Long gradeId) {
        return batchEligibleGradeMapper.exists(
                new LambdaQueryWrapper<BatchEligibleGrade>()
                        .eq(BatchEligibleGrade::getBatchId, batchId)
                        .eq(BatchEligibleGrade::getGradeId, gradeId));
    }

    private BatchSnapshot toSnapshot(GreenChannelBatch batch) {
        List<Long> gradeIds = batchEligibleGradeMapper.selectList(
                        new LambdaQueryWrapper<BatchEligibleGrade>()
                                .eq(BatchEligibleGrade::getBatchId, batch.getId()))
                .stream()
                .map(BatchEligibleGrade::getGradeId)
                .collect(Collectors.toList());

        return BatchSnapshot.builder()
                .batchId(batch.getId())
                .batchCode(batch.getBatchCode())
                .batchName(batch.getBatchName())
                .batchType("GREEN_CHANNEL")
                .startTime(batch.getStartTime())
                .endTime(batch.getEndTime())
                .collegeDeadline(batch.getCollegeDeadline())
                .status(batch.getStatus())
                .eligibleGradeIds(gradeIds)
                .build();
    }
}
