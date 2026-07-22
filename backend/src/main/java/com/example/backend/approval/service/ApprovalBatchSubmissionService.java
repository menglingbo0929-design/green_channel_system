package com.example.backend.approval.service;

import com.example.backend.approval.api.ApprovalSubmissionResult;
import com.example.backend.approval.api.ApprovalSubmissionService;
import com.example.backend.approval.api.ApprovalSubmissionStatus;
import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApprovalAction;
import com.example.backend.approval.domain.ApprovalErrorCode;
import com.example.backend.approval.domain.ApprovalException;
import com.example.backend.approval.domain.ApprovalLevel;
import com.example.backend.approval.persistence.entity.ApprovalRecordEntity;
import com.example.backend.approval.persistence.entity.ApprovalSubmissionRecordEntity;
import com.example.backend.approval.persistence.mapper.ApprovalRecordMapper;
import com.example.backend.approval.persistence.mapper.ApprovalSubmissionRecordMapper;
import com.example.backend.approval.persistence.type.ApprovalRecordLevel;
import com.example.backend.approval.persistence.type.BatchType;
import com.example.backend.approval.persistence.type.SubmissionLevel;
import com.example.backend.approval.persistence.type.SubmissionScopeType;
import com.example.backend.approval.persistence.type.SubmissionStatus;
import com.example.backend.approval.persistence.type.SubmissionType;
import com.example.backend.approval.port.ApprovalBatchQueryService;
import com.example.backend.approval.port.ApprovalResourceService;
import com.example.backend.approval.port.ApprovalSubmissionApplicationQueryService;
import com.example.backend.approval.port.ApplicationStateQueryService;
import com.example.backend.approval.port.ApplicationStateSnapshot;
import com.example.backend.approval.port.ApplicationStateWriteService;
import com.example.backend.approval.port.LoginUser;
import com.example.backend.approval.port.StudentScopeService;
import com.example.backend.approval.port.UserRole;
import com.example.backend.approval.web.ApprovalIntegrationUnavailableException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;

/**
 * Member-three orchestration for initial batch submission and returned-item
 * resubmission.  It never writes the application table directly.
 */
public class ApprovalBatchSubmissionService implements ApprovalSubmissionService {

    private final ApprovalSubmissionRecordMapper submissionRecords;
    private final ApprovalRecordMapper approvalRecords;
    private final ApplicationStateQueryService stateQueryService;
    private final ApplicationStateWriteService stateWriteService;
    private final ObjectProvider<ApprovalBatchQueryService> batchQueryProvider;
    private final ObjectProvider<ApprovalSubmissionApplicationQueryService> applicationQueryProvider;
    private final ObjectProvider<StudentScopeService> scopeServiceProvider;
    private final ObjectProvider<ApprovalResourceService> resourceServiceProvider;
    private final Clock clock;

