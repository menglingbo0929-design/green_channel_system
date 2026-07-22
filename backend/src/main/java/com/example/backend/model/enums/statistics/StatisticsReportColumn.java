package com.example.backend.model.enums.statistics;

import java.util.List;

/**
 * 6.1.7 允许查询、导出和打印的报表字段白名单。
 *
 * <p>key 是稳定的前后端契约，不是数据库字段名；成员二实现排序 SQL 时必须显式映射，
 * 不能把 key 直接拼到 SQL。width 是 Excel 的建议字符宽度。</p>
 */
public enum StatisticsReportColumn {
    APPLICATION_ID("applicationId", "申请ID", 12),
    APPLICATION_NO("applicationNo", "申请编号", 22),
    STUDENT_NO("studentNo", "学号", 16),
    STUDENT_NAME("studentName", "姓名", 12),
    COLLEGE_NAME("collegeName", "学院", 20),
    MAJOR_NAME("majorName", "专业", 20),
    GRADE_NAME("gradeName", "年级", 12),
    CLASS_NAME("className", "班级", 18),
    APPLICATION_TYPE("applicationType", "申请类型", 20),
    BATCH_TYPE("batchType", "批次体系", 18),
    BATCH_ID("batchId", "批次ID", 12),
    BATCH_NAME("batchName", "批次名称", 24),
    APPLICATION_STATUS("applicationStatus", "申请状态", 20),
    APPLICATION_SOURCE("applicationSource", "申请来源", 18),
    ARREARS_ITEM_NAMES("arrearsItemNames", "欠费项目", 28),
    ARREARS_REASON_NAME("arrearsReasonName", "欠费原因", 24),
    DECLARED_AMOUNT("declaredAmount", "申报金额", 14),
    CONFIRMED_AMOUNT("confirmedAmount", "确认金额", 14),
    GIFT_ITEM_NAMES("giftItemNames", "礼包物品", 28),
    SUBSIDY_AMOUNT("subsidyAmount", "补助金额", 14),
    APPLICATION_TIME("applicationTime", "申请时间", 21),
    COMPLETION_TIME("completionTime", "完成时间", 21);

    private final String key;
    private final String title;
    private final int width;

    StatisticsReportColumn(String key, String title, int width) {
        this.key = key;
        this.title = title;
        this.width = width;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public int getWidth() {
        return width;
    }

    /** 页面首次进入时使用的简洁默认列。 */
    public static List<StatisticsReportColumn> defaultColumns() {
        return List.of(
                APPLICATION_NO,
                STUDENT_NO,
                STUDENT_NAME,
                COLLEGE_NAME,
                MAJOR_NAME,
                GRADE_NAME,
                CLASS_NAME,
                APPLICATION_TYPE,
                BATCH_NAME,
                APPLICATION_STATUS,
                CONFIRMED_AMOUNT,
                APPLICATION_TIME
        );
    }

    /** 把公开 key 转为枚举；演示页面只会传下拉框中已有的字段。 */
    public static StatisticsReportColumn fromKey(String key) {
        for (StatisticsReportColumn column : values()) {
            if (column.key.equals(key)) {
                return column;
            }
        }
        return null;
    }
}
