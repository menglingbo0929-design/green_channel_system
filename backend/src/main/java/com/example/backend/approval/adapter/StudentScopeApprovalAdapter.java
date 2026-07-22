package com.example.backend.approval.adapter;

import com.example.backend.approval.port.StudentScopeService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/** Reuses member one's student-scope checks without exposing its persistence layer to approval. */
@Primary
@Service
public class StudentScopeApprovalAdapter implements StudentScopeService {

    private final com.example.backend.service.StudentScopeService studentScopes;

    public StudentScopeApprovalAdapter(com.example.backend.service.StudentScopeService studentScopes) {
        this.studentScopes = studentScopes;
    }

    @Override
    public boolean isCounselorResponsibleFor(Long counselorUserId, Long studentId) {
        return studentScopes.isCounselorResponsibleFor(counselorUserId, studentId);
    }

    @Override
    public boolean isStudentInCollege(Long studentId, Long collegeId) {
        return studentScopes.isStudentInCollege(studentId, collegeId);
    }
}
