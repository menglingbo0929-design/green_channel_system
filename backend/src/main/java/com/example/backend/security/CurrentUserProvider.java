package com.example.backend.security;

import com.example.backend.model.dto.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider implements ICurrentUserProvider {

    @Override
    @SuppressWarnings("unchecked")
    public LoginUser getRequiredUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("当前请求未认证");
        }

        String loginName = (String) auth.getPrincipal();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();

        Long userId = details != null ? (Long) details.get("userId") : null;
        Long studentId = details != null ? (Long) details.get("studentId") : null;
        Long collegeId = details != null ? (Long) details.get("collegeId") : null;

        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(s -> s.startsWith("ROLE_") ? s.substring(5) : s)
                .collect(Collectors.toList());

        return new LoginUser(userId, loginName, roles, studentId, collegeId);
    }
}
