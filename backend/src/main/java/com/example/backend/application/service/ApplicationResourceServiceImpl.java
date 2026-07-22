package com.example.backend.application.service;

import com.example.backend.application.domain.Application;
import com.example.backend.application.domain.ApplicationType;
import com.example.backend.application.exception.ApplicationException;
import com.example.backend.application.mapper.ApplicationMapper;
import com.example.backend.application.mapper.ApplicationOperationMapper;
import com.example.backend.application.mapper.ApplicationResourceMapper;
import com.example.backend.approval.port.ApprovalResourceService;
import com.example.backend.model.dto.StudentApplicationProfile;
import com.example.backend.service.StudentProfileQueryService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns the member-two resource ledger. Every counter change is conditional in SQL,
 * so concurrent submissions cannot over-reserve stock or quotas. */
@Service
public class ApplicationResourceServiceImpl implements ApprovalResourceService {
    private final ApplicationMapper applications;
    private final ApplicationResourceMapper resources;
    private final ApplicationOperationMapper operations;
    private final StudentProfileQueryService studentProfiles;

    public ApplicationResourceServiceImpl(ApplicationMapper applications, ApplicationResourceMapper resources,
                                          ApplicationOperationMapper operations, StudentProfileQueryService studentProfiles) {
        this.applications = applications;
        this.resources = resources;
        this.operations = operations;
        this.studentProfiles = studentProfiles;
    }

    @Override
    @Transactional
    public void reserveOnSubmit(Long applicationId, String requestId, Long operatorId) {
        if (alreadyProcessed(applicationId, "RESERVE_SUBMIT", requestId)) return;
        Application application = required(applicationId);
        StudentApplicationProfile student = studentProfiles.getRequiredProfile(application.getStudentId());
        if (application.getApplicationType() == ApplicationType.GREEN_CHANNEL) reserveGifts(application, student);
        else reserveSubsidy(application, student, requiredSubsidy(applicationId).expectedAmount());
        record(applicationId, "RESERVE_SUBMIT", requestId, operatorId);
    }

    @Override
    @Transactional
    public void applyCounselorSubsidyAmount(Long applicationId, BigDecimal amount, String requestId, Long operatorId) {
        if (alreadyProcessed(applicationId, "ADJUST_SUBSIDY", requestId)) return;
        if (amount == null || amount.signum() <= 0) throw invalid("APPLICATION_SUBSIDY_AMOUNT_INVALID", "最终补助金额必须大于零");
        Application application = required(applicationId);
        if (application.getApplicationType() == ApplicationType.GREEN_CHANNEL) {
            throw invalid("APPLICATION_SUBSIDY_TYPE_INVALID", "绿色通道申请不能确认补助金额");
        }
        var subsidy = requiredSubsidy(applicationId);
        StudentApplicationProfile student = studentProfiles.getRequiredProfile(application.getStudentId());
        BigDecimal delta = amount.subtract(subsidy.expectedAmount());
        if (delta.signum() > 0) reserveSubsidy(application, student, delta);
        if (delta.signum() < 0) releaseSubsidyReservation(application, student, delta.negate());
        resources.updateSubsidyFinalAmount(applicationId, amount);
        record(applicationId, "ADJUST_SUBSIDY", requestId, operatorId);
    }

    @Override
    public void validateCollegeApproval(Long applicationId) {
        required(applicationId);
    }

    @Override
    @Transactional
    public void confirmOnSchoolApproval(Long applicationId, String requestId, Long operatorId) {
        if (alreadyProcessed(applicationId, "CONFIRM_RESOURCE", requestId)) return;
        Application application = required(applicationId);
        StudentApplicationProfile student = studentProfiles.getRequiredProfile(application.getStudentId());
        if (application.getApplicationType() == ApplicationType.GREEN_CHANNEL) confirmGifts(application, student);
        else confirmSubsidy(application, student, effectiveSubsidyAmount(applicationId));
        record(applicationId, "CONFIRM_RESOURCE", requestId, operatorId);
    }

    @Override
    public void handleReturn(Long applicationId, String requestId, Long operatorId) {
        // Approved rule: reservations remain held during a returned review round.
        required(applicationId);
    }

