package com.example.backend.service.impl;

import com.example.backend.model.dto.ApplicationStatusResult;
import com.example.backend.service.ApprovalCompletionService;
import com.example.backend.service.ApprovalFlowQueryService;
import com.example.backend.model.dto.ApprovalFlowSnapshot;
import com.example.backend.model.dto.ApprovalRecordSnapshot;
import com.example.backend.service.ApprovalTransitionService;
import com.example.backend.model.domain.ApplicationStatus;
import com.example.backend.model.domain.ApprovalAction;
import com.example.backend.common.exception.ApprovalErrorCode;
import com.example.backend.common.exception.ApprovalException;
import com.example.backend.model.domain.ApprovalStateMachine;
import com.example.backend.model.domain.ApprovalTransition;
import com.example.backend.model.domain.ApprovalRecordEntity;
import com.example.backend.mapper.ApprovalRecordMapper;
import com.example.backend.model.domain.ApprovalRecordLevel;
import com.example.backend.service.ApplicationStateQueryService;
import com.example.backend.model.dto.ApplicationStateSnapshot;
import com.example.backend.service.ApplicationStateWriteService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ApprovalWorkflowService implements
        ApprovalTransitionService,
        ApprovalCompletionService,
        ApprovalFlowQueryService {

    private final ApprovalStateMachine stateMachine;
    private final ApplicationStateQueryService stateQueryService;
    private final ApplicationStateWriteService stateWriteService;
    private final ApprovalRecordMapper approvalRecordMapper;

    public ApprovalWorkflowService(
            ApprovalStateMachine stateMachine,
            ApplicationStateQueryService stateQueryService,
            ApplicationStateWriteService stateWriteService,
            ApprovalRecordMapper approvalRecordMapper
    ) {
        this.stateMachine = stateMachine;
        this.stateQueryService = stateQueryService;
        this.stateWriteService = stateWriteService;
        this.approvalRecordMapper = approvalRecordMapper;
    }

    @Override
    @Transactional
    public ApplicationStatusResult submitInitial(
            Long applicationId,
            Integer expectedVersion,
            String requestId,
            Long operatorId
    ) {
        return executeTransition(
                applicationId,
                expectedVersion,
                requestId,
                operatorId,
                ApprovalRecordLevel.STUDENT,
                ApprovalAction.SUBMIT,
                "首次提交申请",
                stateMachine::submitInitial
        );
    }

    @Override
    @Transactional
    public ApplicationStatusResult resubmitReturned(
            Long applicationId,
            Integer expectedVersion,
            String requestId,
            Long operatorId
    ) {
        return executeTransition(
                applicationId,
                expectedVersion,
                requestId,
                operatorId,
                ApprovalRecordLevel.STUDENT,
                ApprovalAction.SUBMIT,
                "退回后重新提交",
                stateMachine::resubmitReturned
        );
    }

    @Override
    @Transactional
    public ApplicationStatusResult completeSupplementReview(
            Long applicationId,
            boolean containsArrears,
            Integer expectedVersion,
            String requestId,
            Long operatorId
    ) {
        return executeTransition(
                applicationId,
                expectedVersion,
                requestId,
                operatorId,
                ApprovalRecordLevel.SYSTEM,
                ApprovalAction.APPROVE,
                "学校补录自动审核完成",
                status -> stateMachine.completeSupplementReview(status, containsArrears)
        );
    }

    @Override
    @Transactional
    public ApplicationStatusResult completeAfterConfirmation(
            Long applicationId,
            Integer expectedVersion,
            String requestId,
            Long operatorId
    ) {
        return executeTransition(
                applicationId,
                expectedVersion,
                requestId,
                operatorId,
                ApprovalRecordLevel.CONFIRMATION,
                ApprovalAction.APPROVE,
                "欠费金额确认完成",
                stateMachine::completeAfterConfirmation
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalFlowSnapshot getFlow(Long applicationId) {
        requirePositive(applicationId, "applicationId");
        ApplicationStateSnapshot state = stateQueryService.getRequiredState(applicationId);
        List<ApprovalRecordSnapshot> records = approvalRecordMapper.listByApplicationId(applicationId)
                .stream()
                .map(this::toRecordSnapshot)
                .toList();

        String returnReason = latestReason(records, ApprovalAction.RETURN).orElse(null);
        String rejectReason = latestReason(records, ApprovalAction.REJECT).orElse(null);
        return new ApprovalFlowSnapshot(
                state.applicationId(),
                state.status(),
                state.currentLevel(),
                returnReason,
                rejectReason,
                records
        );
    }

    private ApplicationStatusResult executeTransition(
            Long applicationId,
            Integer expectedVersion,
            String requestId,
            Long operatorId,
            ApprovalRecordLevel recordLevel,
            ApprovalAction action,
            String comment,
            TransitionResolver transitionResolver
    ) {
        validateCommand(applicationId, expectedVersion, requestId, operatorId);

        Optional<ApprovalRecordEntity> previous = approvalRecordMapper.findByRequestId(requestId);
        if (previous.isPresent()) {
            return idempotentResult(previous.get(), applicationId, expectedVersion);
        }

        ApplicationStateSnapshot before = stateQueryService.getRequiredState(applicationId);
        validateSnapshot(before, applicationId, expectedVersion);
        ApprovalTransition transition = transitionResolver.resolve(before.status());

        ApplicationStateSnapshot after = transition.reviewRoundDelta() == 1
                ? stateWriteService.incrementReviewRoundAndUpdateState(
                        applicationId,
                        transition.sourceStatus(),
                        transition.targetStatus(),
                        transition.targetLevel(),
                        expectedVersion,
                        operatorId
                )
                : stateWriteService.updateState(
                        applicationId,
                        transition.sourceStatus(),
                        transition.targetStatus(),
                        transition.targetLevel(),
                        expectedVersion,
                        operatorId
                );

        validateWriteResult(after, transition);
        int resultingRound = before.reviewRound() + transition.reviewRoundDelta();
        approvalRecordMapper.insert(ApprovalRecordEntity.builder()
                .applicationId(applicationId)
                .reviewRound(resultingRound)
                .approvalLevel(recordLevel)
                .approverId(operatorId)
                .action(action)
                .comment(comment)
                .oldStatus(transition.sourceStatus())
                .newStatus(transition.targetStatus())
                .requestId(requestId)
                .build());
        return toStatusResult(after);
    }

    private ApplicationStatusResult idempotentResult(
            ApprovalRecordEntity record,
            Long applicationId,
            Integer expectedVersion
    ) {
        if (!Objects.equals(record.getApplicationId(), applicationId)) {
            throw new ApprovalException(
                    ApprovalErrorCode.APPROVAL_ALREADY_PROCESSED,
                    "requestId 已被其他申请使用"
            );
        }
        return new ApplicationStatusResult(
                applicationId,
                record.getNewStatus(),
                record.getNewStatus().level(),
                expectedVersion + 1
        );
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

    private void validateWriteResult(
            ApplicationStateSnapshot state,
            ApprovalTransition transition
    ) {
        if (state.status() != transition.targetStatus()
                || state.currentLevel() != transition.targetLevel()) {
            throw new IllegalStateException("申请状态写入结果与审核状态机不一致");
        }
    }

    private void validateCommand(
            Long applicationId,
            Integer expectedVersion,
            String requestId,
            Long operatorId
    ) {
        requirePositive(applicationId, "applicationId");
        requirePositive(operatorId, "operatorId");
        if (expectedVersion == null || expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must be non-negative");
        }
        if (requestId == null || requestId.isBlank() || requestId.length() > 64) {
            throw new IllegalArgumentException("requestId must contain 1 to 64 characters");
        }
    }

    private void requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
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

    private ApprovalRecordSnapshot toRecordSnapshot(ApprovalRecordEntity entity) {
        return new ApprovalRecordSnapshot(
                entity.getId(),
                entity.getApplicationId(),
                entity.getReviewRound(),
                entity.getApprovalLevel(),
                entity.getApproverId(),
                entity.getApproverNameSnapshot(),
                entity.getAction(),
                entity.getComment(),
                entity.getOldStatus(),
                entity.getNewStatus(),
                entity.getModifiedFields(),
                entity.getRequestId(),
                entity.getCreateTime()
        );
    }

    private Optional<String> latestReason(
            List<ApprovalRecordSnapshot> records,
            ApprovalAction action
    ) {
        for (int index = records.size() - 1; index >= 0; index--) {
            ApprovalRecordSnapshot record = records.get(index);
            if (record.action() == action && record.comment() != null && !record.comment().isBlank()) {
                return Optional.of(record.comment());
            }
        }
        return Optional.empty();
    }

    @FunctionalInterface
    private interface TransitionResolver {
        ApprovalTransition resolve(ApplicationStatus status);
    }
}
