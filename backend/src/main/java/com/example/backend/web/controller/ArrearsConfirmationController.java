package com.example.backend.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.JsonResponse;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.confirmation.ArrearsConfirmationQueryDTO;
import com.example.backend.model.dto.confirmation.ConfirmArrearsDTO;
import com.example.backend.model.vo.confirmation.ConfirmResultVO;
import com.example.backend.model.vo.confirmation.PendingArrearsApplicationVO;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.service.IArrearsConfirmationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** School-side arrears confirmation endpoints protected by the JWT user context. */
@RestController
@RequestMapping("/api/confirm")
public class ArrearsConfirmationController {

    @Autowired
    private IArrearsConfirmationService arrearsConfirmationService;

    @Autowired
    private ICurrentUserProvider currentUserProvider;

    @GetMapping("/list")
    public JsonResponse<Page<PendingArrearsApplicationVO>> listPending(
            ArrearsConfirmationQueryDTO queryDTO,
            PageDTO pageDTO) {
        requireSchoolUser();
        return JsonResponse.success(arrearsConfirmationService.listPending(queryDTO, pageDTO));
    }

    @GetMapping("/app/{applicationId}")
    public JsonResponse<PendingArrearsApplicationVO> getPendingDetail(
            @PathVariable("applicationId") Long applicationId) {
        requireSchoolUser();
        return JsonResponse.success(arrearsConfirmationService.getPendingDetail(applicationId));
    }

    @PostMapping("/{applicationId}")
    public JsonResponse<ConfirmResultVO> confirm(
            @PathVariable("applicationId") Long applicationId,
            @Valid @RequestBody ConfirmArrearsDTO confirmDTO) {
        Long confirmUserId = requireSchoolUser();
        return JsonResponse.success(
                arrearsConfirmationService.confirm(applicationId, confirmDTO, confirmUserId),
                "欠费确认成功");
    }

    private Long requireSchoolUser() {
        com.example.backend.model.dto.LoginUser user = currentUserProvider.getRequiredUser();
        if (user.getRoles() == null || !user.getRoles().contains("SCHOOL")) {
            throw new SecurityException("仅学校管理员可操作欠费确认");
        }
        return user.getUserId();
    }
}
