package com.example.backend.service.impl;

import com.example.backend.model.dto.ApplicationStatusResult;
import com.example.backend.service.SystemMessageService;
import com.example.backend.model.domain.ApplicationStatus;
import com.example.backend.model.domain.ApprovalAction;
import com.example.backend.common.exception.ApprovalErrorCode;
import com.example.backend.common.exception.ApprovalException;
import com.example.backend.model.domain.ApprovalLevel;
import com.example.backend.model.domain.ApprovalRecordEntity;
import com.example.backend.mapper.ApprovalRecordMapper;
import com.example.backend.model.domain.ApprovalRecordLevel;
import com.example.backend.service.ApprovalMessageRecipientResolver;
import com.example.backend.service.ApprovalResourceService;
import com.example.backend.service.ApplicationStateQueryService;
import com.example.backend.model.dto.ApplicationStateSnapshot;
import com.example.backend.service.ApplicationStateWriteService;
import com.example.backend.model.dto.LoginUser;
import com.example.backend.service.StudentScopeService;
import com.example.backend.model.domain.UserRole;
import com.example.backend.model.dto.ArrearsItemCommand;
import com.example.backend.model.dto.GiftApplicationItemCommand;
import com.example.backend.model.dto.ReviewableApplicationEditCommand;
import com.example.backend.service.ReviewableApplicationEditService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;

/** Implements one-record review decisions. Batch submission is intentionally a
 * separate integration concern because it needs member-one scope and batch ports. */
public class ApprovalReviewService {

    private final ApplicationStateQueryService stateQueryService;
    private final ApplicationStateWriteService stateWriteService;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final ApprovalResourceService resourceService;
    private final com.example.backend.service.ApplicationDetailService detailService;
    private final ApprovalMessageRecipientResolver recipientResolver;
    private final ObjectProvider<SystemMessageService> messageServiceProvider;
    private final StudentScopeService studentScopeService;
    private final ReviewableApplicationEditService applicationEditService;

    public ApprovalReviewService(
            ApplicationStateQueryService stateQueryService,
            ApplicationStateWriteService stateWriteService,
            ApprovalRecordMapper approvalRecordMapper,
            ApprovalResourceService resourceService,
            com.example.backend.service.ApplicationDetailService detailService,
            ApprovalMessageRecipientResolver recipientResolver,
            ObjectProvider<SystemMessageService> messageServiceProvider,
            StudentScopeService studentScopeService,
            ReviewableApplicationEditService applicationEditService
    ) {
        this.stateQueryService = stateQueryService;
        this.stateWriteService = stateWriteService;
        this.approvalRecordMapper = approvalRecordMapper;
        this.resourceService = resourceService;
        this.detailService = detailService;
        this.recipientResolver = recipientResolver;
        this.messageServiceProvider = messageServiceProvider;
        this.studentScopeService = studentScopeService;
        this.applicationEditService = applicationEditService;
    }

