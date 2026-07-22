package com.example.backend.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.JsonResponse;
import com.example.backend.model.dto.LoginUser;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.supplement.SupplementCreateDTO;
import com.example.backend.model.dto.supplement.SupplementQueryDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.model.vo.supplement.SupplementApplicationVO;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.service.ISupplementApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Offline supplementary applications are always operated by the JWT school user. */
@RestController
@RequestMapping("/api/supplements")
@RequiredArgsConstructor
public class SupplementApplicationController {

    private final ISupplementApplicationService supplementApplicationService;
    private final ICurrentUserProvider currentUsers;

    @GetMapping("/students")
    public JsonResponse<SchoolProxyStudentVO> findStudent(@RequestParam String studentNo) {
        return JsonResponse.success(supplementApplicationService
                .findStudent(studentNo, currentSchoolUserId()));
    }

    @GetMapping
    public JsonResponse<Page<SupplementApplicationVO>> pageSupplements(
            SupplementQueryDTO query, PageDTO page) {
        return JsonResponse.success(supplementApplicationService
                .pageSupplements(query, page, currentSchoolUserId()));
    }

    @GetMapping("/{applicationId}")
    public JsonResponse<SupplementApplicationVO> getSupplement(@PathVariable Long applicationId) {
        return JsonResponse.success(supplementApplicationService
                .getSupplement(applicationId, currentSchoolUserId()));
    }

    @PostMapping
    public JsonResponse<SupplementApplicationVO> createSupplement(
            @Valid @RequestBody SupplementCreateDTO request) {
        return JsonResponse.success(supplementApplicationService
                .createSupplement(request, currentSchoolUserId()), "线下补录完成");
    }

    private Long currentSchoolUserId() {
        LoginUser user = currentUsers.getRequiredUser();
        if (user.getRoles() == null || !user.getRoles().contains("SCHOOL")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SCHOOL users can manage supplements");
        }
        return user.getUserId();
    }

    /** 返回已通过身份校验的学校管理员用户 ID。 */
    private Long requireSchoolUser() {
        LoginUser user = currentUserProvider.getRequiredUser();
        if (user.getRoles() == null || !user.getRoles().contains("SCHOOL")) {
            throw new SecurityException("仅学校管理员可执行线下补录");
        }
        return user.getUserId();
    }
}
