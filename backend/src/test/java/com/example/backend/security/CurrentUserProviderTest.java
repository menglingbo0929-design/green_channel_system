package com.example.backend.security;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrentUserProviderTest {

    private final CurrentUserProvider provider = new CurrentUserProvider();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsUserAndRolesFromJwtAuthenticationContext() {
        var authentication = new UsernamePasswordAuthenticationToken("student_demo", null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT"),
                        new SimpleGrantedAuthority("ROLE_COUNSELOR")));
        authentication.setDetails(Map.of("userId", 5, "studentId", 1L, "collegeId", 9));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var user = provider.getRequiredUser();

        assertEquals(5L, user.getUserId());
        assertEquals("student_demo", user.getLoginName());
        assertEquals(java.util.List.of("STUDENT", "COUNSELOR"), user.getRoles());
        assertEquals(1L, user.getStudentId());
        assertEquals(9L, user.getCollegeId());
    }

    @Test
    void rejectsUnauthenticatedContext() {
        assertThrows(IllegalStateException.class, provider::getRequiredUser);
    }
}