    @Transactional
    public ApplicationStatusResult review(LoginUser user, Long applicationId, ReviewCommand command) {
        requireRole(user, command.level());
        validate(command);
        Optional<ApprovalRecordEntity> previous = approvalRecordMapper.findByRequestId(command.requestId());
        if (previous.isPresent()) {
            return idempotentResult(previous.get(), applicationId, command);
        }

        ApplicationStateSnapshot before = stateQueryService.getRequiredState(applicationId);
        validateState(before, command);
        validateScope(user, before);
        if (approvalRecordMapper.findLatestDecision(applicationId, before.reviewRound(), command.level()).isPresent()) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_INVALID_STATUS, "当前审核轮次已经给出结论，请勿重复审核");
        }
        ApplicationStatus target = targetStatus(before.status(), command);
        if (command.action() == ApprovalAction.APPROVE && command.level() == ApprovalRecordLevel.COLLEGE) {
            requireResourceService().validateCollegeApproval(applicationId);
        }
        if (command.action() == ApprovalAction.APPROVE && command.level() == ApprovalRecordLevel.COUNSELOR
                && command.finalSubsidyAmount() != null) {
            requireResourceService().applyCounselorSubsidyAmount(
                    applicationId, command.finalSubsidyAmount(), command.requestId(), user.userId());
        }
        if (command.action() == ApprovalAction.APPROVE && command.level() == ApprovalRecordLevel.SCHOOL) {
            requireResourceService().confirmOnSchoolApproval(applicationId, command.requestId(), user.userId());
        }
        if (command.action() == ApprovalAction.RETURN) {
            requireResourceService().handleReturn(applicationId, command.requestId(), user.userId());
        }
        if (command.action() == ApprovalAction.REJECT) {
            requireResourceService().releaseOnReject(applicationId, command.requestId(), user.userId());
        }

        ApplicationStateSnapshot after = target == before.status()
                ? before
                : stateWriteService.updateState(applicationId, before.status(), target, target.level(), command.version(), user.userId());
        approvalRecordMapper.insert(ApprovalRecordEntity.builder()
                .applicationId(applicationId)
                .reviewRound(before.reviewRound())
                .approvalLevel(command.level())
                .approverId(user.userId())
                .action(command.action())
                .comment(command.comment())
                .oldStatus(before.status())
                .newStatus(after.status())
                .requestId(command.requestId())
                .build());
        notifyStudent(before.studentId(), applicationId, command.action(), command.comment(), after.status());
        return new ApplicationStatusResult(after.applicationId(), after.status(), after.currentLevel(), after.version());
    }

    @Transactional
    public ApplicationStatusResult editAllowedFields(LoginUser user, Long applicationId, EditFieldsCommand command) {
        validateEditCommand(applicationId, command);
        ApprovalRecordLevel level = reviewerLevel(user);
        Optional<ApprovalRecordEntity> previous = approvalRecordMapper.findByRequestId(command.requestId());
        if (previous.isPresent()) return idempotentEditResult(previous.get(), applicationId, level, command);

        ApplicationStateSnapshot before = stateQueryService.getRequiredState(applicationId);
        if (!Objects.equals(before.version(), command.version())) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_VERSION_CONFLICT, "申请版本已变化，请刷新后重试");
        }
        if (before.status() != expectedStatus(level)) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_INVALID_STATUS, "申请当前不在该审核环节");
        }
        validateScope(user, before);

        List<String> requestedFields = command.requestedFields();
        List<String> allowedFields = ApprovalEditableFieldPolicy.applicationWriteFields(user, before);
        if (requestedFields.isEmpty()) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_ACTION_REQUIRED, "至少提供一个需要修改的字段");
        }
        if (!allowedFields.containsAll(requestedFields)) {
            Set<String> denied = new LinkedHashSet<>(requestedFields);
            denied.removeAll(allowedFields);
            throw new ApprovalException(
                    ApprovalErrorCode.APPROVAL_EDIT_FIELD_NOT_ALLOWED,
                    "当前角色、状态或申请类型不允许修改字段: " + String.join(",", denied)
            );
        }

        requireApplicationEditService().editForReview(
                applicationId,
                new ReviewableApplicationEditCommand(
                        command.version(), command.applicationReason(), command.arrearsItems(),
                        command.giftItems(), command.expectedSubsidyAmount()
                ),
                user.userId()
        );
        ApplicationStateSnapshot after = stateQueryService.getRequiredState(applicationId);
        approvalRecordMapper.insert(ApprovalRecordEntity.builder()
                .applicationId(applicationId)
                .reviewRound(before.reviewRound())
                .approvalLevel(level)
                .approverId(user.userId())
                .action(ApprovalAction.MODIFY)
                .comment(command.comment().trim())
                .oldStatus(before.status())
                .newStatus(after.status())
                .modifiedFields(toJsonArray(requestedFields))
                .requestId(command.requestId())
                .build());
        return new ApplicationStatusResult(after.applicationId(), after.status(), after.currentLevel(), after.version());
    }

    private ApplicationStatus targetStatus(ApplicationStatus current, ReviewCommand command) {
        return switch (command.action()) {
            case APPROVE -> {
                if (command.level() == ApprovalRecordLevel.SCHOOL) {
                    boolean hasArrears = requireDetailService().containsArrears(command.applicationId());
                    yield hasArrears ? ApplicationStatus.CONFIRM_PENDING : ApplicationStatus.APPROVED;
                }
                yield current;
            }
            case RETURN -> switch (command.level()) {
                case COUNSELOR -> ApplicationStatus.COUNSELOR_RETURNED;
                case COLLEGE -> ApplicationStatus.COLLEGE_RETURNED;
                case SCHOOL -> ApplicationStatus.SCHOOL_RETURNED;
                default -> throw new ApprovalException(ApprovalErrorCode.APPROVAL_INVALID_STATUS, "当前层级不支持退回");
            };
            case REJECT -> ApplicationStatus.REJECTED;
            default -> throw new ApprovalException(
                    ApprovalErrorCode.APPROVAL_ACTION_REQUIRED,
                    "逐条审核只支持 APPROVE、RETURN 或 REJECT"
            );
        };
    }

    private void validateState(ApplicationStateSnapshot state, ReviewCommand command) {
        if (!state.applicationId().equals(command.applicationId()) || !state.version().equals(command.version())) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_VERSION_CONFLICT, "申请版本已变化，请刷新后重试");
        }
        if (state.status() != expectedStatus(command.level())) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_INVALID_STATUS, "申请当前不在该审核环节");
        }
    }

    private ApplicationStatus expectedStatus(ApprovalRecordLevel level) {
        return switch (level) {
            case COUNSELOR -> ApplicationStatus.COUNSELOR_PENDING;
            case COLLEGE -> ApplicationStatus.COLLEGE_PENDING;
            case SCHOOL -> ApplicationStatus.SCHOOL_PENDING;
            default -> throw new ApprovalException(ApprovalErrorCode.APPROVAL_INVALID_STATUS, "当前层级不支持审核");
        };
    }

    private void requireRole(LoginUser user, ApprovalRecordLevel level) {
        if (user == null || user.userId() == null || user.role() != UserRole.valueOf(level.name())) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "无权执行当前审核操作");
        }
    }

    private void validate(ReviewCommand command) {
        if (command == null || command.applicationId() == null || command.applicationId() <= 0
                || command.version() == null || command.version() < 0
                || command.action() == null || command.requestId() == null || command.requestId().isBlank()) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_ACTION_REQUIRED, "审核参数不完整");
        }
        if (command.action() != ApprovalAction.APPROVE
                && command.action() != ApprovalAction.RETURN
                && command.action() != ApprovalAction.REJECT) {
            throw new ApprovalException(
                    ApprovalErrorCode.APPROVAL_ACTION_REQUIRED,
                    "逐条审核只支持 APPROVE、RETURN 或 REJECT"
            );
        }
        if ((command.action() == ApprovalAction.RETURN || command.action() == ApprovalAction.REJECT)
                && (command.comment() == null || command.comment().isBlank())) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_COMMENT_REQUIRED, "退回或驳回必须填写原因");
        }
    }

    private void validateScope(LoginUser user, ApplicationStateSnapshot state) {
        if (user.role() == UserRole.SCHOOL) return;
        boolean allowed = user.role() == UserRole.COUNSELOR
                ? studentScopeService.isCounselorResponsibleFor(user.userId(), state.studentId())
                : user.collegeId() != null && studentScopeService.isStudentInCollege(state.studentId(), user.collegeId());
        if (!allowed) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "当前用户不在该申请的数据范围内");
        }
    }

    private void validateEditCommand(Long applicationId, EditFieldsCommand command) {
        if (applicationId == null || applicationId <= 0 || command == null
                || !Objects.equals(applicationId, command.applicationId())
                || command.version() == null || command.version() < 0
                || command.requestId() == null || command.requestId().isBlank()) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_ACTION_REQUIRED, "审核编辑参数不完整");
        }
        if (command.comment() == null || command.comment().isBlank()) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_COMMENT_REQUIRED, "修改申请字段必须填写说明");
        }
    }

    private ApprovalRecordLevel reviewerLevel(LoginUser user) {
        if (user == null || user.userId() == null || user.role() == UserRole.STUDENT) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "当前角色不能修改审核字段");
        }
        return ApprovalRecordLevel.valueOf(user.role().name());
    }

    private ApplicationStatusResult idempotentEditResult(
            ApprovalRecordEntity record,
            Long applicationId,
            ApprovalRecordLevel level,
            EditFieldsCommand command
    ) {
        if (!Objects.equals(record.getApplicationId(), applicationId)
                || record.getApprovalLevel() != level
                || record.getAction() != ApprovalAction.MODIFY
                || !Objects.equals(record.getModifiedFields(), toJsonArray(command.requestedFields()))) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_ALREADY_PROCESSED, "requestId 已被其他操作使用");
        }
        return new ApplicationStatusResult(
                applicationId, record.getNewStatus(), record.getNewStatus().level(), command.version() + 1
        );
    }

    private ReviewableApplicationEditService requireApplicationEditService() {
        return applicationEditService;
    }

    private String toJsonArray(List<String> fields) {
        return fields.stream().map(field -> "\"" + field + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private ApplicationStatusResult idempotentResult(
            ApprovalRecordEntity record,
            Long applicationId,
            ReviewCommand command
    ) {
        if (!Objects.equals(record.getApplicationId(), applicationId)
                || record.getApprovalLevel() != command.level()
                || record.getAction() != command.action()) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_ALREADY_PROCESSED, "requestId 已被其他申请使用");
        }
        int resultVersion = Objects.equals(record.getOldStatus(), record.getNewStatus())
                ? command.version()
                : command.version() + 1;
        return new ApplicationStatusResult(
                applicationId,
                record.getNewStatus(),
                record.getNewStatus().level(),
                resultVersion
        );
    }

    private ApprovalResourceService requireResourceService() {
        return resourceService;
    }

    private com.example.backend.service.ApplicationDetailService requireDetailService() {
        return detailService;
    }

    private void notifyStudent(Long studentId, Long applicationId, ApprovalAction action, String comment, ApplicationStatus status) {
        SystemMessageService messages = messageServiceProvider.getIfAvailable();
        if (messages == null) return;
        Long receiverUserId = recipientResolver.getStudentUserId(studentId);
        if (receiverUserId == null) return;
        if (action == ApprovalAction.RETURN) messages.sendApprovalReturned(receiverUserId, applicationId, comment);
        if (action == ApprovalAction.REJECT) messages.sendApprovalRejected(receiverUserId, applicationId, comment);
        if (action == ApprovalAction.APPROVE && (status == ApplicationStatus.APPROVED || status == ApplicationStatus.CONFIRM_PENDING)) {
            messages.sendApprovalApproved(receiverUserId, applicationId, "学校已完成最终审核");
        }
    }

    public record ReviewCommand(
            Long applicationId,
            ApprovalRecordLevel level,
            ApprovalAction action,
            String comment,
            BigDecimal finalSubsidyAmount,
            Integer version,
            String requestId
    ) {
    }

    public record EditFieldsCommand(
            Long applicationId,
            String applicationReason,
            List<ArrearsItemCommand> arrearsItems,
            List<GiftApplicationItemCommand> giftItems,
            BigDecimal expectedSubsidyAmount,
            String comment,
            Integer version,
            String requestId
    ) {
        List<String> requestedFields() {
            List<String> fields = new ArrayList<>();
            if (applicationReason != null) fields.add(ApprovalEditableFieldPolicy.APPLICATION_REASON);
            if (arrearsItems != null) fields.add(ApprovalEditableFieldPolicy.ARREARS_ITEMS);
            if (giftItems != null) fields.add(ApprovalEditableFieldPolicy.GIFT_ITEMS);
            if (expectedSubsidyAmount != null) fields.add(ApprovalEditableFieldPolicy.EXPECTED_SUBSIDY_AMOUNT);
            return List.copyOf(fields);
        }
    }
}
