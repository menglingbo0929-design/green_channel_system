package com.example.backend.service;

import com.example.backend.model.dto.statistics.StatisticsReportQueryDTO;
import com.example.backend.model.vo.statistics.StatisticsExportFileVO;
import com.example.backend.model.vo.statistics.StatisticsReportPageVO;
import com.example.backend.model.vo.statistics.StatisticsReportPrintVO;

import java.io.IOException;

/** 6.1.7 统计明细、历史记录、Excel 和打印的学校端业务接口。 */
public interface IStatisticsReportService {

    /** 按筛选、排序和自定义列查询最终状态申请明细。 */
    StatisticsReportPageVO queryDetails(StatisticsReportQueryDTO query, Long currentUserId);

    /** 查询指定历史批次；必须同时提供 batchType 和 batchId。 */
    StatisticsReportPageVO queryHistory(StatisticsReportQueryDTO query, Long currentUserId);

    /** 生成最多 1000 行、可直接交给浏览器排版的打印数据。 */
    StatisticsReportPrintVO buildPrintData(StatisticsReportQueryDTO query, Long currentUserId);

    /** 使用流式工作簿生成真正的 xlsx 文件。 */
    StatisticsExportFileVO exportExcel(
            StatisticsReportQueryDTO query,
            Long currentUserId
    ) throws IOException;
}
