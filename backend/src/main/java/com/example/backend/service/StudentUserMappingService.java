package com.example.backend.service;

import com.example.backend.model.domain.Student;

/** Trusted lookup for the one active student profile associated with a login user. */
public interface StudentUserMappingService {
    Student findActiveStudentByUserId(Long userId);
}
