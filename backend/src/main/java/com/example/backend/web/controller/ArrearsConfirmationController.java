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
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 6.1.1 欠费信息最终确认模块的学校端 Controller。
 *
 * <p>这里只放“待确认、查看申报金额、执行确认”接口。
 * 欠费单据查询、学生查看、打印会由下一个独立 Voucher 模块提供。</p>
 */
@RestController
@RequestMapping("/api/confirm")
public class ArrearsConfirmationController {

    @Autowired
    private IArrearsConfirmationService arrearsConfirmationService;

    @Autowired
    private ICurrentUserProvider currentUserProvider;

    /**
     * 查询待确认欠费申请。
     *
     * <p>返回学生基础信息和申报金额，学校管理员在列表中选择一笔申请后，
     * 再调用 app 详情接口查看并填写实际确认金额。</p>
     */
    @GetMapping("/list")
    public JsonResponse<Page<PendingArrearsApplicationVO>> listPending(
            ArrearsConfirmationQueryDTO queryDTO,
            PageDTO pageDTO) {
        requireSchoolUser();
        return JsonResponse.success(arrearsConfirmationService.listPending(queryDTO, pageDTO));
    }

    /**
     * 查看某笔待确认申请详情，重点返回学生申报金额。
     *
     * <p>示例：GET /api/confirm/app/1001。</p>
     */
    @GetMapping("/app/{applicationId}")
    public JsonResponse<PendingArrearsApplicationVO> getPendingDetail(
            @PathVariable("applicationId") Long applicationId) {
        requireSchoolUser();
        return JsonResponse.success(arrearsConfirmationService.getPendingDetail(applicationId));
    }

    /**
     * 填写实际欠费金额并完成最终确认。
     *
     * <p>请求体必须包含 confirmedAmount、version、requestId：前两者分别用于金额
     * 校验与乐观锁，requestId 用于幂等。确认人与 SCHOOL 角色均从成员一 JWT 登录
     * 上下文读取，前端不再传递临时用户请求头。</p>
     */
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
