package com.example.backend.service;

import java.math.BigDecimal;

/**
 * Member-two integration point. The application module remains the resource and
 * application-table owner; this interface only expresses what approval needs.
 */
public interface ApprovalResourceService {
    void reserveOnSubmit(Long applicationId, String requestId, Long operatorId);
    void applyCounselorSubsidyAmount(Long applicationId, BigDecimal amount, String requestId, Long operatorId);
    void validateCollegeApproval(Long applicationId);
    void confirmOnSchoolApproval(Long applicationId, String requestId, Long operatorId);
    void handleReturn(Long applicationId, String requestId, Long operatorId);
    void releaseOnReject(Long applicationId, String requestId, Long operatorId);
    void releaseOnCancel(Long applicationId, String requestId, Long operatorId);
}
