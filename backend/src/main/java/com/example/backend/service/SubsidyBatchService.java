package com.example.backend.service;

import com.example.backend.model.dto.CreateSubsidyBatchRequest;
import com.example.backend.model.dto.SubsidyBatchVO;
import com.example.backend.model.dto.UpdateSubsidyBatchRequest;
import java.util.List;

public interface SubsidyBatchService {
    List<SubsidyBatchVO> list();
    SubsidyBatchVO create(CreateSubsidyBatchRequest request);
    SubsidyBatchVO update(Long id, UpdateSubsidyBatchRequest request);
    void toggleStatus(Long id);
}