    public ApprovalBatchSubmissionService(
            ApprovalSubmissionRecordMapper submissionRecords,
            ApprovalRecordMapper approvalRecords,
            ApplicationStateQueryService stateQueryService,
            ApplicationStateWriteService stateWriteService,
            ObjectProvider<ApprovalBatchQueryService> batchQueryProvider,
            ObjectProvider<ApprovalSubmissionApplicationQueryService> applicationQueryProvider,
            ObjectProvider<StudentScopeService> scopeServiceProvider,
            ObjectProvider<ApprovalResourceService> resourceServiceProvider,
            Clock clock
    ) {
        this.submissionRecords = submissionRecords;
        this.approvalRecords = approvalRecords;
        this.stateQueryService = stateQueryService;
        this.stateWriteService = stateWriteService;
        this.batchQueryProvider = batchQueryProvider;
        this.applicationQueryProvider = applicationQueryProvider;
        this.scopeServiceProvider = scopeServiceProvider;
        this.resourceServiceProvider = resourceServiceProvider;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalSubmissionStatus getStatus(LoginUser user, BatchType batchType, Long batchId) {
        Scope scope = scopeFor(user);
        ApprovalBatchQueryService.ApprovalBatchSnapshot batch = requiredBatchQuery().getRequiredBatch(batchType, batchId);
        validateBatchIdentity(batch, batchType, batchId);
        InitialSubmission initial = initialSubmission(batchType, batchId, scope);
        List<ApplicationStateSnapshot> applications = scopedApplications(batchType, batchId, scope);
        ApplicationStatus pendingStatus = pendingStatus(scope.level());
        int pendingReview = 0;
        int approvedWaiting = 0;
        int returned = 0;
        int rejected = 0;
        for (ApplicationStateSnapshot application : applications) {
            if (application.status() == pendingStatus) {
                Optional<ApprovalRecordEntity> decision = latestDecision(application, scope.level());
                if (decision.isEmpty()) {
                    pendingReview++;
                } else if (decision.get().getAction() == ApprovalAction.APPROVE) {
                    approvedWaiting++;
                }
            }
            if (isReturnedForLevel(application.status(), scope.level())) {
                returned++;
            }
            if (application.status() == ApplicationStatus.REJECTED) {
                rejected++;
            }
        }
        boolean canSubmit = !initial.exists()
                && approvedWaiting > 0
                && pendingReview == 0
                && isInitialSubmissionWindowOpen(batch, scope.level());
        return new ApprovalSubmissionStatus(
                batchType, batchId, scope.level(), batch.applicationDeadline(), batch.collegeDeadline(),
                initial.exists(), initial.submittedAt(), pendingReview, approvedWaiting, returned, rejected, canSubmit
        );
    }

    @Override
    @Transactional
    public ApprovalSubmissionResult submitInitial(LoginUser user, BatchType batchType, Long batchId, String requestId) {
        Scope scope = scopeFor(user);
        validateRequest(batchType, batchId, requestId);
        Optional<ApprovalSubmissionRecordEntity> previous = submissionRecords.findByRequestId(requestId);
        if (previous.isPresent()) {
            return idempotentResult(previous.get(), batchType, batchId, scope, SubmissionType.INITIAL_BATCH, null);
        }

        ApprovalBatchQueryService.ApprovalBatchSnapshot batch = requiredBatchQuery().getRequiredBatch(batchType, batchId);
        validateBatchIdentity(batch, batchType, batchId);
        requireInitialSubmissionWindow(batch, scope.level());
        if (initialSubmission(batchType, batchId, scope).exists()) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_BATCH_ALREADY_SUBMITTED, "当前审核范围已完成首次批量上报");
        }

