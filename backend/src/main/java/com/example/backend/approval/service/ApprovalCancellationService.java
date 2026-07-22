package com.example.backend.approval.service;

import com.example.backend.approval.api.ApplicationStatusResult;
import com.example.backend.approval.api.SystemMessageService;
import com.example.backend.application.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApprovalAction;
import com.example.backend.approval.domain.ApprovalErrorCode;
import com.example.backend.approval.domain.ApprovalException;
import com.example.backend.approval.domain.ApprovalStateMachine;
import com.example.backend.approval.domain.ApprovalTransition;
import com.example.backend.approval.persistence.entity.ApprovalRecordEntity;
import com.example.backend.approval.persistence.mapper.ApprovalRecordMapper;
import com.example.backend.approval.persistence.type.ApprovalRecordLevel;
import com.example.backend.approval.port.ApprovalResourceService;
import com.example.backend.approval.port.ApprovalMessageRecipientResolver;
import com.example.backend.application.port.ApplicationStateQueryService;
import com.example.backend.application.dto.ApplicationStateSnapshot;
import com.example.backend.application.port.ApplicationStateWriteService;
import com.example.backend.approval.port.ArrearsDocumentService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates a school cancellation without directly writing another member's
 * application, resource, or arrears-document tables.
 */
public class ApprovalCancellationService {

    private static final int MAX_REQUEST_ID_LENGTH = 64;
    private static final int MAX_REASON_LENGTH = 500;

    private final ApprovalStateMachine stateMachine;
    private final ApplicationStateQueryService stateQueryService;
    private final ApplicationStateWriteService stateWriteService;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final ApprovalResourceService resourceService;
    private final ArrearsDocumentService arrearsDocumentService;
    private final ApprovalMessageRecipientResolver messageRecipientResolver;
    private final java.util.function.Supplier<SystemMessageService> systemMessageService;

    public ApprovalCancellationService(
            ApprovalStateMachine stateMachine,
            ApplicationStateQueryService stateQueryService,
            ApplicationStateWriteService stateWriteService,
            ApprovalRecordMapper approvalRecordMapper,
            ApprovalResourceService resourceService,
            ArrearsDocumentService arrearsDocumentService,
            ApprovalMessageRecipientResolver messageRecipientResolver,
            SystemMessageService systemMessageService
    ) {
        this.stateMachine = stateMachine;
        this.stateQueryService = stateQueryService;
        this.stateWriteService = stateWriteService;
        this.approvalRecordMapper = approvalRecordMapper;
        this.resourceService = resourceService;
        this.arrearsDocumentService = arrearsDocumentService;
        this.messageRecipientResolver = messageRecipientResolver;
        this.systemMessageService = () -> systemMessageService;
    }

    public ApprovalCancellationService(
            ApprovalStateMachine stateMachine,
            ApplicationStateQueryService stateQueryService,
            ApplicationStateWriteService stateWriteService,
            ApprovalRecordMapper approvalRecordMapper,
            ApprovalResourceService resourceService,
            ArrearsDocumentService arrearsDocumentService,
            ApprovalMessageRecipientResolver messageRecipientResolver,
            ObjectProvider<SystemMessageService> systemMessageService
    ) {
        this.stateMachine = stateMachine;
        this.stateQueryService = stateQueryService;
        this.stateWriteService = stateWriteService;
        this.approvalRecordMapper = approvalRecordMapper;
        this.resourceService = resourceService;
        this.arrearsDocumentService = arrearsDocumentService;
        this.messageRecipientResolver = messageRecipientResolver;
        this.systemMessageService = () -> systemMessageService.getIfAvailable();
    }

