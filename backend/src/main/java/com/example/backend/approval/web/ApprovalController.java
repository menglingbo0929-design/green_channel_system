package com.example.backend.approval.web;

import com.example.backend.approval.api.ApprovalFlowQueryService;
import com.example.backend.approval.api.ApprovalFlowSnapshot;
import com.example.backend.approval.api.ApprovalTransitionService;
import com.example.backend.approval.api.ApplicationStatusResult;
import com.example.backend.approval.api.ApprovalDashboard;
import com.example.backend.approval.api.ApprovalDetailView;
import com.example.backend.approval.api.ApprovalListItem;
import com.example.backend.approval.api.ApprovalListQuery;
import com.example.backend.approval.api.ApprovalPage;
import com.example.backend.approval.api.ApprovalWorkbenchQueryService;
import com.example.backend.approval.domain.ApprovalAction;
import com.example.backend.approval.domain.ApprovalErrorCode;
import com.example.backend.approval.domain.ApprovalException;
import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApplicationType;
import com.example.backend.approval.persistence.type.ApprovalRecordLevel;
import com.example.backend.approval.persistence.type.BatchType;
import com.example.backend.approval.port.CurrentUserProvider;
import com.example.backend.approval.port.LoginUser;
import com.example.backend.approval.port.ApplicationStateQueryService;
import com.example.backend.approval.port.ApplicationStateSnapshot;
import com.example.backend.approval.port.StudentScopeService;
import com.example.backend.approval.port.UserRole;
import com.example.backend.approval.service.ApprovalReviewService;
import com.example.backend.approval.service.ApprovalCancellationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP boundary for member-three approval features. Query/list integration is
 * deliberately kept unavailable until the member-one scope and member-two detail
 * ports are provided; writes never trust identity values supplied by the client.
 */
