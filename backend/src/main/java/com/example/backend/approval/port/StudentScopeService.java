package com.example.backend.approval.port;

/** Member-one integration point for counselor and college data-scope checks. */
public interface StudentScopeService {
    boolean isCounselorResponsibleFor(Long counselorUserId, Long studentId);
    boolean isStudentInCollege(Long studentId, Long collegeId);
}
