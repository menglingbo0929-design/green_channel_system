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

/**
 * 6.1.7 报表明细、历史批次、Excel 导出和打印 Controller。
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

    /** 查询最终审核通过或已完成的统计明细。 */
    @GetMapping("/details")
    public JsonResponse<StatisticsReportPageVO> details(@Valid StatisticsReportQueryDTO query) {
        Long currentUserId = currentUserProvider.getRequiredUser().getUserId();
        return JsonResponse.success(
                statisticsReportService.queryDetails(query, currentUserId)
        );
    }

    /** 查询指定 batchType + batchId 的历史申请记录。 */
    @GetMapping("/history")
    public JsonResponse<StatisticsReportPageVO> history(@Valid StatisticsReportQueryDTO query) {
        Long currentUserId = currentUserProvider.getRequiredUser().getUserId();
        return JsonResponse.success(
                statisticsReportService.queryHistory(query, currentUserId)
        );
    }

    /** 返回与 Excel 相同字段顺序的打印数据，由浏览器完成最终打印。 */
    @GetMapping("/print")
    public JsonResponse<StatisticsReportPrintVO> print(@Valid StatisticsReportQueryDTO query) {
        Long currentUserId = currentUserProvider.getRequiredUser().getUserId();
        return JsonResponse.success(
                statisticsReportService.buildPrintData(query, currentUserId)
        );
    }

    /**
     * 下载 xlsx 文件。
     *
     * <p>Excel 是二进制响应，不能再包 JsonResponse；失败时仍由全局异常处理器
     * 返回统一 JSON 错误。no-store 防止浏览器或代理缓存含学生信息的报表。</p>
     */
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
