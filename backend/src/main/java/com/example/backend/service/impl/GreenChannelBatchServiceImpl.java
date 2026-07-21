package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.BatchEligibleGradeMapper;
import com.example.backend.mapper.GreenChannelBatchMapper;
import com.example.backend.model.domain.BatchEligibleGrade;
import com.example.backend.model.domain.GreenChannelBatch;
import com.example.backend.model.dto.BatchVO;
import com.example.backend.model.dto.CreateBatchRequest;
import com.example.backend.model.dto.UpdateBatchRequest;
import com.example.backend.service.GreenChannelBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GreenChannelBatchServiceImpl implements GreenChannelBatchService {

    private final GreenChannelBatchMapper batchMapper;
    private final BatchEligibleGradeMapper eligibleGradeMapper;

    @Override
    @Transactional
    public BatchVO create(CreateBatchRequest req) {
        GreenChannelBatch batch = new GreenChannelBatch();
        batch.setBatchCode(req.getBatchCode());
        batch.setBatchName(req.getBatchName());
        batch.setStartTime(req.getStartTime());
        batch.setEndTime(req.getEndTime());
        batch.setCollegeDeadline(req.getCollegeDeadline());
        batch.setStatus("DRAFT");
        batch.setEnabled(1);
        batch.setRemark(req.getRemark());
        batchMapper.insert(batch);

        if (req.getEligibleGradeIds() != null) {
            saveGrades(batch.getId(), req.getEligibleGradeIds());
        }
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
        batch.setStartTime(req.getStartTime());
        batch.setEndTime(req.getEndTime());
        batch.setCollegeDeadline(req.getCollegeDeadline());
        batch.setRemark(req.getRemark());
        batchMapper.updateById(batch);

        if (req.getEligibleGradeIds() != null) {
            saveGrades(id, req.getEligibleGradeIds());
        }
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
        for (Long gradeId : gradeIds) {
            BatchEligibleGrade eg = new BatchEligibleGrade();
            eg.setBatchId(batchId);
            eg.setGradeId(gradeId);
            eligibleGradeMapper.insert(eg);
        }
    }

    private BatchVO toVO(GreenChannelBatch b) {
        List<Long> gradeIds = eligibleGradeMapper.selectList(
                new LambdaQueryWrapper<BatchEligibleGrade>()
                        .eq(BatchEligibleGrade::getBatchId, b.getId()))
                .stream().map(BatchEligibleGrade::getGradeId)
                .collect(Collectors.toList());

        return BatchVO.builder()
                .id(b.getId())
                .batchCode(b.getBatchCode())
                .batchName(b.getBatchName())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .collegeDeadline(b.getCollegeDeadline())
                .status(b.getStatus())
                .enabled(b.getEnabled())
                .remark(b.getRemark())
                .eligibleGradeIds(gradeIds)
                .createTime(b.getCreateTime())
                .build();
    }
}
