package com.example.backend.application.service;

import com.example.backend.application.domain.*;
import com.example.backend.application.dto.*;
import com.example.backend.application.exception.ApplicationException;
import com.example.backend.application.mapper.ApplicationMapper;
import com.example.backend.application.mapper.ApplicationOperationMapper;
import com.example.backend.application.mapper.ArrearsApplicationMapper;
import com.example.backend.application.port.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService implements ApplicationCreationService, ApplicationStateQueryService,
        ApplicationStateWriteService, ApplicationDetailService {
    private final ApplicationMapper applicationMapper;
    private final ApplicationOperationMapper operationMapper;
    private final ArrearsApplicationMapper arrearsMapper;
    public ApplicationService(ApplicationMapper applicationMapper, ApplicationOperationMapper operationMapper, ArrearsApplicationMapper arrearsMapper) {
        this.applicationMapper = applicationMapper; this.operationMapper = operationMapper; this.arrearsMapper = arrearsMapper;
    }

    @Override @Transactional
    public ApplicationStateSnapshot create(Long studentId, Long operatorId, ApplicationDraftCommand command) {
        return create(studentId, operatorId, command, ApplicationSource.STUDENT);
    }
    @Override @Transactional
    public ApplicationStateSnapshot createSchoolProxyApplication(Long studentId, Long operatorId, ApplicationDraftCommand command) {
        return create(studentId, operatorId, command, ApplicationSource.SCHOOL_PROXY);
    }
    private ApplicationStateSnapshot create(Long studentId, Long operatorId, ApplicationDraftCommand command, ApplicationSource source) {
        Long existingId = operationMapper.findApplicationIdByRequestId(command.requestId());
        if (existingId != null && existingId > 0) return getRequiredState(existingId);
        validateBatch(command.applicationType(), command.batchType());
        if (applicationMapper.findActiveByUnique(studentId, command.applicationType(), command.batchType(), command.batchId()) != null) {
            throw conflict("APPLICATION_ALREADY_EXISTS", "同一学生、批次和申请类型只能保留一条有效申请");
        }
        Application application = new Application();
        application.setApplicationNo(nextApplicationNo()); application.setStudentId(studentId);
        application.setApplicationType(command.applicationType()); application.setSource(source);
        application.setBatchType(command.batchType());
        if (command.batchType() == BatchType.GREEN_CHANNEL) application.setGreenChannelBatchId(command.batchId());
        else application.setSubsidyBatchId(command.batchId());
        application.setStatus(ApplicationStatus.DRAFT); application.setCurrentLevel(ApprovalLevel.STUDENT);
        application.setReviewRound(0); application.setVersion(0); application.setApplicationReason(command.applicationReason());
        application.setCreateBy(operatorId); application.setUpdateBy(operatorId); applicationMapper.insert(application);
        operationMapper.insert(application.getId(), "CREATE_DRAFT", command.requestId(), operatorId);
        return snapshot(application);
    }

    public List<ApplicationSummary> findMine(Long studentId) {
        return applicationMapper.findMine(studentId).stream().map(a -> new ApplicationSummary(a.getId(), a.getApplicationNo(),
                a.getApplicationType(), a.getStatus(), a.getVersion(), a.getApplicationReason())).toList();
    }
    public ApplicationSummary findOne(Long id) { Application a = required(id); return new ApplicationSummary(a.getId(), a.getApplicationNo(), a.getApplicationType(), a.getStatus(), a.getVersion(), a.getApplicationReason()); }
    @Transactional public ApplicationSummary updateDraft(Long id, String reason, Integer version, Long operatorId) {
        if (applicationMapper.updateDraft(id, reason, version, operatorId) != 1) throw conflict("APPLICATION_VERSION_CONFLICT", "申请状态或版本已变化");
        return findOne(id);
    }
    @Transactional public void deleteDraft(Long id, Integer version, Long operatorId) {
        if (applicationMapper.deleteDraft(id, version, operatorId) != 1) throw conflict("APPLICATION_INVALID_STATUS", "仅未提交草稿可删除");
    }
    @Override public ApplicationStateSnapshot getRequiredState(Long id) { return snapshot(required(id)); }
    @Override @Transactional public ApplicationStateSnapshot updateState(Long id, ApplicationStatus expected, ApplicationStatus target, ApprovalLevel level, Integer version, Long operatorId) {
        if (applicationMapper.updateState(id, expected, target, level, version, operatorId) != 1) throw conflict("APPLICATION_VERSION_CONFLICT", "申请状态或版本已变化");
        return getRequiredState(id);
    }
    @Override @Transactional public ApplicationStateSnapshot incrementReviewRoundAndUpdateState(Long id, ApplicationStatus expected, ApplicationStatus target, ApprovalLevel level, Integer version, Long operatorId) {
        if (applicationMapper.incrementReviewRoundAndUpdateState(id, expected, target, level, version, operatorId) != 1) throw conflict("APPLICATION_VERSION_CONFLICT", "申请状态或版本已变化");
        return getRequiredState(id);
    }
    @Override public boolean containsArrears(Long applicationId) {
        required(applicationId); return arrearsMapper.countActiveByApplicationId(applicationId) > 0;
    }
    private Application required(Long id) { Application a = applicationMapper.findRequired(id); if (a == null) throw new ApplicationException("APPLICATION_NOT_FOUND", HttpStatus.NOT_FOUND, "申请不存在"); return a; }
    private void validateBatch(ApplicationType type, BatchType batchType) {
        if ((type == ApplicationType.GREEN_CHANNEL) != (batchType == BatchType.GREEN_CHANNEL)) throw new ApplicationException("APPLICATION_BATCH_TYPE_INVALID", HttpStatus.BAD_REQUEST, "申请类型与批次类型不匹配");
    }
    private String nextApplicationNo() { return "GC" + LocalDate.now().toString().replace("-", "") + String.format("%06d", System.nanoTime() % 1_000_000); }
    private ApplicationStateSnapshot snapshot(Application a) { Long batchId = a.getBatchType() == BatchType.GREEN_CHANNEL ? a.getGreenChannelBatchId() : a.getSubsidyBatchId(); return new ApplicationStateSnapshot(a.getId(), a.getStudentId(), a.getBatchType(), batchId, a.getApplicationType(), a.getStatus(), a.getCurrentLevel(), a.getReviewRound(), a.getVersion()); }
    private ApplicationException conflict(String code, String message) { return new ApplicationException(code, HttpStatus.CONFLICT, message); }
}
