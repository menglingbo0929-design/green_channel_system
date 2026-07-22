package com.example.backend.service.adapter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.model.dto.statistics.StatisticsReportQueryDTO;
import com.example.backend.model.vo.statistics.StatisticsReportRowVO;
import com.example.backend.service.port.StatisticsReportQueryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统计明细的真实数据库查询实现。
 *
 * <p>主查询以成员二维护的 application 为主表，一次关联成员一的学生组织信息、
 * 成员二的批次与申请明细以及成员四的欠费确认结果。多条欠费项目和礼包物品先在
 * 子查询中聚合，避免主表行被重复后导致分页总数和金额错误。</p>
 */
@Component
public class StatisticsReportQueryPortAdapter implements StatisticsReportQueryPort {

    /** 页面可用的排序 key 到固定 SQL 列的映射，不直接拼接前端传入的字符串。 */
    private static final Map<String, String> SORT_COLUMNS = new LinkedHashMap<>();

    static {
        SORT_COLUMNS.put("applicationId", "a.id");
        SORT_COLUMNS.put("applicationNo", "a.application_no");
        SORT_COLUMNS.put("studentNo", "s.student_no");
        SORT_COLUMNS.put("studentName", "s.student_name");
        SORT_COLUMNS.put("collegeName", "c.college_name");
        SORT_COLUMNS.put("majorName", "m.major_name");
        SORT_COLUMNS.put("gradeName", "g.grade_name");
        SORT_COLUMNS.put("className", "ci.class_name");
        SORT_COLUMNS.put("applicationType", "a.application_type");
        SORT_COLUMNS.put("applicationStatus", "a.status");
        SORT_COLUMNS.put("declaredAmount", "arrears.declared_amount");
        SORT_COLUMNS.put("confirmedAmount", "confirmation.confirmed_amount");
        SORT_COLUMNS.put("subsidyAmount", "subsidy.subsidy_amount");
        SORT_COLUMNS.put("applicationTime", "a.create_time");
        SORT_COLUMNS.put("completionTime", "a.update_time");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 按页面筛选条件返回最终状态申请的真实分页明细。 */
    @Override
    public Page<StatisticsReportRowVO> queryReportPage(
            StatisticsReportQueryDTO query,
            Long currentUserId
    ) {
        NamedParameterJdbcTemplate namedJdbc =
                new NamedParameterJdbcTemplate(jdbcTemplate);
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        String whereSql = buildWhereSql(query, parameters);

        Long total = namedJdbc.queryForObject(
                "SELECT COUNT(*) FROM application a "
                        + "JOIN student s ON s.id=a.student_id AND s.deleted=0 "
                        + whereSql,
                parameters,
                Long.class
        );

        int pageNo = query.getPageNo() == null ? 1 : query.getPageNo();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        parameters.addValue("offset", (pageNo - 1) * pageSize);
        parameters.addValue("pageSize", pageSize);

        String sortColumn = SORT_COLUMNS.getOrDefault(
                query.getSortBy(), "a.create_time");
        String sortDirection = "ASC".equalsIgnoreCase(query.getSortDirection())
                ? "ASC" : "DESC";

        String dataSql = "SELECT a.id application_id,a.application_no," 
                + "s.student_no,s.student_name,c.college_name,m.major_name," 
                + "g.grade_name,ci.class_name,a.application_type,a.batch_type," 
                + "a.batch_id,COALESCE(gcb.batch_name,sb.batch_name) batch_name," 
                + "a.status application_status,a.source application_source," 
                + "arrears.item_names arrears_item_names," 
                + "arrears.reason_names arrears_reason_name," 
                + "arrears.declared_amount,confirmation.confirmed_amount," 
                + "gifts.item_names gift_item_names,subsidy.subsidy_amount," 
                + "a.create_time application_time,a.update_time completion_time " 
                + "FROM application a " 
                + "JOIN student s ON s.id=a.student_id AND s.deleted=0 " 
                + "LEFT JOIN college c ON c.id=s.college_id AND c.deleted=0 " 
                + "LEFT JOIN major m ON m.id=s.major_id AND m.deleted=0 " 
                + "LEFT JOIN grade g ON g.id=s.grade_id AND g.deleted=0 " 
                + "LEFT JOIN class_info ci ON ci.id=s.class_id AND ci.deleted=0 " 
                + "LEFT JOIN green_channel_batch gcb " 
                + "ON gcb.id=a.green_channel_batch_id AND gcb.deleted=0 " 
                + "LEFT JOIN subsidy_batch sb " 
                + "ON sb.id=a.subsidy_batch_id AND sb.deleted=0 " 
                + "LEFT JOIN (SELECT aa.application_id," 
                + "GROUP_CONCAT(fi.item_name ORDER BY fi.id SEPARATOR '、') item_names," 
                + "GROUP_CONCAT(DISTINCT CASE aa.arrears_reason_code "
                + "WHEN 'FAMILY_FINANCIAL_DIFFICULTY' THEN '家庭经济困难' "
                + "WHEN 'FAMILY_EMERGENCY' THEN '家庭突发变故' "
                + "WHEN 'MAJOR_ILLNESS' THEN '重大疾病' "
                + "WHEN 'DISASTER_ACCIDENT' THEN '灾害或意外事故' "
                + "WHEN 'OTHER' THEN '其他' END ORDER BY aa.id SEPARATOR '、') reason_names," 
                + "SUM(aa.declared_amount) declared_amount " 
                + "FROM arrears_application aa " 
                + "JOIN fee_item fi ON fi.id=aa.fee_item_id AND fi.deleted=0 " 
                + "WHERE aa.deleted=0 GROUP BY aa.application_id) arrears " 
                + "ON arrears.application_id=a.id " 
                + "LEFT JOIN (SELECT application_id,MAX(confirmed_amount) confirmed_amount " 
                + "FROM arrears_confirmation WHERE deleted=0 GROUP BY application_id) confirmation " 
                + "ON confirmation.application_id=a.id " 
                + "LEFT JOIN (SELECT ga.application_id," 
                + "GROUP_CONCAT(gi.item_name ORDER BY gi.id SEPARATOR '、') item_names " 
                + "FROM gift_application ga " 
                + "JOIN gift_application_item gai " 
                + "ON gai.gift_application_id=ga.id AND gai.deleted=0 " 
                + "JOIN batch_gift_item bgi " 
                + "ON bgi.id=gai.batch_gift_item_id AND bgi.deleted=0 " 
                + "JOIN gift_item gi ON gi.id=bgi.gift_item_id AND gi.deleted=0 " 
                + "WHERE ga.deleted=0 GROUP BY ga.application_id) gifts " 
                + "ON gifts.application_id=a.id " 
                + "LEFT JOIN (SELECT application_id," 
                + "MAX(COALESCE(final_amount,expected_amount)) subsidy_amount " 
                + "FROM subsidy_application WHERE deleted=0 GROUP BY application_id) subsidy " 
                + "ON subsidy.application_id=a.id " 
                + whereSql + " ORDER BY " + sortColumn + " " + sortDirection
                + " LIMIT :pageSize OFFSET :offset";

        Page<StatisticsReportRowVO> result = new Page<>(pageNo, pageSize, total == null ? 0 : total);
        result.setRecords(namedJdbc.query(dataSql, parameters, (rs, rowNum) -> {
            StatisticsReportRowVO row = new StatisticsReportRowVO();
            row.setApplicationId(rs.getLong("application_id"));
            row.setApplicationNo(rs.getString("application_no"));
            row.setStudentNo(rs.getString("student_no"));
            row.setStudentName(rs.getString("student_name"));
            row.setCollegeName(rs.getString("college_name"));
            row.setMajorName(rs.getString("major_name"));
            row.setGradeName(rs.getString("grade_name"));
            row.setClassName(rs.getString("class_name"));
            row.setApplicationType(rs.getString("application_type"));
            row.setBatchType(rs.getString("batch_type"));
            row.setBatchId(rs.getLong("batch_id"));
            row.setBatchName(rs.getString("batch_name"));
            row.setApplicationStatus(rs.getString("application_status"));
            row.setApplicationSource(rs.getString("application_source"));
            row.setArrearsItemNames(rs.getString("arrears_item_names"));
            row.setArrearsReasonName(rs.getString("arrears_reason_name"));
            row.setDeclaredAmount(rs.getBigDecimal("declared_amount"));
            row.setConfirmedAmount(rs.getBigDecimal("confirmed_amount"));
            row.setGiftItemNames(rs.getString("gift_item_names"));
            row.setSubsidyAmount(rs.getBigDecimal("subsidy_amount"));
            row.setApplicationTime(rs.getTimestamp("application_time").toLocalDateTime());
            row.setCompletionTime(rs.getTimestamp("completion_time").toLocalDateTime());
            return row;
        }));
        return result;
    }

    /** 组装汇总和明细共用的最终状态及页面筛选条件。 */
    private String buildWhereSql(
            StatisticsReportQueryDTO query,
            MapSqlParameterSource parameters
    ) {
        StringBuilder sql = new StringBuilder(" WHERE a.deleted=0 ");
        if (query.getApplicationStatus() == null || query.getApplicationStatus().isBlank()) {
            // 与统计大盘使用同一最终审核口径：欠费申请在金额确认前为
            // CONFIRM_PENDING，确认完成后才会进入 COMPLETED。
            sql.append("AND a.status IN ('APPROVED','CONFIRM_PENDING','COMPLETED') ");
        } else {
            sql.append("AND a.status=:applicationStatus ");
            parameters.addValue("applicationStatus", query.getApplicationStatus());
        }
        if (query.getBatchType() != null && !query.getBatchType().isBlank()) {
            sql.append("AND a.batch_type=:batchType ");
            parameters.addValue("batchType", query.getBatchType());
        }
        if (query.getBatchId() != null) {
            sql.append("AND a.batch_id=:batchId ");
            parameters.addValue("batchId", query.getBatchId());
        }
        if (query.getCollegeId() != null) {
            sql.append("AND s.college_id=:collegeId ");
            parameters.addValue("collegeId", query.getCollegeId());
        }
        if (query.getMajorId() != null) {
            sql.append("AND s.major_id=:majorId ");
            parameters.addValue("majorId", query.getMajorId());
        }
        if (query.getGradeId() != null) {
            sql.append("AND s.grade_id=:gradeId ");
            parameters.addValue("gradeId", query.getGradeId());
        }
        if (query.getClassId() != null) {
            sql.append("AND s.class_id=:classId ");
            parameters.addValue("classId", query.getClassId());
        }
        if (query.getApplicationType() != null && !query.getApplicationType().isBlank()) {
            sql.append("AND a.application_type=:applicationType ");
            parameters.addValue("applicationType", query.getApplicationType());
        }
        if (query.getFeeItemId() != null) {
            sql.append("AND EXISTS (SELECT 1 FROM arrears_application filter_arrears "
                    + "WHERE filter_arrears.application_id=a.id "
                    + "AND filter_arrears.fee_item_id=:feeItemId "
                    + "AND filter_arrears.deleted=0) ");
            parameters.addValue("feeItemId", query.getFeeItemId());
        }
        if (query.getApplicationStartTime() != null) {
            sql.append("AND a.create_time>=:applicationStartTime ");
            parameters.addValue("applicationStartTime", query.getApplicationStartTime());
        }
        if (query.getApplicationEndTime() != null) {
            sql.append("AND a.create_time<=:applicationEndTime ");
            parameters.addValue("applicationEndTime", query.getApplicationEndTime());
        }
        return sql.toString();
    }
}