    /**
     * The resource release, document voiding, state update and audit record
     * share the caller's Spring transaction. Any failure therefore rolls back
     * the cancellation instead of leaving a partially cancelled application.
     */
    @Transactional
    public ApplicationStatusResult cancel(
            Long applicationId,
            Integer expectedVersion,
            String requestId,
            String reason,
            Long operatorId
    ) {
        validateCommand(applicationId, expectedVersion, requestId, reason, operatorId);

        Optional<ApprovalRecordEntity> previous = approvalRecordMapper.findByRequestId(requestId);
        if (previous.isPresent()) {
            return previousResult(previous.get(), applicationId, expectedVersion);
        }

        ApplicationStateSnapshot before = stateQueryService.getRequiredState(applicationId);
        validateSnapshot(before, applicationId, expectedVersion);
        if (!before.status().isCancellable()) {
            throw new ApprovalException(
                    ApprovalErrorCode.APPROVAL_CANCEL_NOT_ALLOWED,
                    "当前申请状态不允许学校取消"
            );
        }
        if (arrearsDocumentService.hasIrreversibleOfflineProcessing(applicationId)) {
            throw new ApprovalException(
                    ApprovalErrorCode.APPROVAL_CANCEL_NOT_ALLOWED,
                    "申请已发生不可逆线下处理，不能取消"
            );
        }

        ApprovalTransition transition = stateMachine.cancel(before.status());
        ApplicationStateSnapshot after = stateWriteService.updateState(
                applicationId,
                transition.sourceStatus(),
                transition.targetStatus(),
                transition.targetLevel(),
                expectedVersion,
                operatorId
        );
        validateWriteResult(after, transition);

        resourceService.releaseOnCancel(applicationId, requestId, operatorId);
        arrearsDocumentService.voidDocumentForCancellation(applicationId, reason.strip(), operatorId);
        approvalRecordMapper.insert(ApprovalRecordEntity.builder()
                .applicationId(applicationId)
                .reviewRound(before.reviewRound())
                .approvalLevel(ApprovalRecordLevel.SCHOOL)
                .approverId(operatorId)
                .action(ApprovalAction.CANCEL)
                .comment(reason.strip())
                .oldStatus(transition.sourceStatus())
                .newStatus(transition.targetStatus())
                .requestId(requestId)
                .build());
        Long studentUserId = messageRecipientResolver.getStudentUserId(before.studentId());
        if (studentUserId == null || studentUserId <= 0) {
            throw new IllegalStateException("无法解析申请学生的登录用户，取消操作已回滚");
        }
        SystemMessageService messages = systemMessageService.get();
        if (messages == null) throw new IllegalStateException("消息通知能力不可用，取消操作已回滚");
        messages.sendApprovalCancelled(studentUserId, applicationId, reason.strip());
        return toStatusResult(after);
    }

    private ApplicationStatusResult previousResult(
            ApprovalRecordEntity record,
            Long applicationId,
            Integer expectedVersion
    ) {
        if (!Objects.equals(record.getApplicationId(), applicationId) || record.getAction() != ApprovalAction.CANCEL) {
            throw new ApprovalException(
                    ApprovalErrorCode.APPROVAL_ALREADY_PROCESSED,
                    "requestId 已被其他审核操作使用"
            );
        }
        return new ApplicationStatusResult(
                applicationId,
                record.getNewStatus(),
                record.getNewStatus().level(),
                expectedVersion + 1
        );
    }

    private void validateCommand(
            Long applicationId,
            Integer expectedVersion,
            String requestId,
            String reason,
            Long operatorId
    ) {
        requirePositive(applicationId, "applicationId");
        requirePositive(operatorId, "operatorId");
        if (expectedVersion == null || expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must be non-negative");
        }
        if (requestId == null || requestId.isBlank() || requestId.length() > MAX_REQUEST_ID_LENGTH) {
            throw new IllegalArgumentException("requestId must contain 1 to 64 characters");
        }
        if (reason == null || reason.isBlank() || reason.strip().length() > MAX_REASON_LENGTH) {
            throw new ApprovalException(
                    ApprovalErrorCode.APPROVAL_COMMENT_REQUIRED,
                    "取消原因不能为空且不能超过 500 个字符"
            );
        }
    }

    private void validateSnapshot(
            ApplicationStateSnapshot state,
            Long applicationId,
            Integer expectedVersion
    ) {
        if (!Objects.equals(state.applicationId(), applicationId)) {
            throw new IllegalStateException("申请状态服务返回了错误的 applicationId");
        }
        if (!Objects.equals(state.version(), expectedVersion)) {
            throw new ApprovalException(
                    ApprovalErrorCode.APPROVAL_VERSION_CONFLICT,
                    "申请版本已变化，请刷新后重试"
            );
        }
        if (state.reviewRound() == null || state.reviewRound() < 0) {
            throw new IllegalStateException("申请审核轮次无效");
        }
    }

    private void validateWriteResult(ApplicationStateSnapshot state, ApprovalTransition transition) {
        if (state.status() != transition.targetStatus()
                || state.currentLevel() != transition.targetLevel()) {
            throw new IllegalStateException("申请状态写入结果与取消状态机不一致");
        }
    }

    private ApplicationStatusResult toStatusResult(ApplicationStateSnapshot state) {
        return new ApplicationStatusResult(
                state.applicationId(),
                state.status(),
                state.currentLevel(),
                state.version()
        );
    }

    private void requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
