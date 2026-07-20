package com.example.backend.model.dto.statistics;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 6.1.7 统计明细、Excel 导出和打印共用的查询参数。
 *
 * <p>本类继承 6.1.6 的批次、组织、类型、状态、欠费项目和时间筛选条件，
 * 再增加分页、排序以及自定义列。三个输出入口必须接收同一个 DTO，避免
 * 页面列表、Excel 和打印报表出现筛选口径不一致。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StatisticsReportQueryDTO extends StatisticsFilterDTO {

    /** 明细页码，从 1 开始；导出时由 Service 按固定批大小循环覆盖。 */
    private Integer pageNo = 1;

    /** 页面查询默认 20 条、最大 100 条；导出内部固定使用 500 条一批。 */
    private Integer pageSize = 20;

    /**
     * 用户选择的报表字段 key。为空时使用后端确定的默认列，不能由前端传数据库字段名。
     */
    private List<String> columns = new ArrayList<>();

    /** 排序字段同样使用报表字段 key，默认按申请时间排序。 */
    private String sortBy = "applicationTime";

    /** 只允许 ASC 或 DESC，默认最新申请在前。 */
    private String sortDirection = "DESC";
}
