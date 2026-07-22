package com.example.backend.web.controller;

import com.example.backend.common.JsonResponse;
import com.example.backend.model.dto.statistics.StatisticsFilterDTO;
import com.example.backend.model.vo.statistics.ApplicationStatisticsVO;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.service.IApplicationStatisticsService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 6.1.5、6.1.6 学校端统计接口。
 *
 * <p>当前登录人与 SCHOOL 角色均从成员一 JWT 登录上下文读取，Controller 不接收
 * 也不信任前端传入的用户 ID 或角色。</p>
 */
@RestController
@RequestMapping("/api/statistics/applications")
@RequiredArgsConstructor
public class ApplicationStatisticsController {
    private final IApplicationStatisticsService applicationStatisticsService;
    private final ICurrentUserProvider currentUserProvider;

    /**
     * 查询最终状态申请的统计大盘。
     *
     * <p>所有筛选字段由 Spring 自动绑定到 StatisticsFilterDTO；Controller 不接收或信任角色、
     * 学生范围等前端权限字段。</p>
     */
    @GetMapping("/summary")
    public JsonResponse<ApplicationStatisticsVO> summary(@Valid StatisticsFilterDTO filter) {
        Long currentUserId = currentUserProvider.getRequiredUser().getUserId();
        return JsonResponse.success(
                applicationStatisticsService.querySummary(filter, currentUserId));
    }
}
