package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.BatchEligibleGradeMapper;
import com.example.backend.mapper.BatchFundingSourceMapper;
import com.example.backend.mapper.GreenChannelBatchMapper;
import com.example.backend.model.domain.BatchEligibleGrade;
import com.example.backend.model.domain.BatchFundingSource;
import com.example.backend.model.domain.GreenChannelBatch;
import com.example.backend.model.dto.BatchVO;
import com.example.backend.model.dto.CreateBatchRequest;
import com.example.backend.model.dto.UpdateBatchRequest;
import com.example.backend.service.GreenChannelBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GreenChannelBatchServiceImpl implements GreenChannelBatchService {

    private final GreenChannelBatchMapper batchMapper;
    private final BatchEligibleGradeMapper eligibleGradeMapper;
    private final BatchFundingSourceMapper fundingSourceMapper;

    @Override
    @Transactional
    public BatchVO create(CreateBatchRequest req) {
        GreenChannelBatch batch = new GreenChannelBatch();
        batch.setBatchCode(req.getBatchCode());
        batch.setBatchName(req.getBatchName());
        batch.setAcademicYear(req.getAcademicYear());
        batch.setStartTime(req.getStartTime());
        batch.setEndTime(req.getEndTime());
        batch.setCollegeDeadline(req.getCollegeDeadline());
        batch.setStatus(req.getStatus() == null ? "DRAFT" : req.getStatus());
        batch.setEnabled(1);
        batch.setRemark(req.getRemark());
        batchMapper.insert(batch);

        validateBatchTimes(batch.getStartTime(), batch.getEndTime(), batch.getCollegeDeadline());
        saveGrades(batch.getId(), req.getEligibleGradeIds());
        saveFundingSources(batch.getId(), req.getFundingSourceCodes());
        return toVO(batch);
    }

    @Override
    @Transactional
    public BatchVO update(Long id, UpdateBatchRequest req) {
        GreenChannelBatch batch = batchMapper.selectById(id);
        if (batch == null || batch.getDeleted() != 0) {
            throw new IllegalArgumentException("批次不存在");
        }
        batch.setBatchName(req.getBatchName());
        batch.setAcademicYear(req.getAcademicYear());
        batch.setStartTime(req.getStartTime());
        batch.setEndTime(req.getEndTime());
        batch.setCollegeDeadline(req.getCollegeDeadline());
        batch.setRemark(req.getRemark());
        if (req.getStatus() != null) batch.setStatus(req.getStatus());
        validateBatchTimes(batch.getStartTime(), batch.getEndTime(), batch.getCollegeDeadline());
        batchMapper.updateById(batch);

        saveGrades(id, req.getEligibleGradeIds());
        saveFundingSources(id, req.getFundingSourceCodes());
        return toVO(batch);
    }

    @Override
    public List<BatchVO> list() {
        return batchMapper.selectList(
                new LambdaQueryWrapper<GreenChannelBatch>()
                        .eq(GreenChannelBatch::getDeleted, 0)
                        .orderByDesc(GreenChannelBatch::getCreateTime))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public BatchVO getDetail(Long id) {
        GreenChannelBatch batch = batchMapper.selectById(id);
        if (batch == null || batch.getDeleted() != 0) {
            throw new IllegalArgumentException("批次不存在");
        }
        return toVO(batch);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        GreenChannelBatch batch = batchMapper.selectById(id);
        if (batch == null || batch.getDeleted() != 0) {
            throw new IllegalArgumentException("批次不存在");
        }
        batch.setEnabled(batch.getEnabled() == 1 ? 0 : 1);
        batchMapper.updateById(batch);
    }

    @Override
    @Transactional
    public void setEligibleGrades(Long id, List<Long> gradeIds) {
        GreenChannelBatch batch = batchMapper.selectById(id);
        if (batch == null || batch.getDeleted() != 0) {
            throw new IllegalArgumentException("批次不存在");
        }
        saveGrades(id, gradeIds);
    }

    private void saveGrades(Long batchId, List<Long> gradeIds) {
        eligibleGradeMapper.delete(
                new LambdaQueryWrapper<BatchEligibleGrade>()
                        .eq(BatchEligibleGrade::getBatchId, batchId));
        if (gradeIds == null) return;
        for (Long gradeId : new LinkedHashSet<>(gradeIds)) {
            if (gradeId == null || gradeId <= 0) continue;
            BatchEligibleGrade eg = new BatchEligibleGrade();
            eg.setBatchId(batchId);
            eg.setGradeId(gradeId);
            eligibleGradeMapper.insert(eg);
        }
    }

    private void saveFundingSources(Long batchId, List<String> sourceCodes) {
        fundingSourceMapper.delete(new LambdaQueryWrapper<BatchFundingSource>()
                .eq(BatchFundingSource::getBatchId, batchId));
        if (sourceCodes == null) return;
        sourceCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .distinct()
                .forEach(code -> {
                    BatchFundingSource source = new BatchFundingSource();
                    source.setBatchId(batchId);
                    source.setSourceCode(code);
                    fundingSourceMapper.insert(source);
                });
    }

    private void validateBatchTimes(java.time.LocalDateTime startTime,
                                    java.time.LocalDateTime endTime,
                                    java.time.LocalDateTime collegeDeadline) {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("申请开始时间必须早于申请截止时间");
        }
        if (collegeDeadline.isBefore(endTime)) {
            throw new IllegalArgumentException("学院上报截止时间不得早于学生申请截止时间");
        }
    }

    private BatchVO toVO(GreenChannelBatch b) {
        List<Long> gradeIds = eligibleGradeMapper.selectList(
                new LambdaQueryWrapper<BatchEligibleGrade>()
                        .eq(BatchEligibleGrade::getBatchId, b.getId()))
                .stream().map(BatchEligibleGrade::getGradeId)
                .collect(Collectors.toList());
        List<String> fundingSourceCodes = fundingSourceMapper.selectList(
                        new LambdaQueryWrapper<BatchFundingSource>()
                                .eq(BatchFundingSource::getBatchId, b.getId())
                                .orderByAsc(BatchFundingSource::getId))
                .stream().map(BatchFundingSource::getSourceCode)
                .toList();

        return BatchVO.builder()
                .id(b.getId())
                .batchCode(b.getBatchCode())
                .batchName(b.getBatchName())
                .academicYear(b.getAcademicYear())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .collegeDeadline(b.getCollegeDeadline())
                .status(b.getStatus())
                .enabled(b.getEnabled())
                .remark(b.getRemark())
                .eligibleGradeIds(gradeIds)
                .fundingSourceCodes(fundingSourceCodes)
                .createTime(b.getCreateTime())
                .build();
    }
}
