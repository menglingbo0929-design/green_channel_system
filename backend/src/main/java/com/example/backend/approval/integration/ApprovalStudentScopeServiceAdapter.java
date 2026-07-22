package com.example.backend.approval.integration;

import com.example.backend.approval.port.StudentScopeService;
import org.springframework.stereotype.Component;

/** Bridges member one's public student-scope service into the approval port. */
@Component
public class ApprovalStudentScopeServiceAdapter implements StudentScopeService {

    private final com.example.backend.service.StudentScopeService delegate;

    public ApprovalStudentScopeServiceAdapter(
            com.example.backend.service.StudentScopeService delegate
    ) {
        this.delegate = delegate;
    }

    @Override
    public boolean isCounselorResponsibleFor(Long counselorUserId, Long studentId) {
        return delegate.isCounselorResponsibleFor(counselorUserId, studentId);
    }

    @Override
    public boolean isStudentInCollege(Long studentId, Long collegeId) {
        return delegate.isStudentInCollege(studentId, collegeId);
    }
}
