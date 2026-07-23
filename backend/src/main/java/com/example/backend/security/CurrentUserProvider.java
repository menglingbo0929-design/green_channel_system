package com.example.backend.security;

import com.example.backend.model.dto.LoginUser;
import com.example.backend.model.domain.Student;
import com.example.backend.service.StudentUserMappingService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Exposes only the identity established by {@link JwtAuthenticationFilter}. */
@Component
public class CurrentUserProvider implements ICurrentUserProvider {

    private final StudentUserMappingService studentUserMappings;

    public CurrentUserProvider(StudentUserMappingService studentUserMappings) {
        this.studentUserMappings = studentUserMappings;
    }

    @Override
    @SuppressWarnings("unchecked")
    public LoginUser getRequiredUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof String loginName)
                || !(authentication.getDetails() instanceof Map<?, ?> rawDetails)) {
            throw new IllegalStateException("Current request is not authenticated");
        }

        Map<String, Object> details = (Map<String, Object>) rawDetails;
        Long userId = asLong(details.get("userId"));
        if (userId == null) {
            throw new IllegalStateException("JWT does not contain userId");
        }

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(value -> value.startsWith("ROLE_") ? value.substring(5) : value)
                .distinct()
                .toList();
        Long studentId = asLong(details.get("studentId"));
        // A database repair can link a previously unbound student account while
        // the browser still holds a JWT issued before that mapping existed.
        // Recover the trusted mapping server-side so every student endpoint
        // continues to work without requiring the user to log in again.
        if (studentId == null && roles.contains("STUDENT")) {
            Student student = studentUserMappings.findActiveStudentByUserId(userId);
            if (student != null) studentId = student.getId();
        }
        return new LoginUser(userId, loginName, roles, studentId, asLong(details.get("collegeId")));
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
