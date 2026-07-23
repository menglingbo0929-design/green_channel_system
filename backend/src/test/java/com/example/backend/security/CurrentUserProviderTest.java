package com.example.backend.security;

import com.example.backend.model.domain.Student;
import com.example.backend.model.dto.LoginUser;
import com.example.backend.service.StudentUserMappingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserProviderTest {

    private final StudentUserMappingService mappings = mock(StudentUserMappingService.class);
    private final CurrentUserProvider provider = new CurrentUserProvider(mappings);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsUserAndRolesFromJwtAuthenticationContext() {
        var authentication = new UsernamePasswordAuthenticationToken("student_demo", null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT"),
                        new SimpleGrantedAuthority("ROLE_COUNSELOR")));
        authentication.setDetails(Map.of("userId", 5, "studentId", 1L, "collegeId", 9));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        LoginUser current = provider.getRequiredUser();

        assertEquals(5L, current.getUserId());
        assertEquals("student_demo", current.getLoginName());
        assertEquals(java.util.List.of("STUDENT", "COUNSELOR"), current.getRoles());
        assertEquals(1L, current.getStudentId());
        assertEquals(9L, current.getCollegeId());
    }

    @Test
    void resolvesStudentProfileWhenAnOlderTokenDoesNotContainStudentId() {
        Student student = new Student();
        student.setId(42L);
        when(mappings.findActiveStudentByUserId(5L)).thenReturn(student);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "student_demo", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        authentication.setDetails(Map.of("userId", 5L));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        LoginUser current = provider.getRequiredUser();

        assertEquals(42L, current.getStudentId());
    }

    @Test
    void rejectsUnauthenticatedContext() {
        assertThrows(IllegalStateException.class, provider::getRequiredUser);
    }
}
