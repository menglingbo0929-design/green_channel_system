package com.example.backend.model.vo.statistics;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** 后端为浏览器打印准备的完整报表数据；前端只负责排版并调用 window.print。 */
@Data
public class StatisticsReportPrintVO {
    private String title;
    private LocalDateTime generatedAt;
    private List<StatisticsReportColumnVO> columns = new ArrayList<>();
    private List<LinkedHashMap<String, Object>> records = new ArrayList<>();
    private long total;
}
