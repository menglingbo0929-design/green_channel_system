package com.example.backend.web.controller;

import com.example.backend.common.exception.ApprovalErrorCode;
import com.example.backend.common.exception.ApprovalException;
import com.example.backend.model.dto.ApprovalSubmissionResult;
import com.example.backend.service.ApprovalSubmissionService;
import com.example.backend.model.dto.ApprovalSubmissionStatus;
import com.example.backend.model.domain.BatchType;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.model.dto.LoginUser;
import com.example.backend.model.domain.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 辅导员、学院批量上报入口；核心依赖必须在应用启动时完整装配。 */
@RestController
@RequestMapping("/api/approval-submissions")
public class ApprovalSubmissionController {

    private final ICurrentUserProvider currentUserProvider;
    private final ApprovalSubmissionService submissionService;

    public ApprovalSubmissionController(
            ICurrentUserProvider currentUserProvider,
            ApprovalSubmissionService submissionService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.submissionService = submissionService;
    }

    @GetMapping("/status")
    public ApprovalSubmissionStatus status(@RequestParam BatchType batchType, @RequestParam Long batchId) {
        LoginUser user = requireReviewRole();
        return submissionService.getStatus(user, batchType, batchId);
    }

    @PostMapping("/counselor/initial")
    public ApprovalSubmissionResult submitCounselorInitial(@Valid @RequestBody BatchRequest request) {
        return submissionService.submitInitial(requireRole(UserRole.COUNSELOR), request.toBatchType(), request.batchId(), request.requestId());
    }

    @PostMapping("/college/initial")
    public ApprovalSubmissionResult submitCollegeInitial(@Valid @RequestBody BatchRequest request) {
        return submissionService.submitInitial(requireRole(UserRole.COLLEGE), request.toBatchType(), request.batchId(), request.requestId());
    }

    @PostMapping("/return-resubmit")
    public ApprovalSubmissionResult returnResubmit(@Valid @RequestBody ReturnResubmitRequest request) {
        return submissionService.submitReturnResubmit(
                requireReviewRole(), request.applicationId(), request.version(), request.requestId()
        );
    }

    private LoginUser requireReviewRole() {
        LoginUser user = currentUser();
        if (user.role() != UserRole.COUNSELOR && user.role() != UserRole.COLLEGE) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "当前角色不能批量上报");
        }
        return user;
    }

    private LoginUser requireRole(UserRole role) {
        LoginUser user = currentUser();
        if (user.role() != role) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "当前用户没有所需角色");
        }
        return user;
    }

    private LoginUser currentUser() {
        return currentUserProvider.getRequiredUser();
    }

    public record BatchRequest(@NotBlank String batchType, @NotNull Long batchId, @NotBlank String requestId) {
        BatchType toBatchType() {
            try {
                return BatchType.valueOf(batchType);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("batchType must be GREEN_CHANNEL or SUBSIDY", exception);
            }
        }
    }

    public record ReturnResubmitRequest(@NotNull Long applicationId, @NotNull Integer version, @NotBlank String requestId) {
    }
}
