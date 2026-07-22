package com.example.backend.model.vo.statistics;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 自定义字段后的统计明细分页结果。
 *
 * <p>records 使用 LinkedHashMap 保证 JSON 字段、Excel 列和打印列严格按照
 * columns 的顺序输出；未被用户选择的字段不会进入响应。</p>
 */
@Data
public class StatisticsReportPageVO {
    private List<StatisticsReportColumnVO> columns = new ArrayList<>();
    private List<LinkedHashMap<String, Object>> records = new ArrayList<>();
    private long total;
    private long pageNo;
    private long pageSize;
    private long pages;
}
