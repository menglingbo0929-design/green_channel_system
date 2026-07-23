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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
//归属第四部分
//此controller对应,欠费状态审核相关操作

/** School-side arrears confirmation endpoints protected by the JWT user context. */
@RestController
@RequestMapping("/api/confirm")
@RequiredArgsConstructor
public class ArrearsConfirmationController {
    private final IArrearsConfirmationService arrearsConfirmationService;
    private final ICurrentUserProvider currentUserProvider;
    //打开欠费页面后可以看到欠费待确认列表
    @GetMapping("/list")
    public JsonResponse<Page<PendingArrearsApplicationVO>> listPending(
            ArrearsConfirmationQueryDTO queryDTO,
            PageDTO pageDTO) {
        requireSchoolUser();
        return JsonResponse.success(arrearsConfirmationService.listPending(queryDTO, pageDTO));
    }

    //欠费确认功能，点击确认金额之后会谈一个学生信息的弹窗
    @GetMapping("/app/{applicationId}")
    public JsonResponse<PendingArrearsApplicationVO> getPendingDetail(
            @PathVariable("applicationId") Long applicationId) {
        requireSchoolUser();
        return JsonResponse.success(arrearsConfirmationService.getPendingDetail(applicationId));
    }

    //对应弹窗里面的确定按钮点击事件
    @PostMapping("/{applicationId}")
    public JsonResponse<ConfirmResultVO> confirm(
            @PathVariable("applicationId") Long applicationId,
            @Valid @RequestBody ConfirmArrearsDTO confirmDTO) {
        Long confirmUserId = requireSchoolUser();
        return JsonResponse.success(
                arrearsConfirmationService.confirm(applicationId, confirmDTO, confirmUserId),
                "欠费确认成功");
    }
    //在每次调用controller的时候先确认用户是否为学校管理圆，确认后才能进入。并取得该管理员的id，作为欠费信息确认人记录下来
    private Long requireSchoolUser() {
        com.example.backend.model.dto.LoginUser user = currentUserProvider.getRequiredUser();
        if (user.getRoles() == null || !user.getRoles().contains("SCHOOL")) {
            throw new SecurityException("仅学校管理员可操作欠费确认");
        }
        return user.getUserId();
    }
}
