package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.SubsidyBatchEligibleGradeMapper;
import com.example.backend.mapper.SubsidyBatchMapper;
import com.example.backend.model.domain.SubsidyBatch;
import com.example.backend.model.domain.SubsidyBatchEligibleGrade;
import com.example.backend.model.dto.CreateSubsidyBatchRequest;
import com.example.backend.model.dto.SubsidyBatchVO;
import com.example.backend.model.dto.UpdateSubsidyBatchRequest;
import com.example.backend.service.SubsidyBatchService;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubsidyBatchServiceImpl implements SubsidyBatchService {
    private final SubsidyBatchMapper batchMapper;
    private final SubsidyBatchEligibleGradeMapper eligibleGradeMapper;

    @Override
    public List<SubsidyBatchVO> list() {
        return batchMapper.selectList(new LambdaQueryWrapper<SubsidyBatch>()
                        .eq(SubsidyBatch::getDeleted, 0)
                        .orderByDesc(SubsidyBatch::getCreateTime))
                .stream().map(this::toView).toList();
    }

    @Override
    @Transactional
    public SubsidyBatchVO create(CreateSubsidyBatchRequest request) {
        validateTimes(request.getStartTime(), request.getEndTime());
        if (batchMapper.exists(new LambdaQueryWrapper<SubsidyBatch>()
                .eq(SubsidyBatch::getBatchCode, request.getBatchCode()).eq(SubsidyBatch::getDeleted, 0))) {
            throw new IllegalArgumentException("补助批次编号已存在");
        }
        SubsidyBatch batch = new SubsidyBatch();
        batch.setBatchCode(request.getBatchCode()); batch.setBatchName(request.getBatchName());
        batch.setAcademicYear(request.getAcademicYear()); batch.setBatchType(request.getBatchType());
        batch.setStartTime(request.getStartTime()); batch.setEndTime(request.getEndTime());
        batch.setStatus(request.getStatus() == null ? "DRAFT" : request.getStatus());
        batch.setEnabled(1); batch.setRemark(request.getRemark()); batchMapper.insert(batch);
        saveGrades(batch.getId(), request.getEligibleGradeIds());
        return toView(batch);
    }

    @Override
    @Transactional
    public SubsidyBatchVO update(Long id, UpdateSubsidyBatchRequest request) {
        SubsidyBatch batch = required(id);
        validateTimes(request.getStartTime(), request.getEndTime());
        batch.setBatchName(request.getBatchName()); batch.setAcademicYear(request.getAcademicYear());
        batch.setBatchType(request.getBatchType()); batch.setStartTime(request.getStartTime());
        batch.setEndTime(request.getEndTime()); batch.setRemark(request.getRemark());
        if (request.getStatus() != null) batch.setStatus(request.getStatus());
        batchMapper.updateById(batch); saveGrades(id, request.getEligibleGradeIds());
        return toView(batch);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        SubsidyBatch batch = required(id);
        batch.setEnabled(batch.getEnabled() == 1 ? 0 : 1);
        batchMapper.updateById(batch);
    }

    private SubsidyBatch required(Long id) {
        SubsidyBatch batch = batchMapper.selectById(id);
        if (batch == null || batch.getDeleted() != 0) throw new IllegalArgumentException("补助批次不存在");
        return batch;
    }
    private void validateTimes(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        if (!startTime.isBefore(endTime)) throw new IllegalArgumentException("申请开始时间必须早于申请截止时间");
    }
    private void saveGrades(Long batchId, List<Long> gradeIds) {
        eligibleGradeMapper.delete(new LambdaQueryWrapper<SubsidyBatchEligibleGrade>().eq(SubsidyBatchEligibleGrade::getBatchId, batchId));
        if (gradeIds == null) return;
        for (Long gradeId : new LinkedHashSet<>(gradeIds)) {
            if (gradeId == null || gradeId <= 0) continue;
            SubsidyBatchEligibleGrade grade = new SubsidyBatchEligibleGrade();
            grade.setBatchId(batchId); grade.setGradeId(gradeId); eligibleGradeMapper.insert(grade);
        }
    }
    private SubsidyBatchVO toView(SubsidyBatch batch) {
        List<Long> grades = eligibleGradeMapper.selectList(new LambdaQueryWrapper<SubsidyBatchEligibleGrade>()
                        .eq(SubsidyBatchEligibleGrade::getBatchId, batch.getId()))
                .stream().map(SubsidyBatchEligibleGrade::getGradeId).toList();
        return SubsidyBatchVO.builder().id(batch.getId()).batchCode(batch.getBatchCode()).batchName(batch.getBatchName())
                .academicYear(batch.getAcademicYear()).batchType(batch.getBatchType()).startTime(batch.getStartTime())
                .endTime(batch.getEndTime()).status(batch.getStatus()).enabled(batch.getEnabled()).remark(batch.getRemark())
                .eligibleGradeIds(grades).createTime(batch.getCreateTime()).build();
    }
}
