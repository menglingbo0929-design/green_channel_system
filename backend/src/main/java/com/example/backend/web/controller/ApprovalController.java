package com.example.backend.web.controller;

import com.example.backend.service.ApprovalFlowQueryService;
import com.example.backend.model.dto.ApprovalFlowSnapshot;
import com.example.backend.service.ApprovalTransitionService;
import com.example.backend.model.dto.ApplicationStatusResult;
import com.example.backend.model.dto.ApprovalDashboard;
import com.example.backend.model.dto.ApprovalDetailView;
import com.example.backend.model.dto.ApprovalListItem;
import com.example.backend.model.dto.ApprovalListQuery;
import com.example.backend.model.dto.ApprovalPage;
import com.example.backend.service.ApprovalWorkbenchQueryService;
import com.example.backend.model.domain.ApprovalAction;
import com.example.backend.common.exception.ApprovalErrorCode;
import com.example.backend.common.exception.ApprovalException;
import com.example.backend.model.domain.ApplicationStatus;
import com.example.backend.model.domain.ApplicationType;
import com.example.backend.model.domain.ApprovalRecordLevel;
import com.example.backend.model.domain.BatchType;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.model.dto.LoginUser;
import com.example.backend.service.ApplicationStateQueryService;
import com.example.backend.service.ApplicationAttachmentReadService;
import com.example.backend.model.dto.ApplicationStateSnapshot;
import com.example.backend.service.StudentScopeService;
import com.example.backend.model.domain.UserRole;
import com.example.backend.service.impl.ApprovalReviewService;
import com.example.backend.service.impl.ApprovalCancellationService;
import com.example.backend.model.dto.ArrearsItemCommand;
import com.example.backend.model.dto.GiftApplicationItemCommand;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * 审批 HTTP 边界。所有核心能力均为启动时必需依赖，避免服务启动后才返回“未接入”。
 */
@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final ICurrentUserProvider currentUserProvider;
    private final ApprovalReviewService reviewService;
    private final ApprovalCancellationService cancellationService;
    private final ApprovalTransitionService transitionService;
    private final ApprovalFlowQueryService flowQueryService;
    private final ApplicationStateQueryService stateQueryService;
    private final StudentScopeService studentScopeService;
    private final ApprovalWorkbenchQueryService workbenchQueryService;
    private final ApplicationAttachmentReadService attachmentReadService;

    public ApprovalController(
            ICurrentUserProvider currentUserProvider,
            ApprovalReviewService reviewService,
            ApprovalCancellationService cancellationService,
            ApprovalTransitionService transitionService,
            ApprovalFlowQueryService flowQueryService,
            ApplicationStateQueryService stateQueryService,
            StudentScopeService studentScopeService,
            ApprovalWorkbenchQueryService workbenchQueryService,
            ApplicationAttachmentReadService attachmentReadService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.reviewService = reviewService;
        this.cancellationService = cancellationService;
        this.transitionService = transitionService;
        this.flowQueryService = flowQueryService;
        this.stateQueryService = stateQueryService;
        this.studentScopeService = studentScopeService;
        this.workbenchQueryService = workbenchQueryService;
        this.attachmentReadService = attachmentReadService;
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
        return transitionService.resubmitReturned(applicationId, request.version(), request.requestId(), user.userId());
    }

    @GetMapping("/{applicationId}/flow")
    public ApprovalFlowSnapshot flow(@PathVariable Long applicationId) {
        assertCanRead(currentUser(), applicationId);
        return flowQueryService.getFlow(applicationId);
    }

    @GetMapping("/{applicationId}/status")
    public ApprovalFlowSnapshot status(@PathVariable Long applicationId) {
        assertCanRead(currentUser(), applicationId);
        return flowQueryService.getFlow(applicationId);
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

    @GetMapping("/{applicationId}/attachments/{attachmentId}/content")
    public ResponseEntity<ByteArrayResource> readAttachment(
            @PathVariable Long applicationId,
            @PathVariable Long attachmentId
    ) {
        assertCanRead(currentUser(), applicationId);
        var content = attachmentReadService.readForAuthorizedReviewer(applicationId, attachmentId);
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(content.contentType());
        } catch (Exception ignored) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(content.content().length)
                .header("Content-Disposition", ContentDisposition.inline()
                        .filename(content.originalFilename()).build().toString())
                .body(new ByteArrayResource(content.content()));
    }

    @PostMapping("/{applicationId}/cancel")
    public ApplicationStatusResult cancel(@PathVariable Long applicationId, @Valid @RequestBody CancelRequest request) {
        LoginUser user = currentUser();
        requireRole(user, UserRole.SCHOOL);
        return cancellationService.cancel(applicationId, request.version(), request.requestId(), request.reason(), user.userId());
    }

    private ApplicationStatusResult review(Long applicationId, ApprovalRecordLevel level, ReviewRequest request) {
        LoginUser user = currentUser();
        return reviewService.review(user, applicationId,
                new ApprovalReviewService.ReviewCommand(
                        applicationId, level, request.action(), request.comment(), request.finalSubsidyAmount(),
                        request.version(), request.requestId()
                ));
    }

    private LoginUser currentUser() {
        return currentUserProvider.getRequiredUser();
    }

    private void requireRole(LoginUser user, UserRole role) {
        if (user.role() != role) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "当前用户没有所需角色");
        }
    }

    private void assertCanRead(LoginUser user, Long applicationId) {
        ApplicationStateSnapshot state = stateQueryService.getRequiredState(applicationId);
        if (user.role() == UserRole.SCHOOL) return;
        if (user.role() == UserRole.STUDENT) {
            if (user.studentId() != null && user.studentId().equals(state.studentId())) return;
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "只能查看本人申请");
        }
        boolean allowed = user.role() == UserRole.COUNSELOR
                ? studentScopeService.isCounselorResponsibleFor(user.userId(), state.studentId())
                : user.role() == UserRole.COLLEGE && user.collegeId() != null
                && studentScopeService.isStudentInCollege(state.studentId(), user.collegeId());
        if (!allowed) throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "当前用户不在该申请的数据范围内");
    }

    @PutMapping("/{applicationId}/editable-fields")
    public ApplicationStatusResult editAllowedFields(
            @PathVariable Long applicationId,
            @Valid @RequestBody EditFieldsRequest request
    ) {
        LoginUser user = currentUser();
        EditableFields fields = request.fields();
        return reviewService.editAllowedFields(
                user,
                applicationId,
                new ApprovalReviewService.EditFieldsCommand(
                        applicationId, fields.applicationReason(), fields.arrearsItems(), fields.giftItems(),
                        fields.expectedSubsidyAmount(), request.comment(), request.version(), request.requestId()
                )
        );
    }

    private ApprovalWorkbenchQueryService workbenchQueries() {
        return workbenchQueryService;
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

    public record EditFieldsRequest(
            @NotNull @Valid EditableFields fields,
            @NotBlank String comment,
            @NotNull Integer version,
            @NotBlank String requestId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record EditableFields(
            String applicationReason,
            @Valid List<ArrearsItemCommand> arrearsItems,
            @Valid List<GiftApplicationItemCommand> giftItems,
            BigDecimal expectedSubsidyAmount
    ) {
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
