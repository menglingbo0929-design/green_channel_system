package com.example.backend.approval.web;

import com.example.backend.approval.domain.ApprovalErrorCode;
import com.example.backend.approval.domain.ApprovalException;
import com.example.backend.approval.port.CurrentUserProvider;
import com.example.backend.approval.port.LoginUser;
import com.example.backend.approval.port.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Batch endpoints are published now; execution waits for member-one scope and
 * batch ports plus the member-two resource bridge. */
@RestController
@RequestMapping("/api/approval-submissions")
public class ApprovalSubmissionController {

    private final ObjectProvider<CurrentUserProvider> currentUserProvider;

    public ApprovalSubmissionController(ObjectProvider<CurrentUserProvider> currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/status")
    public Map<String, String> status() {
        requireReviewRole();
        throw new ApprovalIntegrationUnavailableException("成员一批次/范围 Service 和成员二申请列表 Service");
    }

    @PostMapping("/counselor/initial")
    public Map<String, String> submitCounselorInitial(@Valid @RequestBody BatchRequest request) {
        requireRole(UserRole.COUNSELOR);
        throw new ApprovalIntegrationUnavailableException("成员一批次/范围 Service 和成员二资源 Service");
    }

    @PostMapping("/college/initial")
    public Map<String, String> submitCollegeInitial(@Valid @RequestBody BatchRequest request) {
        requireRole(UserRole.COLLEGE);
        throw new ApprovalIntegrationUnavailableException("成员一批次/范围 Service 和成员二资源 Service");
    }

    @PostMapping("/return-resubmit")
    public Map<String, String> returnResubmit(@Valid @RequestBody ReturnResubmitRequest request) {
        requireReviewRole();
        throw new ApprovalIntegrationUnavailableException("成员一数据范围 Service 和成员二资源 Service");
    }

    private void requireReviewRole() {
        LoginUser user = currentUser();
        if (user.role() != UserRole.COUNSELOR && user.role() != UserRole.COLLEGE) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "当前角色不能批量上报");
        }
    }

    private void requireRole(UserRole role) {
        if (currentUser().role() != role) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "当前用户没有所需角色");
        }
    }

    private LoginUser currentUser() {
        CurrentUserProvider provider = currentUserProvider.getIfAvailable();
        if (provider == null) throw new ApprovalIntegrationUnavailableException("成员一 CurrentUserProvider");
        return provider.getRequiredUser();
    }

    public record BatchRequest(@NotBlank String batchType, @NotNull Long batchId, @NotBlank String requestId) {
    }

    public record ReturnResubmitRequest(@NotNull Long applicationId, @NotNull Integer version, @NotBlank String requestId) {
    }
}
