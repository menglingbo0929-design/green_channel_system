package com.example.backend.service.adapter;

import com.example.backend.model.dto.statistics.StatisticsFilterDTO;
import com.example.backend.model.vo.statistics.ApplicationStatisticsVO;
import com.example.backend.model.vo.statistics.ArrearsReasonStatisticsVO;
import com.example.backend.model.vo.statistics.BatchHistoryStatisticsVO;
import com.example.backend.model.vo.statistics.CollegeApplicantCountVO;
import com.example.backend.model.vo.statistics.GiftItemApplicationCountVO;
import com.example.backend.model.vo.statistics.GradeApplicantCountVO;
import com.example.backend.service.port.ApplicationStatisticsQueryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 6.1.5、6.1.6 统计大盘的真实数据库聚合实现。
 *
 * <p>所有人数均由数据库按 student_id 去重，金额直接读取最终欠费确认记录；
 * 学院、年级、欠费项目、礼包物品和历史批次也都来自当前数据库，不使用页面样例值。</p>
 */
@Component
public class ApplicationStatisticsQueryPortAdapter implements ApplicationStatisticsQueryPort {

    /**
     * 只读取由真实欠费申请产生、且由学校管理员确认的有效记录。
     * 旧测试脚本曾用裸 application_id 写入确认表，ID 与后续正式申请复用后会把
     * 生活补助误算成欠费确认金额，因此所有统计入口统一使用这段关联约束。
     */
    private static final String VALID_CONFIRMATION_AGGREGATE =
            "SELECT ac.application_id,MAX(ac.confirmed_amount) confirmed_amount "
                    + "FROM arrears_confirmation ac "
                    + "JOIN sys_user su ON su.id=ac.confirm_user_id AND su.deleted=0 "
                    + "JOIN sys_user_role sur ON sur.user_id=su.id AND sur.role_id=4 "
                    + "WHERE ac.deleted=0 "
                    + "AND EXISTS (SELECT 1 FROM arrears_application valid_aa "
                    + "WHERE valid_aa.application_id=ac.application_id AND valid_aa.deleted=0) "
                    + "GROUP BY ac.application_id";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 按同一套筛选条件一次完成各项统计查询并组装页面大盘。 */
    @Override
    public ApplicationStatisticsVO queryFinalStatistics(StatisticsFilterDTO filter) {
        NamedParameterJdbcTemplate namedJdbc =
                new NamedParameterJdbcTemplate(jdbcTemplate);
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        String whereSql = buildWhereSql(filter, parameters);

        ApplicationStatisticsVO result = new ApplicationStatisticsVO();
        result.setFinalApplicantCount(valueOrZero(namedJdbc.queryForObject(
                "SELECT COUNT(DISTINCT a.student_id) FROM application a "
                        + "JOIN student s ON s.id=a.student_id AND s.deleted=0 "
                        + whereSql,
                parameters,
                Long.class
        )));
        result.setCompletedStudentCount(valueOrZero(namedJdbc.queryForObject(
                "SELECT COUNT(DISTINCT CASE WHEN a.status='COMPLETED' "
                        + "THEN a.student_id END) FROM application a "
                        + "JOIN student s ON s.id=a.student_id AND s.deleted=0 "
                        + whereSql,
                parameters,
                Long.class
        )));
        result.setFeeItemApplicantCount(valueOrZero(namedJdbc.queryForObject(
                "SELECT COUNT(DISTINCT CASE WHEN aa.id IS NOT NULL "
                        + "THEN a.student_id END) FROM application a "
                        + "JOIN student s ON s.id=a.student_id AND s.deleted=0 "
                        + "LEFT JOIN arrears_application aa "
                        + "ON aa.application_id=a.id AND aa.deleted=0 "
                        + whereSql,
                parameters,
                Long.class
        )));
        BigDecimal confirmedAmount = namedJdbc.queryForObject(
                "SELECT COALESCE(SUM(confirmation.confirmed_amount),0) "
                        + "FROM application a "
                        + "JOIN student s ON s.id=a.student_id AND s.deleted=0 "
                        + "LEFT JOIN (" + VALID_CONFIRMATION_AGGREGATE + ") confirmation "
                        + "ON confirmation.application_id=a.id "
                        + whereSql,
                parameters,
                BigDecimal.class
        );
        result.setConfirmedArrearsAmount(
                confirmedAmount == null ? BigDecimal.ZERO : confirmedAmount);

        result.setCollegeApplicantCounts(namedJdbc.query(
                "SELECT c.id college_id,c.college_name," 
                        + "COUNT(DISTINCT a.student_id) applicant_count "
                        + "FROM application a "
                        + "JOIN student s ON s.id=a.student_id AND s.deleted=0 "
                        + "JOIN college c ON c.id=s.college_id AND c.deleted=0 "
                        + whereSql + " GROUP BY c.id,c.college_name ORDER BY c.id",
                parameters,
                (rs, rowNum) -> {
                    CollegeApplicantCountVO row = new CollegeApplicantCountVO();
                    row.setCollegeId(rs.getLong("college_id"));
                    row.setCollegeName(rs.getString("college_name"));
                    row.setApplicantCount(rs.getLong("applicant_count"));
                    return row;
                }
        ));
        result.setGradeApplicantCounts(namedJdbc.query(
                "SELECT g.id grade_id,g.grade_name," 
                        + "COUNT(DISTINCT a.student_id) applicant_count "
                        + "FROM application a "
                        + "JOIN student s ON s.id=a.student_id AND s.deleted=0 "
                        + "JOIN grade g ON g.id=s.grade_id AND g.deleted=0 "
                        + whereSql + " GROUP BY g.id,g.grade_name ORDER BY g.id",
                parameters,
                (rs, rowNum) -> {
                    GradeApplicantCountVO row = new GradeApplicantCountVO();
                    row.setGradeId(rs.getLong("grade_id"));
                    row.setGradeName(rs.getString("grade_name"));
                    row.setApplicantCount(rs.getLong("applicant_count"));
                    return row;
                }
        ));
        result.setArrearsReasonStatistics(namedJdbc.query(
                "SELECT aa.arrears_reason_code reason_code," 
                        + "CASE aa.arrears_reason_code "
                        + "WHEN 'FAMILY_FINANCIAL_DIFFICULTY' THEN '家庭经济困难' "
                        + "WHEN 'FAMILY_EMERGENCY' THEN '家庭突发变故' "
                        + "WHEN 'MAJOR_ILLNESS' THEN '重大疾病' "
                        + "WHEN 'DISASTER_ACCIDENT' THEN '灾害或意外事故' "
                        + "WHEN 'OTHER' THEN '其他' END reason_name," 
                        + "COUNT(DISTINCT a.student_id) applicant_count," 
                        + "COALESCE(SUM(CASE WHEN arrears_total.total_declared_amount>0 "
                        + "THEN COALESCE(confirmation.confirmed_amount,0)*aa.declared_amount/arrears_total.total_declared_amount "
                        + "ELSE 0 END),0) confirmed_amount "
                        + "FROM application a "
                        + "JOIN student s ON s.id=a.student_id AND s.deleted=0 "
                        + "JOIN arrears_application aa "
                        + "ON aa.application_id=a.id AND aa.deleted=0 "
                        + "LEFT JOIN (SELECT application_id,SUM(declared_amount) total_declared_amount "
                        + "FROM arrears_application WHERE deleted=0 GROUP BY application_id) arrears_total "
                        + "ON arrears_total.application_id=a.id "
                        + "LEFT JOIN (" + VALID_CONFIRMATION_AGGREGATE + ") confirmation "
                        + "ON confirmation.application_id=a.id "
                        + whereSql
                        + " GROUP BY aa.arrears_reason_code ORDER BY aa.arrears_reason_code",
                parameters,
                (rs, rowNum) -> {
                    ArrearsReasonStatisticsVO row = new ArrearsReasonStatisticsVO();
                    row.setArrearsReasonCode(rs.getString("reason_code"));
                    row.setArrearsReasonName(rs.getString("reason_name"));
                    row.setApplicantCount(rs.getLong("applicant_count"));
                    row.setConfirmedAmount(rs.getBigDecimal("confirmed_amount"));
                    return row;
                }
        ));
        result.setGiftItemApplicationCounts(namedJdbc.query(
                "SELECT gi.id gift_item_id,gi.item_name gift_item_name," 
                        + "COUNT(DISTINCT a.id) application_count "
                        + "FROM application a "
                        + "JOIN student s ON s.id=a.student_id AND s.deleted=0 "
                        + "JOIN gift_application ga "
                        + "ON ga.application_id=a.id AND ga.deleted=0 "
                        + "JOIN gift_application_item gai "
                        + "ON gai.gift_application_id=ga.id AND gai.deleted=0 "
                        + "JOIN batch_gift_item bgi "
                        + "ON bgi.id=gai.batch_gift_item_id AND bgi.deleted=0 "
                        + "JOIN gift_item gi ON gi.id=bgi.gift_item_id AND gi.deleted=0 "
                        + whereSql
                        + " GROUP BY gi.id,gi.item_name ORDER BY gi.id",
                parameters,
                (rs, rowNum) -> {
                    GiftItemApplicationCountVO row = new GiftItemApplicationCountVO();
                    row.setGiftItemId(rs.getLong("gift_item_id"));
                    row.setGiftItemName(rs.getString("gift_item_name"));
                    row.setApplicationCount(rs.getLong("application_count"));
                    return row;
                }
        ));
        result.setBatchHistoryStatistics(namedJdbc.query(
                "SELECT a.batch_type,a.batch_id," 
                        + "COALESCE(gcb.batch_name,sb.batch_name) batch_name," 
                        + "COUNT(DISTINCT a.student_id) applicant_count," 
                        + "COUNT(DISTINCT CASE WHEN a.status='COMPLETED' "
                        + "THEN a.student_id END) completed_count," 
                        + "COALESCE(SUM(confirmation.confirmed_amount),0) confirmed_amount "
                        + "FROM application a "
                        + "JOIN student s ON s.id=a.student_id AND s.deleted=0 "
                        + "LEFT JOIN green_channel_batch gcb "
                        + "ON gcb.id=a.green_channel_batch_id AND gcb.deleted=0 "
                        + "LEFT JOIN subsidy_batch sb "
                        + "ON sb.id=a.subsidy_batch_id AND sb.deleted=0 "
                        + "LEFT JOIN (" + VALID_CONFIRMATION_AGGREGATE + ") confirmation "
                        + "ON confirmation.application_id=a.id "
                        + whereSql
                        + " GROUP BY a.batch_type,a.batch_id,gcb.batch_name,sb.batch_name "
                        + "ORDER BY a.batch_type,a.batch_id",
                parameters,
                (rs, rowNum) -> {
                    BatchHistoryStatisticsVO row = new BatchHistoryStatisticsVO();
                    row.setBatchType(rs.getString("batch_type"));
                    row.setBatchId(rs.getLong("batch_id"));
                    row.setBatchName(rs.getString("batch_name"));
                    row.setApplicantCount(rs.getLong("applicant_count"));
                    row.setCompletedStudentCount(rs.getLong("completed_count"));
                    row.setConfirmedArrearsAmount(rs.getBigDecimal("confirmed_amount"));
                    return row;
                }
        ));
        return result;
    }

