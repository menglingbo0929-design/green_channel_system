package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.BatchEligibleGradeMapper;
import com.example.backend.mapper.GreenChannelBatchMapper;
import com.example.backend.mapper.SubsidyBatchMapper;
import com.example.backend.mapper.SubsidyBatchEligibleGradeMapper;
import com.example.backend.mapper.BatchFundingSourceMapper;
import com.example.backend.model.domain.BatchEligibleGrade;
import com.example.backend.model.domain.GreenChannelBatch;
import com.example.backend.model.domain.SubsidyBatch;
import com.example.backend.model.domain.SubsidyBatchEligibleGrade;
import com.example.backend.model.domain.BatchFundingSource;
import com.example.backend.model.dto.BatchSnapshot;
import com.example.backend.service.BatchQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 批次查询服务实现
 */
@Service
@RequiredArgsConstructor
public class BatchQueryServiceImpl implements BatchQueryService {

    private final GreenChannelBatchMapper greenChannelBatchMapper;
    private final BatchEligibleGradeMapper batchEligibleGradeMapper;
    private final SubsidyBatchMapper subsidyBatchMapper;
    private final SubsidyBatchEligibleGradeMapper subsidyEligibleGradeMapper;
    private final BatchFundingSourceMapper fundingSourceMapper;

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
        return getRequiredBatch("GREEN_CHANNEL", batchId);
    }

    @Override
    public BatchSnapshot getRequiredBatch(String batchType, Long batchId) {
        if ("SUBSIDY".equalsIgnoreCase(batchType)) {
            SubsidyBatch batch = subsidyBatchMapper.selectById(batchId);
            if (batch == null || batch.getDeleted() != 0) {
                throw new IllegalArgumentException("补助批次不存在: id=" + batchId);
            }
            return toSnapshot(batch);
        }
        GreenChannelBatch batch = greenChannelBatchMapper.selectById(batchId);
        if (batch == null || batch.getDeleted() != 0) {
            throw new IllegalArgumentException("绿色通道批次不存在: id=" + batchId);
        }
        return toSnapshot(batch);
    }

    @Override
    public List<BatchSnapshot> listOpenBatches() {
        return listOpenBatches("GREEN_CHANNEL");
    }

    @Override
    public List<BatchSnapshot> listOpenBatches(String batchType) {
        if ("SUBSIDY".equalsIgnoreCase(batchType)) {
            return subsidyBatchMapper.selectList(
                            new LambdaQueryWrapper<SubsidyBatch>()
                                    .eq(SubsidyBatch::getStatus, "OPEN")
                                    .eq(SubsidyBatch::getEnabled, 1)
                                    .eq(SubsidyBatch::getDeleted, 0)
                                    .orderByDesc(SubsidyBatch::getCreateTime))
                    .stream().map(this::toSnapshot).toList();
        }
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
    public List<BatchSnapshot> listAvailableBatches(String applicationType, Long gradeId) {
        if (gradeId == null || gradeId <= 0) return List.of();
        LocalDateTime now = LocalDateTime.now();
        if ("GREEN_CHANNEL".equalsIgnoreCase(applicationType)) {
            return listOpenBatches("GREEN_CHANNEL").stream()
                    .filter(batch -> !now.isBefore(batch.getStartTime()) && !now.isAfter(batch.getEndTime()))
                    .filter(batch -> batch.getEligibleGradeIds().contains(gradeId))
                    .toList();
        }
        return listOpenBatches("SUBSIDY").stream()
                .filter(batch -> applicationType.equalsIgnoreCase(batch.getApplicationType()))
                .filter(batch -> !now.isBefore(batch.getStartTime()) && !now.isAfter(batch.getEndTime()))
                .filter(batch -> batch.getEligibleGradeIds().contains(gradeId))
                .toList();
    }

    @Override
    public void validateStudentEligibility(String applicationType, Long batchId, Long gradeId) {
        boolean available = listAvailableBatches(applicationType, gradeId).stream()
                .anyMatch(batch -> batch.getBatchId().equals(batchId));
        if (!available) throw new IllegalArgumentException("批次不存在、未开放、已过申请期或当前年级不在适用范围内");
    }

    @Override
    public boolean isGradeEligible(Long batchId, Long gradeId) {
        return isGradeEligible("GREEN_CHANNEL", batchId, gradeId);
    }

    @Override
    public boolean isGradeEligible(String batchType, Long batchId, Long gradeId) {
        if ("SUBSIDY".equalsIgnoreCase(batchType)) {
            return subsidyEligibleGradeMapper.exists(
                    new LambdaQueryWrapper<SubsidyBatchEligibleGrade>()
                            .eq(SubsidyBatchEligibleGrade::getBatchId, batchId)
                            .eq(SubsidyBatchEligibleGrade::getGradeId, gradeId));
        }
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
                .applicationType("GREEN_CHANNEL")
                .academicYear(batch.getAcademicYear())
                .startTime(batch.getStartTime())
                .endTime(batch.getEndTime())
                .collegeDeadline(batch.getCollegeDeadline())
                .status(batch.getStatus())
                .enabled(batch.getEnabled())
                .eligibleGradeIds(gradeIds)
                .fundingSourceCodes(fundingSourceMapper.selectList(
                                new LambdaQueryWrapper<BatchFundingSource>()
                                        .eq(BatchFundingSource::getBatchId, batch.getId())
                                        .orderByAsc(BatchFundingSource::getId))
                        .stream().map(BatchFundingSource::getSourceCode).toList())
                .build();
    }

    private BatchSnapshot toSnapshot(SubsidyBatch batch) {
        List<Long> gradeIds = subsidyEligibleGradeMapper.selectList(
                        new LambdaQueryWrapper<SubsidyBatchEligibleGrade>()
                                .eq(SubsidyBatchEligibleGrade::getBatchId, batch.getId()))
                .stream().map(SubsidyBatchEligibleGrade::getGradeId).toList();
        return BatchSnapshot.builder()
                .batchId(batch.getId())
                .batchCode(batch.getBatchCode())
                .batchName(batch.getBatchName())
                .batchType("SUBSIDY")
                .applicationType(batch.getBatchType())
                .academicYear(batch.getAcademicYear())
                .startTime(batch.getStartTime())
                .endTime(batch.getEndTime())
                .status(batch.getStatus())
                .enabled(batch.getEnabled())
                .eligibleGradeIds(gradeIds)
                .fundingSourceCodes(List.of())
                .build();
    }
}
