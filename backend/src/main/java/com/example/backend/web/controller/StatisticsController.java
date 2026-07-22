package com.example.backend.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.JsonResponse;
import com.example.backend.model.dto.LoginUser;
import com.example.backend.model.dto.statistics.StatisticsFilterDTO;
import com.example.backend.model.dto.statistics.StatisticsReportQueryDTO;
import com.example.backend.model.vo.statistics.ApplicationStatisticsVO;
import com.example.backend.model.vo.statistics.StatisticsReportRowVO;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.service.port.ApplicationStatisticsQueryPort;
import com.example.backend.service.port.StatisticsReportQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** JWT-protected entry points for the school statistics dashboard. */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final ApplicationStatisticsQueryPort statistics;
    private final StatisticsReportQueryPort reports;
    private final ICurrentUserProvider currentUsers;

    @GetMapping("/applications/summary")
    public JsonResponse<ApplicationStatisticsVO> summary(StatisticsFilterDTO filter) {
        requireSchoolUser();
        return JsonResponse.success(statistics.queryFinalStatistics(filter));
    }

    @GetMapping("/reports/details")
    public JsonResponse<Page<StatisticsReportRowVO>> details(StatisticsReportQueryDTO query) {
        LoginUser user = requireSchoolUser();
        return JsonResponse.success(reports.queryReportPage(query, user.getUserId()));
    }

    private LoginUser requireSchoolUser() {
        LoginUser user = currentUsers.getRequiredUser();
        if (user.getRoles() == null || !user.getRoles().contains("SCHOOL")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SCHOOL users can view statistics");
        }
        return user;
    }
}
