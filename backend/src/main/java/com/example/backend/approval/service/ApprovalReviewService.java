package com.example.backend.approval.service;

import com.example.backend.approval.api.ApplicationStatusResult;
import com.example.backend.approval.api.SystemMessageService;
import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApprovalAction;
import com.example.backend.approval.domain.ApprovalErrorCode;
import com.example.backend.approval.domain.ApprovalException;
import com.example.backend.approval.domain.ApprovalLevel;
import com.example.backend.approval.persistence.entity.ApprovalRecordEntity;
import com.example.backend.approval.persistence.mapper.ApprovalRecordMapper;
import com.example.backend.approval.persistence.type.ApprovalRecordLevel;
import com.example.backend.approval.port.ApprovalMessageRecipientResolver;
import com.example.backend.approval.port.ApprovalResourceService;
import com.example.backend.approval.port.ApplicationStateQueryService;
import com.example.backend.approval.port.ApplicationStateSnapshot;
import com.example.backend.approval.port.ApplicationStateWriteService;
import com.example.backend.approval.port.LoginUser;
import com.example.backend.approval.port.StudentScopeService;
import com.example.backend.approval.port.UserRole;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;

/** Implements one-record review decisions. Batch submission is intentionally a
 * separate integration concern because it needs member-one scope and batch ports. */
public class ApprovalReviewService {

    private final ApplicationStateQueryService stateQueryService;
    private final ApplicationStateWriteService stateWriteService;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final ObjectProvider<ApprovalResourceService> resourceServiceProvider;
    private final ObjectProvider<com.example.backend.application.port.ApplicationDetailService> detailServiceProvider;
    private final ObjectProvider<ApprovalMessageRecipientResolver> recipientResolverProvider;
    private final ObjectProvider<SystemMessageService> messageServiceProvider;
    private final ObjectProvider<StudentScopeService> studentScopeServiceProvider;

    public ApprovalReviewService(
            ApplicationStateQueryService stateQueryService,
            ApplicationStateWriteService stateWriteService,
            ApprovalRecordMapper approvalRecordMapper,
            ObjectProvider<ApprovalResourceService> resourceServiceProvider,
            ObjectProvider<com.example.backend.application.port.ApplicationDetailService> detailServiceProvider,
            ObjectProvider<ApprovalMessageRecipientResolver> recipientResolverProvider,
            ObjectProvider<SystemMessageService> messageServiceProvider,
            ObjectProvider<StudentScopeService> studentScopeServiceProvider
    ) {
        this.stateQueryService = stateQueryService;
        this.stateWriteService = stateWriteService;
        this.approvalRecordMapper = approvalRecordMapper;
        this.resourceServiceProvider = resourceServiceProvider;
        this.detailServiceProvider = detailServiceProvider;
        this.recipientResolverProvider = recipientResolverProvider;
        this.messageServiceProvider = messageServiceProvider;
        this.studentScopeServiceProvider = studentScopeServiceProvider;
    }

    @Transactional
    public ApplicationStatusResult review(LoginUser user, Long applicationId, ReviewCommand command) {
        requireRole(user, command.level());
        validate(command);
        Optional<ApprovalRecordEntity> previous = approvalRecordMapper.findByRequestId(command.requestId());
        if (previous.isPresent()) {
            return idempotentResult(previous.get(), applicationId, command.version());
        }

        ApplicationStateSnapshot before = stateQueryService.getRequiredState(applicationId);
        validateState(before, command);
        validateScope(user, before);
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

    private ApplicationStatus targetStatus(ApplicationStatus current, ReviewCommand command) {
        if (command.action() == ApprovalAction.APPROVE) {
            if (command.level() == ApprovalRecordLevel.SCHOOL) {
                boolean hasArrears = requireDetailService().containsArrears(command.applicationId());
                return hasArrears ? ApplicationStatus.CONFIRM_PENDING : ApplicationStatus.APPROVED;
            }
            return current;
        }
        if (command.action() == ApprovalAction.REJECT) return ApplicationStatus.REJECTED;
        return switch (command.level()) {
            case COUNSELOR -> ApplicationStatus.COUNSELOR_RETURNED;
            case COLLEGE -> ApplicationStatus.COLLEGE_RETURNED;
            case SCHOOL -> ApplicationStatus.SCHOOL_RETURNED;
            default -> throw new ApprovalException(ApprovalErrorCode.APPROVAL_INVALID_STATUS, "当前层级不支持退回");
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
        if ((command.action() == ApprovalAction.RETURN || command.action() == ApprovalAction.REJECT)
                && (command.comment() == null || command.comment().isBlank())) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_COMMENT_REQUIRED, "退回或驳回必须填写原因");
        }
    }

    private void validateScope(LoginUser user, ApplicationStateSnapshot state) {
        if (user.role() == UserRole.SCHOOL) return;
        StudentScopeService scopes = studentScopeServiceProvider.getIfAvailable();
        if (scopes == null) {
            throw new com.example.backend.approval.web.ApprovalIntegrationUnavailableException("成员一学生数据范围 Service");
        }
        boolean allowed = user.role() == UserRole.COUNSELOR
                ? scopes.isCounselorResponsibleFor(user.userId(), state.studentId())
                : user.collegeId() != null && scopes.isStudentInCollege(state.studentId(), user.collegeId());
        if (!allowed) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "当前用户不在该申请的数据范围内");
        }
    }

    private ApplicationStatusResult idempotentResult(ApprovalRecordEntity record, Long applicationId, Integer version) {
        if (!record.getApplicationId().equals(applicationId)) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_ALREADY_PROCESSED, "requestId 已被其他申请使用");
        }
        return new ApplicationStatusResult(applicationId, record.getNewStatus(), record.getNewStatus().level(), version + 1);
    }

    private ApprovalResourceService requireResourceService() {
        ApprovalResourceService service = resourceServiceProvider.getIfAvailable();
        if (service == null) throw new com.example.backend.approval.web.ApprovalIntegrationUnavailableException("成员二资源 Service");
        return service;
    }

    private com.example.backend.application.port.ApplicationDetailService requireDetailService() {
        var service = detailServiceProvider.getIfAvailable();
        if (service == null) throw new com.example.backend.approval.web.ApprovalIntegrationUnavailableException("成员二申请详情 Service");
        return service;
    }

    private void notifyStudent(Long studentId, Long applicationId, ApprovalAction action, String comment, ApplicationStatus status) {
        ApprovalMessageRecipientResolver resolver = recipientResolverProvider.getIfAvailable();
        SystemMessageService messages = messageServiceProvider.getIfAvailable();
        if (resolver == null || messages == null) return;
        Long receiverUserId = resolver.getStudentUserId(studentId);
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
}