@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final ObjectProvider<CurrentUserProvider> currentUserProvider;
    private final ObjectProvider<ApprovalReviewService> reviewServiceProvider;
    private final ObjectProvider<ApprovalCancellationService> cancellationServiceProvider;
    private final ObjectProvider<ApprovalTransitionService> transitionServiceProvider;
    private final ObjectProvider<ApprovalFlowQueryService> flowQueryServiceProvider;
    private final ObjectProvider<ApplicationStateQueryService> stateQueryServiceProvider;
    private final ObjectProvider<StudentScopeService> studentScopeServiceProvider;
    private final ObjectProvider<ApprovalWorkbenchQueryService> workbenchQueryServiceProvider;

    public ApprovalController(
            ObjectProvider<CurrentUserProvider> currentUserProvider,
            ObjectProvider<ApprovalReviewService> reviewServiceProvider,
            ObjectProvider<ApprovalCancellationService> cancellationServiceProvider,
            ObjectProvider<ApprovalTransitionService> transitionServiceProvider,
            ObjectProvider<ApprovalFlowQueryService> flowQueryServiceProvider,
            ObjectProvider<ApplicationStateQueryService> stateQueryServiceProvider,
            ObjectProvider<StudentScopeService> studentScopeServiceProvider,
            ObjectProvider<ApprovalWorkbenchQueryService> workbenchQueryServiceProvider
    ) {
        this.currentUserProvider = currentUserProvider;
        this.reviewServiceProvider = reviewServiceProvider;
        this.cancellationServiceProvider = cancellationServiceProvider;
        this.transitionServiceProvider = transitionServiceProvider;
        this.flowQueryServiceProvider = flowQueryServiceProvider;
        this.stateQueryServiceProvider = stateQueryServiceProvider;
        this.studentScopeServiceProvider = studentScopeServiceProvider;
        this.workbenchQueryServiceProvider = workbenchQueryServiceProvider;
    }

    @PostMapping("/counselor/{applicationId}/review")
    public ApplicationStatusResult reviewByCounselor(@PathVariable Long applicationId, @Valid @RequestBody ReviewRequest request) {
        return review(applicationId, ApprovalRecordLevel.COUNSELOR, request);
    }

    @PostMapping("/college/{applicationId}/review")
    public ApplicationStatusResult reviewByCollege(@PathVariable Long applicationId, @Valid @RequestBody ReviewRequest request) {
        return review(applicationId, ApprovalRecordLevel.COLLEGE, request);
    }

    @PostMapping("/school/{applicationId}/review")
    public ApplicationStatusResult reviewBySchool(@PathVariable Long applicationId, @Valid @RequestBody ReviewRequest request) {
        return review(applicationId, ApprovalRecordLevel.SCHOOL, request);
    }

    @PostMapping("/{applicationId}/resubmit")
    public ApplicationStatusResult resubmit(@PathVariable Long applicationId, @Valid @RequestBody VersionRequest request) {
        LoginUser user = currentUser();
        requireRole(user, UserRole.STUDENT);
        assertCanRead(user, applicationId);
        return required(transitionServiceProvider, "成员三审核流转 Service")
                .resubmitReturned(applicationId, request.version(), request.requestId(), user.userId());
    }

    @GetMapping("/{applicationId}/flow")
    public ApprovalFlowSnapshot flow(@PathVariable Long applicationId) {
        assertCanRead(currentUser(), applicationId);
        return required(flowQueryServiceProvider, "成员三审核流程查询 Service").getFlow(applicationId);
    }

    @GetMapping("/{applicationId}/status")
    public ApprovalFlowSnapshot status(@PathVariable Long applicationId) {
        assertCanRead(currentUser(), applicationId);
        return required(flowQueryServiceProvider, "成员三审核流程查询 Service").getFlow(applicationId);
    }

    @GetMapping("/pending")
    public ApprovalPage<ApprovalListItem> pending(@Valid ApprovalListRequest request) {
        return workbenchQueries().pagePending(currentUser(), request.toQuery());
    }

    @GetMapping("/processed")
    public ApprovalPage<ApprovalListItem> processed(@Valid ApprovalListRequest request) {
        return workbenchQueries().pageProcessed(currentUser(), request.toQuery());
    }

    @GetMapping("/dashboard")
    public ApprovalDashboard dashboard(@Valid ApprovalListRequest request) {
        return workbenchQueries().getDashboard(currentUser(), request.toQuery());
    }

    @GetMapping("/{applicationId}")
    public ApprovalDetailView detail(@PathVariable Long applicationId) {
        return workbenchQueries().getDetail(currentUser(), applicationId);
    }

    @PostMapping("/{applicationId}/cancel")
    public ApplicationStatusResult cancel(@PathVariable Long applicationId, @Valid @RequestBody CancelRequest request) {
        LoginUser user = currentUser();
        requireRole(user, UserRole.SCHOOL);
        return required(cancellationServiceProvider, "成员一消息收件人、成员二资源释放 Service 和成员四欠费单据 Service")
                .cancel(applicationId, request.version(), request.requestId(), request.reason(), user.userId());
    }

    private ApplicationStatusResult review(Long applicationId, ApprovalRecordLevel level, ReviewRequest request) {
        LoginUser user = currentUser();
        return required(reviewServiceProvider, "成员三审核 Service").review(user, applicationId,
                new ApprovalReviewService.ReviewCommand(
                        applicationId, level, request.action(), request.comment(), request.finalSubsidyAmount(),
                        request.version(), request.requestId()
                ));
    }

    private LoginUser currentUser() {
        return required(currentUserProvider, "成员一 CurrentUserProvider").getRequiredUser();
    }

    private void requireRole(LoginUser user, UserRole role) {
        if (user.role() != role) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "当前用户没有所需角色");
        }
    }

    private void assertCanRead(LoginUser user, Long applicationId) {
        ApplicationStateSnapshot state = required(stateQueryServiceProvider, "成员二申请状态查询 Service").getRequiredState(applicationId);
        if (user.role() == UserRole.SCHOOL) return;
        if (user.role() == UserRole.STUDENT) {
            if (user.studentId() != null && user.studentId().equals(state.studentId())) return;
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "只能查看本人申请");
        }
        StudentScopeService scopes = required(studentScopeServiceProvider, "成员一学生数据范围 Service");
        boolean allowed = user.role() == UserRole.COUNSELOR
                ? scopes.isCounselorResponsibleFor(user.userId(), state.studentId())
                : user.role() == UserRole.COLLEGE && user.collegeId() != null
                && scopes.isStudentInCollege(state.studentId(), user.collegeId());
        if (!allowed) throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "当前用户不在该申请的数据范围内");
    }

    private <T> T required(ObjectProvider<T> provider, String capability) {
        T value = provider.getIfAvailable();
        if (value == null) throw new ApprovalIntegrationUnavailableException(capability);
        return value;
    }

    private ApprovalWorkbenchQueryService workbenchQueries() {
        return required(workbenchQueryServiceProvider, "成员一数据范围与成员二审核查询 Service");
    }

    public record ReviewRequest(
            @NotNull ApprovalAction action,
            String comment,
            BigDecimal finalSubsidyAmount,
            @NotNull Integer version,
            @NotBlank String requestId
    ) {
    }

    public record VersionRequest(@NotNull Integer version, @NotBlank String requestId) {
    }

    public record CancelRequest(@NotBlank String reason, @NotNull Integer version, @NotBlank String requestId) {
    }

    public static class ApprovalListRequest {
        @Min(1)
        private int page = 1;
        @Min(1)
        @Max(100)
        private int size = 10;
        private BatchType batchType;
        private Long batchId;
        private ApplicationType applicationType;
        private String applicationNo;
        private String studentNo;
        private String studentName;
        private Long collegeId;
        private ApplicationStatus status;

        ApprovalListQuery toQuery() {
            return new ApprovalListQuery(page, size, batchType, batchId, applicationType, applicationNo,
                    studentNo, studentName, collegeId, status);
        }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public BatchType getBatchType() { return batchType; }
        public void setBatchType(BatchType batchType) { this.batchType = batchType; }
        public Long getBatchId() { return batchId; }
        public void setBatchId(Long batchId) { this.batchId = batchId; }
        public ApplicationType getApplicationType() { return applicationType; }
        public void setApplicationType(ApplicationType applicationType) { this.applicationType = applicationType; }
        public String getApplicationNo() { return applicationNo; }
        public void setApplicationNo(String applicationNo) { this.applicationNo = applicationNo; }
        public String getStudentNo() { return studentNo; }
        public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        public Long getCollegeId() { return collegeId; }
        public void setCollegeId(Long collegeId) { this.collegeId = collegeId; }
        public ApplicationStatus getStatus() { return status; }
        public void setStatus(ApplicationStatus status) { this.status = status; }
    }
}
