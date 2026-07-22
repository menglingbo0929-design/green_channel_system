package com.example.backend.application.service;

import com.example.backend.application.domain.*;
import com.example.backend.application.dto.*;
import com.example.backend.application.exception.ApplicationException;
import com.example.backend.application.mapper.ApplicationMapper;
import com.example.backend.application.mapper.ApplicationOperationMapper;
import com.example.backend.application.mapper.ArrearsApplicationMapper;
import com.example.backend.application.mapper.ApplicationResourceMapper;
import com.example.backend.mapper.StudentMapper;
import com.example.backend.service.BatchQueryService;
import com.example.backend.application.port.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Set;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService implements ApplicationCreationService, ApplicationStateQueryService,
        ApplicationStateWriteService, ApplicationDetailService, ReviewableApplicationEditService {
    private final ApplicationMapper applicationMapper;
    private final ApplicationOperationMapper operationMapper;
    private final ArrearsApplicationMapper arrearsMapper;
    private final ApplicationResourceMapper resourceMapper;
    private final StudentMapper studentMapper;
    private final BatchQueryService batchQueryService;
    private final RecommendationService recommendations;
    public ApplicationService(ApplicationMapper applicationMapper, ApplicationOperationMapper operationMapper, ArrearsApplicationMapper arrearsMapper, ApplicationResourceMapper resourceMapper, StudentMapper studentMapper, RecommendationService recommendations, BatchQueryService batchQueryService) {
        this.applicationMapper = applicationMapper; this.operationMapper = operationMapper; this.arrearsMapper = arrearsMapper; this.resourceMapper = resourceMapper;
        this.studentMapper = studentMapper; this.recommendations = recommendations; this.batchQueryService = batchQueryService;
    }

    @Override @Transactional
    public ApplicationStateSnapshot create(Long studentId, Long operatorId, ApplicationDraftCommand command) {
        return create(studentId, operatorId, command, ApplicationSource.STUDENT);
    }
    @Override @Transactional
    public ApplicationStateSnapshot createSchoolProxyApplication(Long studentId, Long operatorId, ApplicationDraftCommand command) {
        return create(studentId, operatorId, command, ApplicationSource.SCHOOL_PROXY);
    }
    /** 学校线下补录复用同一主表、唯一约束和 requestId 幂等逻辑。 */
    @Transactional public ApplicationStateSnapshot createSupplementApplication(Long studentId, Long operatorId, ApplicationDraftCommand command) {
        return create(studentId, operatorId, command, ApplicationSource.SUPPLEMENT);
    }
    private ApplicationStateSnapshot create(Long studentId, Long operatorId, ApplicationDraftCommand command, ApplicationSource source) {
        Long existingId = operationMapper.findApplicationIdByRequestId(command.requestId());
        if (existingId != null && existingId > 0) return getRequiredState(existingId);
        validateBatch(command.applicationType(), command.batchType());
        if (source == ApplicationSource.STUDENT) validateStudentBatch(studentId, command.applicationType(), command.batchId());
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
                a.getApplicationType(), a.getStatus(), a.getVersion(), a.getApplicationReason(), batchId(a))).toList();
    }
    public ApplicationSummary findOne(Long id) { Application a = required(id); return new ApplicationSummary(a.getId(), a.getApplicationNo(), a.getApplicationType(), a.getStatus(), a.getVersion(), a.getApplicationReason(), batchId(a)); }
    @Transactional public ApplicationSummary updateDraft(Long id, String reason, Integer version, Long operatorId) {
        if (applicationMapper.updateDraft(id, reason, version, operatorId) != 1) throw conflict("APPLICATION_VERSION_CONFLICT", "申请状态或版本已变化");
        return findOne(id);
    }
    @Transactional public void deleteDraft(Long id, Integer version, Long operatorId) {
        if (applicationMapper.deleteDraft(id, version, operatorId) != 1) throw conflict("APPLICATION_INVALID_STATUS", "仅未提交草稿可删除");
    }
    @Transactional public List<ArrearsItemSnapshot> replaceArrearsItems(Long applicationId, Integer version, List<ArrearsItemCommand> items, Long operatorId) {
        Application application = required(applicationId);
        if (application.getApplicationType() != ApplicationType.GREEN_CHANNEL ||
                (application.getStatus() != ApplicationStatus.DRAFT && !isReturned(application.getStatus()))) {
            throw new ApplicationException("APPLICATION_INVALID_STATUS", HttpStatus.CONFLICT, "仅绿色通道草稿或退回申请可维护欠费明细");
        }
        if (!application.getVersion().equals(version)) throw conflict("APPLICATION_VERSION_CONFLICT", "申请版本已变化");
        BigDecimal total = items.stream().map(ArrearsItemCommand::declaredAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(new BigDecimal("8000.00")) > 0) throw new ApplicationException("APPLICATION_ARREARS_AMOUNT_EXCEEDED", HttpStatus.BAD_REQUEST, "欠费申报总额不得超过 8000 元");
        if (items.stream().map(ArrearsItemCommand::feeItemId).distinct().count() != items.size()) throw new ApplicationException("APPLICATION_ARREARS_ITEM_DUPLICATE", HttpStatus.BAD_REQUEST, "欠费项目不可重复");
        arrearsMapper.deleteActiveByApplicationId(applicationId);
        for (ArrearsItemCommand item : items) {
            arrearsMapper.insert(applicationId, item.feeItemId(), item.declaredAmount(), deriveArrearsReasonCode(application.getStudentId()));
        }
        List<ArrearsItemSnapshot> storedItems = arrearsMapper.findItemsByApplicationId(applicationId);
        if (storedItems.size() != items.size()) throw new ApplicationException("APPLICATION_ARREARS_ITEM_INVALID", HttpStatus.BAD_REQUEST, "欠费项目不存在或已停用");
        if (applicationMapper.updateDraft(applicationId, application.getApplicationReason(), version, operatorId) != 1) throw conflict("APPLICATION_VERSION_CONFLICT", "申请版本已变化");
        return storedItems;
    }
    public List<ArrearsItemSnapshot> findArrearsItems(Long applicationId) { required(applicationId); return arrearsMapper.findItemsByApplicationId(applicationId); }
    @Transactional public List<GiftApplicationItemSnapshot> replaceGiftItems(Long applicationId, Integer version, List<GiftApplicationItemCommand> items, Long operatorId) {
        Application application = requireEditable(applicationId, ApplicationType.GREEN_CHANNEL, version);
        if (items.stream().map(GiftApplicationItemCommand::batchGiftItemId).distinct().count() != items.size()) {
            throw new ApplicationException("APPLICATION_GIFT_ITEM_DUPLICATE", HttpStatus.BAD_REQUEST, "礼包物品不可重复");
        }
        Long batchId = application.getGreenChannelBatchId();
        for (GiftApplicationItemCommand item : items) {
            if (resourceMapper.countValidBatchGiftItem(item.batchGiftItemId(), batchId, item.quantity()) != 1) {
                throw new ApplicationException("APPLICATION_GIFT_ITEM_INVALID", HttpStatus.BAD_REQUEST, "礼包物品不存在、不属于当前批次或超过单人上限");
            }
        }
        Long giftApplicationId = resourceMapper.findGiftApplicationId(applicationId);
        if (giftApplicationId == null) { resourceMapper.insertGiftApplication(applicationId); giftApplicationId = resourceMapper.lastInsertId(); }
        resourceMapper.deleteGiftItems(giftApplicationId);
        for (GiftApplicationItemCommand item : items) resourceMapper.insertGiftItem(giftApplicationId, item.batchGiftItemId(), item.quantity());
        touchDraft(application, version, operatorId);
        return resourceMapper.findGiftItems(applicationId);
    }
    public List<GiftApplicationItemSnapshot> findGiftItems(Long applicationId) { required(applicationId); return resourceMapper.findGiftItems(applicationId); }
    @Transactional public SubsidyApplicationSnapshot replaceSubsidy(Long applicationId, Integer version, BigDecimal expectedAmount, Long operatorId) {
        Application application = required(applicationId);
        if (application.getApplicationType() == ApplicationType.GREEN_CHANNEL) {
            throw new ApplicationException("APPLICATION_SUBSIDY_TYPE_INVALID", HttpStatus.BAD_REQUEST, "绿色通道申请不能维护补助金额");
        }
        requireEditable(applicationId, application.getApplicationType(), version);
        if (resourceMapper.findSubsidy(applicationId) == null) resourceMapper.insertSubsidy(applicationId, expectedAmount);
        else resourceMapper.updateSubsidy(applicationId, expectedAmount);
        touchDraft(application, version, operatorId);
        return resourceMapper.findSubsidy(applicationId);
    }
    public SubsidyApplicationSnapshot findSubsidy(Long applicationId) { required(applicationId); return resourceMapper.findSubsidy(applicationId); }
    @Override public ApplicationStateSnapshot getRequiredState(Long id) { return snapshot(required(id)); }
    @Override @Transactional public ApplicationStateSnapshot updateState(Long id, ApplicationStatus expected, ApplicationStatus target, ApprovalLevel level, Integer version, Long operatorId) {
        if (applicationMapper.updateState(id, expected, target, level, version, operatorId) != 1) throw conflict("APPLICATION_VERSION_CONFLICT", "申请状态或版本已变化");
        ApplicationStateSnapshot result = getRequiredState(id);
        if (target == ApplicationStatus.APPROVED
                || target == ApplicationStatus.CONFIRM_PENDING
                || target == ApplicationStatus.COMPLETED) {
            recommendations.generateForCompletedApplication(id);
        }
        return result;
    }
    @Override @Transactional public ApplicationStateSnapshot incrementReviewRoundAndUpdateState(Long id, ApplicationStatus expected, ApplicationStatus target, ApprovalLevel level, Integer version, Long operatorId) {
        if (applicationMapper.incrementReviewRoundAndUpdateState(id, expected, target, level, version, operatorId) != 1) throw conflict("APPLICATION_VERSION_CONFLICT", "申请状态或版本已变化");
        return getRequiredState(id);
    }
    @Override @Transactional public ApplicationStateSnapshot editForReview(Long applicationId, ReviewableApplicationEditCommand command, Long operatorId) {
        if (command == null || command.expectedVersion() == null || command.expectedVersion() < 0) {
            throw new ApplicationException("APPLICATION_VERSION_REQUIRED", HttpStatus.BAD_REQUEST, "审核编辑必须提供有效版本号");
        }
        Application application = required(applicationId);
        if (application.getStatus() != ApplicationStatus.COUNSELOR_PENDING
                && application.getStatus() != ApplicationStatus.COLLEGE_PENDING
                && application.getStatus() != ApplicationStatus.SCHOOL_PENDING) {
            throw new ApplicationException("APPLICATION_INVALID_STATUS", HttpStatus.CONFLICT, "仅审核中的申请可由审核人员编辑");
        }
        if (!application.getVersion().equals(command.expectedVersion())) throw conflict("APPLICATION_VERSION_CONFLICT", "申请版本已变化");
        if (command.arrearsItems() != null) replaceArrearsForReview(application, command.arrearsItems());
        if (command.giftItems() != null) replaceGiftsForReview(application, command.giftItems());
        if (command.expectedSubsidyAmount() != null) replaceSubsidyForReview(application, command.expectedSubsidyAmount());
        String reason = command.applicationReason() == null ? application.getApplicationReason() : command.applicationReason();
        if (applicationMapper.updateForReview(applicationId, reason, command.expectedVersion(), operatorId) != 1) {
            throw conflict("APPLICATION_VERSION_CONFLICT", "申请状态或版本已变化");
        }
        return getRequiredState(applicationId);
    }
    @Override public boolean containsArrears(Long applicationId) {
        required(applicationId); return arrearsMapper.countActiveByApplicationId(applicationId) > 0;
    }
    private Application required(Long id) { Application a = applicationMapper.findRequired(id); if (a == null) throw new ApplicationException("APPLICATION_NOT_FOUND", HttpStatus.NOT_FOUND, "申请不存在"); return a; }
    private Application requireEditable(Long id, ApplicationType type, Integer version) {
        Application application = required(id);
        if (application.getApplicationType() != type || (application.getStatus() != ApplicationStatus.DRAFT && !isReturned(application.getStatus()))) {
            throw new ApplicationException("APPLICATION_INVALID_STATUS", HttpStatus.CONFLICT, "当前申请不能维护此类明细");
        }
        if (!application.getVersion().equals(version)) throw conflict("APPLICATION_VERSION_CONFLICT", "申请版本已变化");
        return application;
    }
    private void touchDraft(Application application, Integer version, Long operatorId) {
        if (applicationMapper.updateDraft(application.getId(), application.getApplicationReason(), version, operatorId) != 1) {
            throw conflict("APPLICATION_VERSION_CONFLICT", "申请版本已变化");
        }
    }
    private void replaceArrearsForReview(Application application, List<ArrearsItemCommand> items) {
        if (application.getApplicationType() != ApplicationType.GREEN_CHANNEL) throw new ApplicationException("APPLICATION_TYPE_INVALID", HttpStatus.BAD_REQUEST, "仅绿色通道申请可维护欠费明细");
        validateArrearsItems(items);
        arrearsMapper.deleteActiveByApplicationId(application.getId());
        for (ArrearsItemCommand item : items) arrearsMapper.insert(application.getId(), item.feeItemId(), item.declaredAmount(), deriveArrearsReasonCode(application.getStudentId()));
        if (arrearsMapper.findItemsByApplicationId(application.getId()).size() != items.size()) throw new ApplicationException("APPLICATION_ARREARS_ITEM_INVALID", HttpStatus.BAD_REQUEST, "欠费项目不存在或已停用");
    }
    private void replaceGiftsForReview(Application application, List<GiftApplicationItemCommand> items) {
        if (application.getApplicationType() != ApplicationType.GREEN_CHANNEL) throw new ApplicationException("APPLICATION_TYPE_INVALID", HttpStatus.BAD_REQUEST, "仅绿色通道申请可维护礼包明细");
        if (items.stream().map(GiftApplicationItemCommand::batchGiftItemId).distinct().count() != items.size()) throw new ApplicationException("APPLICATION_GIFT_ITEM_DUPLICATE", HttpStatus.BAD_REQUEST, "礼包物品不可重复");
        for (GiftApplicationItemCommand item : items) if (resourceMapper.countValidBatchGiftItem(item.batchGiftItemId(), application.getGreenChannelBatchId(), item.quantity()) != 1) throw new ApplicationException("APPLICATION_GIFT_ITEM_INVALID", HttpStatus.BAD_REQUEST, "礼包物品无效或超过限额");
        Long giftApplicationId = resourceMapper.findGiftApplicationId(application.getId());
        if (giftApplicationId == null) { resourceMapper.insertGiftApplication(application.getId()); giftApplicationId = resourceMapper.lastInsertId(); }
        resourceMapper.deleteGiftItems(giftApplicationId);
        for (GiftApplicationItemCommand item : items) resourceMapper.insertGiftItem(giftApplicationId, item.batchGiftItemId(), item.quantity());
    }
    private void replaceSubsidyForReview(Application application, BigDecimal amount) {
        if (application.getApplicationType() == ApplicationType.GREEN_CHANNEL) throw new ApplicationException("APPLICATION_SUBSIDY_TYPE_INVALID", HttpStatus.BAD_REQUEST, "绿色通道申请不能维护补助金额");
        if (amount.signum() <= 0) throw new ApplicationException("APPLICATION_SUBSIDY_AMOUNT_INVALID", HttpStatus.BAD_REQUEST, "补助金额必须大于零");
        if (resourceMapper.findSubsidy(application.getId()) == null) resourceMapper.insertSubsidy(application.getId(), amount); else resourceMapper.updateSubsidy(application.getId(), amount);
    }
    private void validateArrearsItems(List<ArrearsItemCommand> items) {
        BigDecimal total = items.stream().map(ArrearsItemCommand::declaredAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(new BigDecimal("8000.00")) > 0) throw new ApplicationException("APPLICATION_ARREARS_AMOUNT_EXCEEDED", HttpStatus.BAD_REQUEST, "欠费申报总额不得超过 8000 元");
        if (items.stream().map(ArrearsItemCommand::feeItemId).distinct().count() != items.size()) throw new ApplicationException("APPLICATION_ARREARS_ITEM_DUPLICATE", HttpStatus.BAD_REQUEST, "欠费项目不可重复");
    }
    private void validateBatch(ApplicationType type, BatchType batchType) {
        if ((type == ApplicationType.GREEN_CHANNEL) != (batchType == BatchType.GREEN_CHANNEL)) throw new ApplicationException("APPLICATION_BATCH_TYPE_INVALID", HttpStatus.BAD_REQUEST, "申请类型与批次类型不匹配");
    }
    private void validateStudentBatch(Long studentId, ApplicationType type, Long batchId) {
        var student = studentMapper.selectById(studentId);
        if (student == null || student.getDeleted() != 0 || student.getEnabled() != 1) {
            throw new ApplicationException("APPLICATION_STUDENT_INVALID", HttpStatus.BAD_REQUEST, "当前学生档案不存在或已停用");
        }
        try {
            batchQueryService.validateStudentEligibility(type.name(), batchId, student.getGradeId());
        } catch (IllegalArgumentException ex) {
            throw new ApplicationException("APPLICATION_BATCH_UNAVAILABLE", HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
    private boolean isReturned(ApplicationStatus status) {
        return status == ApplicationStatus.COUNSELOR_RETURNED
                || status == ApplicationStatus.COLLEGE_RETURNED
                || status == ApplicationStatus.SCHOOL_RETURNED;
    }
    private String deriveArrearsReasonCode(Long studentId) {
        var student = studentMapper.selectById(studentId);
        if (student != null && (Integer.valueOf(1).equals(student.getOriginLoan()) || Integer.valueOf(1).equals(student.getCampusLoan()))) return "FAMILY_FINANCIAL_DIFFICULTY";
        return "OTHER";
    }
    private String nextApplicationNo() { return "GC" + LocalDate.now().toString().replace("-", "") + String.format("%06d", System.nanoTime() % 1_000_000); }
    private Long batchId(Application a) { return a.getBatchType() == BatchType.GREEN_CHANNEL ? a.getGreenChannelBatchId() : a.getSubsidyBatchId(); }
    private ApplicationStateSnapshot snapshot(Application a) { return new ApplicationStateSnapshot(a.getId(), a.getStudentId(), a.getBatchType(), batchId(a), a.getApplicationType(), a.getStatus(), a.getCurrentLevel(), a.getReviewRound(), a.getVersion()); }
    private ApplicationException conflict(String code, String message) { return new ApplicationException(code, HttpStatus.CONFLICT, message); }
}