    @Override
    @Transactional
    public void releaseOnReject(Long applicationId, String requestId, Long operatorId) {
        releaseReservation(applicationId, requestId, operatorId, "RELEASE_REJECT");
    }

    @Override
    @Transactional
    public void releaseOnCancel(Long applicationId, String requestId, Long operatorId) {
        if (alreadyProcessed(applicationId, "RELEASE_CANCEL", requestId)) return;
        Application application = required(applicationId);
        StudentApplicationProfile student = studentProfiles.getRequiredProfile(application.getStudentId());
        if (application.getApplicationType() == ApplicationType.GREEN_CHANNEL) releaseGiftUsage(application, student);
        else releaseSubsidyUsage(application, student, effectiveSubsidyAmount(applicationId));
        record(applicationId, "RELEASE_CANCEL", requestId, operatorId);
    }

    private void reserveGifts(Application application, StudentApplicationProfile student) {
        List<com.example.backend.application.dto.GiftApplicationItemSnapshot> items = resources.findGiftItems(application.getId());
        for (var item : items) require(resources.reserveGiftStock(item.batchGiftItemId(), item.quantity()), "APPLICATION_GIFT_STOCK_INSUFFICIENT");
        int total = items.stream().mapToInt(item -> item.quantity()).sum();
        if (total == 0) return;
        Long batchId = application.getGreenChannelBatchId();
        require(resources.reserveCollegeGiftQuota(batchId, student.getCollegeId(), total), "APPLICATION_COLLEGE_QUOTA_INSUFFICIENT");
        require(resources.reserveGradeGiftQuota(batchId, student.getGradeId(), total), "APPLICATION_GRADE_QUOTA_INSUFFICIENT");
    }

    private void confirmGifts(Application application, StudentApplicationProfile student) {
        for (var item : resources.findGiftItems(application.getId())) require(resources.confirmGiftStock(item.batchGiftItemId(), item.quantity()), "APPLICATION_RESOURCE_CONFIRM_CONFLICT");
        int total = giftQuantity(application.getId());
        if (total == 0) return;
        Long batchId = application.getGreenChannelBatchId();
        require(resources.confirmCollegeGiftQuota(batchId, student.getCollegeId(), total), "APPLICATION_RESOURCE_CONFIRM_CONFLICT");
        require(resources.confirmGradeGiftQuota(batchId, student.getGradeId(), total), "APPLICATION_RESOURCE_CONFIRM_CONFLICT");
    }

    private void reserveSubsidy(Application application, StudentApplicationProfile student, BigDecimal amount) {
        Long batchId = application.getSubsidyBatchId();
        require(resources.reserveCollegeSubsidyQuota(batchId, student.getCollegeId(), amount), "APPLICATION_SUBSIDY_QUOTA_INSUFFICIENT");
        require(resources.reserveGradeSubsidyQuota(batchId, student.getGradeId(), amount), "APPLICATION_SUBSIDY_QUOTA_INSUFFICIENT");
    }
    private void releaseSubsidyReservation(Application application, StudentApplicationProfile student, BigDecimal amount) {
        Long batchId = application.getSubsidyBatchId();
        require(resources.releaseCollegeSubsidyReservation(batchId, student.getCollegeId(), amount), "APPLICATION_RESOURCE_RELEASE_CONFLICT");
        require(resources.releaseGradeSubsidyReservation(batchId, student.getGradeId(), amount), "APPLICATION_RESOURCE_RELEASE_CONFLICT");
    }
    private void confirmSubsidy(Application application, StudentApplicationProfile student, BigDecimal amount) {
        Long batchId = application.getSubsidyBatchId();
        require(resources.confirmCollegeSubsidyQuota(batchId, student.getCollegeId(), amount), "APPLICATION_RESOURCE_CONFIRM_CONFLICT");
        require(resources.confirmGradeSubsidyQuota(batchId, student.getGradeId(), amount), "APPLICATION_RESOURCE_CONFIRM_CONFLICT");
    }
    private void releaseSubsidyUsage(Application application, StudentApplicationProfile student, BigDecimal amount) {
        Long batchId = application.getSubsidyBatchId();
        require(resources.releaseCollegeSubsidyUsage(batchId, student.getCollegeId(), amount), "APPLICATION_RESOURCE_RELEASE_CONFLICT");
        require(resources.releaseGradeSubsidyUsage(batchId, student.getGradeId(), amount), "APPLICATION_RESOURCE_RELEASE_CONFLICT");
    }
    private void releaseGiftUsage(Application application, StudentApplicationProfile student) {
        for (var item : resources.findGiftItems(application.getId())) require(resources.releaseGiftStockUsage(item.batchGiftItemId(), item.quantity()), "APPLICATION_RESOURCE_RELEASE_CONFLICT");
        int total = giftQuantity(application.getId());
        if (total == 0) return;
        Long batchId = application.getGreenChannelBatchId();
        require(resources.releaseCollegeGiftQuotaUsage(batchId, student.getCollegeId(), total), "APPLICATION_RESOURCE_RELEASE_CONFLICT");
        require(resources.releaseGradeGiftQuotaUsage(batchId, student.getGradeId(), total), "APPLICATION_RESOURCE_RELEASE_CONFLICT");
    }