    /** 将页面八项筛选统一转换成每个聚合查询共用的 SQL 条件。 */
    private String buildWhereSql(
            StatisticsFilterDTO filter,
            MapSqlParameterSource parameters
    ) {
        StringBuilder sql = new StringBuilder(" WHERE a.deleted=0 ");
        if (filter.getApplicationStatus() == null
                || filter.getApplicationStatus().isBlank()) {
            // 学校审核通过的欠费申请会先进入 CONFIRM_PENDING，待学校完成
            // 欠费金额确认后才变为 COMPLETED；两种状态都属于统计口径。
            sql.append("AND a.status IN ('APPROVED','CONFIRM_PENDING','COMPLETED') ");
        } else {
            sql.append("AND a.status=:applicationStatus ");
            parameters.addValue("applicationStatus", filter.getApplicationStatus());
        }
        if (filter.getBatchType() != null && !filter.getBatchType().isBlank()) {
            sql.append("AND a.batch_type=:batchType ");
            parameters.addValue("batchType", filter.getBatchType());
        }
        if (filter.getBatchId() != null) {
            sql.append("AND a.batch_id=:batchId ");
            parameters.addValue("batchId", filter.getBatchId());
        }
        if (filter.getCollegeId() != null) {
            sql.append("AND s.college_id=:collegeId ");
            parameters.addValue("collegeId", filter.getCollegeId());
        }
        if (filter.getMajorId() != null) {
            sql.append("AND s.major_id=:majorId ");
            parameters.addValue("majorId", filter.getMajorId());
        }
        if (filter.getGradeId() != null) {
            sql.append("AND s.grade_id=:gradeId ");
            parameters.addValue("gradeId", filter.getGradeId());
        }
        if (filter.getClassId() != null) {
            sql.append("AND s.class_id=:classId ");
            parameters.addValue("classId", filter.getClassId());
        }
        if (filter.getApplicationType() != null
                && !filter.getApplicationType().isBlank()) {
            sql.append("AND a.application_type=:applicationType ");
            parameters.addValue("applicationType", filter.getApplicationType());
        }
        if (filter.getFeeItemId() != null) {
            sql.append("AND EXISTS (SELECT 1 FROM arrears_application filter_arrears "
                    + "WHERE filter_arrears.application_id=a.id "
                    + "AND filter_arrears.fee_item_id=:feeItemId "
                    + "AND filter_arrears.deleted=0) ");
            parameters.addValue("feeItemId", filter.getFeeItemId());
        }
        if (filter.getApplicationStartTime() != null) {
            sql.append("AND a.create_time>=:applicationStartTime ");
            parameters.addValue("applicationStartTime", filter.getApplicationStartTime());
        }
        if (filter.getApplicationEndTime() != null) {
            sql.append("AND a.create_time<=:applicationEndTime ");
            parameters.addValue("applicationEndTime", filter.getApplicationEndTime());
        }
        return sql.toString();
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
