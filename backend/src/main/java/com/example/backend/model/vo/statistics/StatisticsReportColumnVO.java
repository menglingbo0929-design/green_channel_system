package com.example.backend.model.vo.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 前端表格、Excel 表头和打印表头共用的字段说明。 */
@Data
@AllArgsConstructor
public class StatisticsReportColumnVO {
    /** 稳定字段 key，例如 applicationNo；不是数据库列名。 */
    private String key;
    /** 中文表头。 */
    private String title;
}
