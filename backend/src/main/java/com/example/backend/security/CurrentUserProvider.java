package com.example.backend.security;

import com.example.backend.model.dto.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Exposes only the identity established by {@link JwtAuthenticationFilter}. */
@Component
public class CurrentUserProvider implements ICurrentUserProvider {

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
        return new LoginUser(userId, loginName, roles,
                asLong(details.get("studentId")), asLong(details.get("collegeId")));
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
