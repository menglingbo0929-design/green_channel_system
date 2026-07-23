package com.example.backend.web.controller;

import com.example.backend.model.dto.StudentRecommendationView;
import com.example.backend.common.exception.ApplicationException;
import com.example.backend.service.impl.RecommendationService;
import com.example.backend.model.dto.LoginUser;
import com.example.backend.security.ICurrentUserProvider;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class StudentRecommendationController {
    private final RecommendationService service;
    private final ICurrentUserProvider currentUsers;

    public StudentRecommendationController(RecommendationService service, ICurrentUserProvider currentUsers) {
        this.service = service;
        this.currentUsers = currentUsers;
    }

    @GetMapping("/mine")
    public List<StudentRecommendationView> mine() { return service.findMine(currentStudent().getStudentId()); }

    @PostMapping("/{id}/read")
    public void read(@PathVariable Long id) { service.markRead(id, currentStudent().getStudentId()); }

    private LoginUser currentStudent() {
        LoginUser user = currentUsers.getRequiredUser();
        if (user.getStudentId() == null || user.getRoles() == null || !user.getRoles().contains("STUDENT")) {
            throw new ApplicationException("APPLICATION_FORBIDDEN", HttpStatus.FORBIDDEN, "当前登录用户不是学生账号");
        }
        return user;
    }
}
