package com.example.backend.approval.port;

public record LoginUser(
        Long userId,
        UserRole role,
        Long studentId,
        Long collegeId
) {
}
