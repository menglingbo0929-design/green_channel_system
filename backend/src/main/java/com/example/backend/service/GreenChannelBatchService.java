package com.example.backend.service;

import com.example.backend.model.dto.BatchVO;
import com.example.backend.model.dto.CreateBatchRequest;
import com.example.backend.model.dto.UpdateBatchRequest;

import java.util.List;

/**
 * 绿色通道批次管理服务 —— 成员一维护
 */
public interface GreenChannelBatchService {

    BatchVO create(CreateBatchRequest request);

    BatchVO update(Long id, UpdateBatchRequest request);

    List<BatchVO> list();

    BatchVO getDetail(Long id);

    void toggleStatus(Long id);

    void setEligibleGrades(Long id, List<Long> gradeIds);
}