    private void releaseReservation(Long applicationId, String requestId, Long operatorId, String operation) {
        if (alreadyProcessed(applicationId, operation, requestId)) return;
        Application application = required(applicationId);
        StudentApplicationProfile student = studentProfiles.getRequiredProfile(application.getStudentId());
        if (application.getApplicationType() == ApplicationType.GREEN_CHANNEL) {
            for (var item : resources.findGiftItems(applicationId)) require(resources.releaseGiftStockReservation(item.batchGiftItemId(), item.quantity()), "APPLICATION_RESOURCE_RELEASE_CONFLICT");
            int total = giftQuantity(applicationId);
            if (total > 0) {
                Long batchId = application.getGreenChannelBatchId();
                require(resources.releaseCollegeGiftQuotaReservation(batchId, student.getCollegeId(), total), "APPLICATION_RESOURCE_RELEASE_CONFLICT");
                require(resources.releaseGradeGiftQuotaReservation(batchId, student.getGradeId(), total), "APPLICATION_RESOURCE_RELEASE_CONFLICT");
            }
        } else releaseSubsidyReservation(application, student, effectiveSubsidyAmount(applicationId));
        record(applicationId, operation, requestId, operatorId);
    }

    private int giftQuantity(Long applicationId) { return resources.findGiftItems(applicationId).stream().mapToInt(item -> item.quantity()).sum(); }
    private com.example.backend.application.dto.SubsidyApplicationSnapshot requiredSubsidy(Long applicationId) { var subsidy = resources.findSubsidy(applicationId); if (subsidy == null) throw invalid("APPLICATION_SUBSIDY_REQUIRED", "申请缺少补助明细"); return subsidy; }
    private BigDecimal effectiveSubsidyAmount(Long applicationId) { var subsidy = requiredSubsidy(applicationId); return subsidy.finalAmount() == null ? subsidy.expectedAmount() : subsidy.finalAmount(); }
    private boolean alreadyProcessed(Long applicationId, String operation, String requestId) { validateRequest(requestId); return operations.countByApplicationOperationRequest(applicationId, operation, requestId) > 0; }
    private void record(Long applicationId, String operation, String requestId, Long operatorId) { operations.insert(applicationId, operation, requestId, operatorId); }
    private Application required(Long applicationId) { if (applicationId == null || applicationId <= 0) throw invalid("APPLICATION_NOT_FOUND", "申请不存在"); Application application = applications.findRequired(applicationId); if (application == null) throw invalid("APPLICATION_NOT_FOUND", "申请不存在"); return application; }
    private void validateRequest(String requestId) { if (requestId == null || requestId.isBlank() || requestId.length() > 64) throw invalid("APPLICATION_REQUEST_INVALID", "requestId 必须为 1 到 64 个字符"); }
    private void require(int changed, String code) { if (changed != 1) throw invalid(code, "资源余额、名额或状态已变化，请刷新后重试"); }
    private ApplicationException invalid(String code, String message) { return new ApplicationException(code, HttpStatus.CONFLICT, message); }
}
