package com.example.backend.web.controller;

import com.example.backend.common.JsonResponse;
import com.example.backend.model.dto.statistics.StatisticsReportQueryDTO;
import com.example.backend.model.vo.statistics.StatisticsExportFileVO;
import com.example.backend.model.vo.statistics.StatisticsReportPageVO;
import com.example.backend.model.vo.statistics.StatisticsReportPrintVO;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.service.IStatisticsReportService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
//
/**
 *报表明细、历史批次、Excel 导出和打印 Controller。
 *
 * <p>Controller 采用构造器注入，只接收参数和组织 HTTP 响应；字段白名单、分页、权限、导出上限和
 * Excel 生成都由 Service 处理。</p>
 */
@RestController
@RequestMapping("/api/statistics/reports")
@RequiredArgsConstructor
public class StatisticsReportController {
    private final IStatisticsReportService statisticsReportService;
    private final ICurrentUserProvider currentUserProvider;

    //支持统计明细表的查看与分页
    @GetMapping("/details")
    public JsonResponse<StatisticsReportPageVO> details(@Valid StatisticsReportQueryDTO query) {
        Long currentUserId = currentUserProvider.getRequiredUser().getUserId();
        return JsonResponse.success(
                statisticsReportService.queryDetails(query, currentUserId)
        );
    }

    //查看历史批次对应的统计内容
    @GetMapping("/history")
    public JsonResponse<StatisticsReportPageVO> history(@Valid StatisticsReportQueryDTO query) {
        Long currentUserId = currentUserProvider.getRequiredUser().getUserId();
        return JsonResponse.success(
                statisticsReportService.queryHistory(query, currentUserId)
        );
    }

    //报表打印功能，只是支持进入打印预览页面
    @GetMapping("/print")
    public JsonResponse<StatisticsReportPrintVO> print(@Valid StatisticsReportQueryDTO query) {
        Long currentUserId = currentUserProvider.getRequiredUser().getUserId();
        return JsonResponse.success(
                statisticsReportService.buildPrintData(query, currentUserId)
        );
    }

    //excel导出
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@Valid StatisticsReportQueryDTO query) throws IOException {
        Long currentUserId = currentUserProvider.getRequiredUser().getUserId();
        StatisticsExportFileVO file = statisticsReportService
                .exportExcel(query, currentUserId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFileName() + "\""
                )
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .contentLength(file.getContent().length)
                .body(file.getContent());
    }
}
