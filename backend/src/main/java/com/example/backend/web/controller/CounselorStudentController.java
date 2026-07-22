package com.example.backend.web.controller;

import com.example.backend.common.JsonResponse;
import com.example.backend.model.dto.LoginUser;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.service.CounselorStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Administrative endpoints for the counselor responsibility scope. */
@RestController
@RequestMapping("/api/counselor-students")
@RequiredArgsConstructor
public class CounselorStudentController {

    private final CounselorStudentService counselorStudents;
    private final ICurrentUserProvider currentUsers;

    @PutMapping("/{counselorUserId}/students/{studentId}")
    public JsonResponse<Void> assign(@PathVariable Long counselorUserId, @PathVariable Long studentId) {
        requireSchoolRole();
        counselorStudents.assign(counselorUserId, studentId);
        return JsonResponse.successMessage("辅导员负责关系已设置");
    }

    @DeleteMapping("/{counselorUserId}/students/{studentId}")
    public JsonResponse<Void> remove(@PathVariable Long counselorUserId, @PathVariable Long studentId) {
        requireSchoolRole();
        counselorStudents.remove(counselorUserId, studentId);
        return JsonResponse.successMessage("辅导员负责关系已解除");
    }

    @GetMapping("/{counselorUserId}/students")
    public JsonResponse<List<Long>> listStudents(@PathVariable Long counselorUserId) {
        requireSchoolRole();
        return JsonResponse.success(counselorStudents.listStudentIdsByCounselor(counselorUserId));
    }

    private void requireSchoolRole() {
        LoginUser user = currentUsers.getRequiredUser();
        if (user.getRoles() == null || !user.getRoles().contains("SCHOOL")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SCHOOL users can manage counselor scope");
        }
    }
}
