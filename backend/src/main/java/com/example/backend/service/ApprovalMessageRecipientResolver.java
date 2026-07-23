package com.example.backend.service;

/** Member-one integration point for resolving a student's login user ID. */
public interface ApprovalMessageRecipientResolver {
    Long getStudentUserId(Long studentId);
}