        List<ApplicationStateSnapshot> pendingApplications = scopedApplications(batchType, batchId, scope).stream()
                .filter(application -> application.status() == pendingStatus(scope.level()))
                .toList();
        List<ApplicationStateSnapshot> approvedApplications = requireAllReviewed(pendingApplications, scope.level());
        if (approvedApplications.isEmpty()) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_INVALID_STATUS, "当前范围没有可上报的已通过申请");
        }
        validateResources(scope.level(), approvedApplications);
        for (ApplicationStateSnapshot application : approvedApplications) {
            advance(application, scope, user.userId(), requestId, SubmissionType.INITIAL_BATCH);
        }

        LocalDateTime submittedAt = LocalDateTime.now(clock);
        submissionRecords.insert(record(
                batchType, batchId, scope, SubmissionType.INITIAL_BATCH, 0L, 0,
                user.userId(), approvedApplications.size(), requestId, submittedAt
        ));
        return new ApprovalSubmissionResult(
                batchType, batchId, scope.level(), SubmissionType.INITIAL_BATCH, null,
                approvedApplications.size(), requestId, submittedAt
        );
    }

    @Override
    @Transactional
    public ApprovalSubmissionResult submitReturnResubmit(
            LoginUser user, Long applicationId, Integer expectedVersion, String requestId
    ) {
        Scope scope = scopeFor(user);
        validateReturnRequest(applicationId, expectedVersion, requestId);
        Optional<ApprovalSubmissionRecordEntity> previous = submissionRecords.findByRequestId(requestId);
        if (previous.isPresent()) {
            return idempotentResult(previous.get(), null, null, scope, SubmissionType.RETURN_RESUBMIT, applicationId);
        }

        ApplicationStateSnapshot application = stateQueryService.getRequiredState(applicationId);
        if (!Objects.equals(application.version(), expectedVersion)) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_VERSION_CONFLICT, "申请版本已变化，请刷新后重试");
        }
        if (application.status() != pendingStatus(scope.level())) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_INVALID_STATUS, "申请当前不处于可补交的审核节点");
        }
        requireInScope(application, scope);
        ApprovalBatchQueryService.ApprovalBatchSnapshot batch = requiredBatchQuery()
                .getRequiredBatch(application.batchType(), application.batchId());
        validateBatchIdentity(batch, application.batchType(), application.batchId());
        if (!initialSubmission(application.batchType(), application.batchId(), scope).exists()) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_BATCH_NOT_CLOSED, "首次批量上报尚未完成，不能走退回补交通道");
        }
        Optional<ApprovalRecordEntity> decision = latestDecision(application, scope.level());
        if (decision.isEmpty() || decision.get().getAction() != ApprovalAction.APPROVE) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_UNREVIEWED_EXISTS, "申请尚未在当前轮次审核通过");
        }
        validateResources(scope.level(), List.of(application));
        advance(application, scope, user.userId(), requestId, SubmissionType.RETURN_RESUBMIT);

        LocalDateTime submittedAt = LocalDateTime.now(clock);
        submissionRecords.insert(record(
                application.batchType(), application.batchId(), scope, SubmissionType.RETURN_RESUBMIT,
                application.applicationId(), application.reviewRound(), user.userId(), 1, requestId, submittedAt
        ));
        return new ApprovalSubmissionResult(
                application.batchType(), application.batchId(), scope.level(), SubmissionType.RETURN_RESUBMIT,
                application.applicationId(), 1, requestId, submittedAt
        );
    }

    private List<ApplicationStateSnapshot> requireAllReviewed(
            List<ApplicationStateSnapshot> pendingApplications, SubmissionLevel level
    ) {
        boolean hasUnreviewed = pendingApplications.stream()
                .anyMatch(application -> latestDecision(application, level).isEmpty());
        if (hasUnreviewed) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_UNREVIEWED_EXISTS, "当前范围仍存在未给出审核结论的申请");
        }
        return pendingApplications.stream()
                .filter(application -> latestDecision(application, level)
                        .map(record -> record.getAction() == ApprovalAction.APPROVE)
                        .orElse(false))
                .toList();
    }

    private void advance(
            ApplicationStateSnapshot application,
            Scope scope,
            Long operatorId,
            String requestId,
            SubmissionType submissionType
    ) {
        ApplicationStatus target = scope.level() == SubmissionLevel.COUNSELOR
                ? ApplicationStatus.COLLEGE_PENDING
                : ApplicationStatus.SCHOOL_PENDING;
        ApprovalLevel targetLevel = scope.level() == SubmissionLevel.COUNSELOR
                ? ApprovalLevel.COLLEGE
                : ApprovalLevel.SCHOOL;
        stateWriteService.updateState(
                application.applicationId(), application.status(), target, targetLevel,
                application.version(), operatorId
        );
        approvalRecords.insert(ApprovalRecordEntity.builder()
                .applicationId(application.applicationId())
                .reviewRound(application.reviewRound())
                .approvalLevel(recordLevel(scope.level()))
                .approverId(operatorId)
                .action(ApprovalAction.SUBMIT)
                .comment(submissionType == SubmissionType.INITIAL_BATCH ? "首次批量上报" : "退回后补交")
                .oldStatus(application.status())
                .newStatus(target)
                .requestId(auditRequestId(requestId, application.applicationId()))
                .build());
    }

    private void validateResources(SubmissionLevel level, List<ApplicationStateSnapshot> applications) {
        if (level != SubmissionLevel.COLLEGE) {
            return;
        }
        ApprovalResourceService resources = resourceServiceProvider.getIfAvailable();
        if (resources == null) {
            throw new ApprovalIntegrationUnavailableException("成员二 ApprovalResourceService（学院上报资源校验）");
        }
        applications.forEach(application -> resources.validateCollegeApproval(application.applicationId()));
    }

    private List<ApplicationStateSnapshot> scopedApplications(BatchType batchType, Long batchId, Scope scope) {
        return requiredApplicationQuery().listByBatch(batchType, batchId).stream()
                .filter(application -> application.batchType() == batchType && Objects.equals(application.batchId(), batchId))
                .filter(application -> inScope(application, scope))
                .toList();
    }

    private void requireInScope(ApplicationStateSnapshot application, Scope scope) {
        if (!inScope(application, scope)) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "当前用户不在该申请的数据范围内");
        }
    }

    private boolean inScope(ApplicationStateSnapshot application, Scope scope) {
        StudentScopeService scopes = scopeServiceProvider.getIfAvailable();
        if (scopes == null) {
            throw new ApprovalIntegrationUnavailableException("成员一 StudentScopeService");
        }
        return scope.level() == SubmissionLevel.COUNSELOR
                ? scopes.isCounselorResponsibleFor(scope.scopeId(), application.studentId())
                : scopes.isStudentInCollege(application.studentId(), scope.scopeId());
    }

    private Optional<ApprovalRecordEntity> latestDecision(ApplicationStateSnapshot application, SubmissionLevel level) {
        return approvalRecords.findLatestDecision(
                application.applicationId(), application.reviewRound(), recordLevel(level)
        );
    }

    private InitialSubmission initialSubmission(BatchType batchType, Long batchId, Scope scope) {
        return submissionRecords.listByScope(batchType, batchId, scope.level(), scope.scopeType(), scope.scopeId()).stream()
                .filter(record -> record.getSubmissionType() == SubmissionType.INITIAL_BATCH)
                .findFirst()
                .map(record -> new InitialSubmission(true, record.getSubmitTime()))
                .orElseGet(() -> new InitialSubmission(false, null));
    }

    private ApprovalSubmissionResult idempotentResult(
            ApprovalSubmissionRecordEntity record,
            BatchType batchType,
            Long batchId,
            Scope scope,
            SubmissionType expectedType,
            Long expectedApplicationId
    ) {
        if (record.getSubmissionType() != expectedType
                || record.getSubmissionLevel() != scope.level()
                || !Objects.equals(record.getScopeId(), scope.scopeId())
                || (batchType != null && (record.getBatchType() != batchType || !Objects.equals(record.normalizedBatchId(), batchId)))
                || !Objects.equals(record.getApplicationId(), expectedApplicationId == null ? 0L : expectedApplicationId)) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_ALREADY_PROCESSED, "requestId 已被其他上报操作使用");
        }
        return new ApprovalSubmissionResult(
                record.getBatchType(), record.normalizedBatchId(), record.getSubmissionLevel(), record.getSubmissionType(),
                record.getApplicationId() == 0L ? null : record.getApplicationId(), record.getSubmittedCount(),
                record.getRequestId(), record.getSubmitTime()
        );
    }

    private ApprovalSubmissionRecordEntity record(
            BatchType batchType, Long batchId, Scope scope, SubmissionType type, Long applicationId,
            Integer reviewRound, Long submitterId, Integer submittedCount, String requestId, LocalDateTime submittedAt
    ) {
        return ApprovalSubmissionRecordEntity.builder()
                .batchType(batchType)
                .greenChannelBatchId(batchType == BatchType.GREEN_CHANNEL ? batchId : null)
                .subsidyBatchId(batchType == BatchType.SUBSIDY ? batchId : null)
                .submissionLevel(scope.level())
                .submissionType(type)
                .scopeType(scope.scopeType())
                .scopeId(scope.scopeId())
                .applicationId(applicationId)
                .reviewRound(reviewRound)
                .submitterId(submitterId)
                .submittedCount(submittedCount)
                .status(SubmissionStatus.SUBMITTED)
                .requestId(requestId)
                .submitTime(submittedAt)
                .build();
    }

    private Scope scopeFor(LoginUser user) {
        if (user == null || user.userId() == null || user.userId() <= 0) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "无法识别当前登录用户");
        }
        if (user.role() == UserRole.COUNSELOR) {
            return new Scope(SubmissionLevel.COUNSELOR, SubmissionScopeType.COUNSELOR, user.userId());
        }
        if (user.role() == UserRole.COLLEGE && user.collegeId() != null && user.collegeId() > 0) {
            return new Scope(SubmissionLevel.COLLEGE, SubmissionScopeType.COLLEGE, user.collegeId());
        }
        throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "当前角色不能执行批量上报");
    }

    private void requireInitialSubmissionWindow(ApprovalBatchQueryService.ApprovalBatchSnapshot batch, SubmissionLevel level) {
        if (!batch.open()) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_BATCH_NOT_CLOSED, "当前批次未开放审核上报");
        }
        if (!isInitialSubmissionWindowOpen(batch, level)) {
            ApprovalErrorCode error = level == SubmissionLevel.COUNSELOR
                    ? ApprovalErrorCode.APPROVAL_BATCH_NOT_CLOSED
                    : ApprovalErrorCode.APPROVAL_COLLEGE_DEADLINE_EXPIRED;
            throw new ApprovalException(error, level == SubmissionLevel.COUNSELOR
                    ? "尚未到学生申请截止时间，不能批量上报"
                    : "已超过学院上报截止时间");
        }
    }

    private boolean isInitialSubmissionWindowOpen(ApprovalBatchQueryService.ApprovalBatchSnapshot batch, SubmissionLevel level) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!batch.open()) {
            return false;
        }
        if (level == SubmissionLevel.COUNSELOR) {
            return batch.applicationDeadline() != null && now.isAfter(batch.applicationDeadline());
        }
        return batch.collegeDeadline() != null && !now.isAfter(batch.collegeDeadline());
    }

    private void validateBatchIdentity(
            ApprovalBatchQueryService.ApprovalBatchSnapshot batch, BatchType batchType, Long batchId
    ) {
        if (batch == null || batch.batchType() != batchType || !Objects.equals(batch.batchId(), batchId)) {
            throw new IllegalStateException("批次查询服务返回的数据与请求不一致");
        }
    }

    private void validateRequest(BatchType batchType, Long batchId, String requestId) {
        if (batchType == null || batchId == null || batchId <= 0) {
            throw new IllegalArgumentException("batchType and a positive batchId are required");
        }
        validateRequestId(requestId);
    }

    private void validateReturnRequest(Long applicationId, Integer expectedVersion, String requestId) {
        if (applicationId == null || applicationId <= 0 || expectedVersion == null || expectedVersion < 0) {
            throw new IllegalArgumentException("applicationId and a non-negative version are required");
        }
        validateRequestId(requestId);
    }

    private void validateRequestId(String requestId) {
        if (requestId == null || requestId.isBlank() || requestId.length() > 64) {
            throw new IllegalArgumentException("requestId must contain 1 to 64 characters");
        }
    }

    private ApplicationStatus pendingStatus(SubmissionLevel level) {
        return level == SubmissionLevel.COUNSELOR
                ? ApplicationStatus.COUNSELOR_PENDING
                : ApplicationStatus.COLLEGE_PENDING;
    }

    private boolean isReturnedForLevel(ApplicationStatus status, SubmissionLevel level) {
        return status == (level == SubmissionLevel.COUNSELOR
                ? ApplicationStatus.COUNSELOR_RETURNED
                : ApplicationStatus.COLLEGE_RETURNED);
    }

    private ApprovalRecordLevel recordLevel(SubmissionLevel level) {
        return level == SubmissionLevel.COUNSELOR ? ApprovalRecordLevel.COUNSELOR : ApprovalRecordLevel.COLLEGE;
    }

    /** One request advances several applications, while approval_record requires a unique requestId per row. */
    private String auditRequestId(String requestId, Long applicationId) {
        return "batch-audit-" + UUID.nameUUIDFromBytes((requestId + ':' + applicationId)
                .getBytes(StandardCharsets.UTF_8));
    }

    private ApprovalBatchQueryService requiredBatchQuery() {
        ApprovalBatchQueryService service = batchQueryProvider.getIfAvailable();
        if (service == null) {
            throw new ApprovalIntegrationUnavailableException("成员一 ApprovalBatchQueryService");
        }
        return service;
    }

    private ApprovalSubmissionApplicationQueryService requiredApplicationQuery() {
        ApprovalSubmissionApplicationQueryService service = applicationQueryProvider.getIfAvailable();
        if (service == null) {
            throw new ApprovalIntegrationUnavailableException("成员二 ApprovalSubmissionApplicationQueryService");
        }
        return service;
    }

    private record Scope(SubmissionLevel level, SubmissionScopeType scopeType, Long scopeId) {
    }

    private record InitialSubmission(boolean exists, LocalDateTime submittedAt) {
    }
}
