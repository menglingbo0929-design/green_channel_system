package com.example.backend.web.controller;

import com.example.backend.application.dto.PageResult;
import com.example.backend.application.dto.PendingArrearsApplication;
import com.example.backend.application.dto.PendingArrearsQuery;
import com.example.backend.application.port.ArrearsConfirmationApplicationPort;
import com.example.backend.common.JsonResponse;
import com.example.backend.model.dto.LoginUser;
import com.example.backend.security.ICurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** JWT-protected entry point for the school arrears-confirmation list. */
@RestController
@RequestMapping("/api/confirm")
@RequiredArgsConstructor
public class ArrearsConfirmationController {

    private final ArrearsConfirmationApplicationPort applications;
    private final ICurrentUserProvider currentUsers;

    @GetMapping("/list")
    public JsonResponse<PageResult<PendingArrearsApplication>> list(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        requireSchoolUser();
        return JsonResponse.success(applications.pagePending(new PendingArrearsQuery(pageNo, pageSize)));
    }

    private void requireSchoolUser() {
        LoginUser user = currentUsers.getRequiredUser();
        if (user.getRoles() == null || !user.getRoles().contains("SCHOOL")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SCHOOL users can confirm arrears");
        }
    }
}
