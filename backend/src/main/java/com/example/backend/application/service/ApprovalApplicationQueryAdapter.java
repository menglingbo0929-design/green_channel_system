package com.example.backend.application.service;

import com.example.backend.application.dto.ArrearsItemSnapshot;
import com.example.backend.application.dto.GiftApplicationItemSnapshot;
import com.example.backend.application.dto.SubsidyApplicationSnapshot;
import com.example.backend.application.mapper.ApplicationResourceMapper;
import com.example.backend.application.mapper.ArrearsApplicationMapper;
import com.example.backend.approval.api.ApprovalCollegeCount;
import com.example.backend.approval.api.ApprovalFunnelCount;
import com.example.backend.approval.api.ApprovalLevelCount;
import com.example.backend.approval.api.ApprovalListQuery;
import com.example.backend.approval.api.ApprovalPage;
import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApplicationType;
import com.example.backend.approval.domain.ApprovalLevel;
import com.example.backend.approval.persistence.type.BatchType;
import com.example.backend.approval.port.ApprovalApplicationDetail;
import com.example.backend.approval.port.ApprovalApplicationQueryPort;
import com.example.backend.approval.port.ApprovalApplicationSnapshot;
import com.example.backend.approval.port.ApprovalDashboardData;
import com.example.backend.approval.port.ApprovalSubmissionApplicationQueryService;
import com.example.backend.approval.port.ApprovalWorkScope;
import com.example.backend.approval.port.ApplicationStateSnapshot;
import com.example.backend.approval.port.UserRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import com.example.backend.application.exception.ApplicationException;

/**
 * Application-module read adapter for the approval workbench and first batch submission.
 * All application-table access remains here; approval only consumes the stable projections.
 */
@Service
public class ApprovalApplicationQueryAdapter implements ApprovalApplicationQueryPort,
        ApprovalSubmissionApplicationQueryService {

    private static final String FROM = " FROM application a "
            + "JOIN student s ON s.id=a.student_id AND s.deleted=0 "
            + "LEFT JOIN college c ON c.id=s.college_id AND c.deleted=0 "
            + "LEFT JOIN grade g ON g.id=s.grade_id AND g.deleted=0 ";

    private final NamedParameterJdbcTemplate jdbc;
    private final ApplicationService applications;
    private final ArrearsApplicationMapper arrears;
    private final ApplicationResourceMapper resources;

    public ApprovalApplicationQueryAdapter(NamedParameterJdbcTemplate jdbc,
                                           ApplicationService applications,
                                           ArrearsApplicationMapper arrears,
                                           ApplicationResourceMapper resources) {
        this.jdbc = jdbc;
        this.applications = applications;
        this.arrears = arrears;
        this.resources = resources;
    }

    @Override
    public ApprovalPage<ApprovalApplicationSnapshot> pagePending(ApprovalWorkScope scope, ApprovalListQuery query) {
        return page(scope, query, null);
    }

    @Override
    public ApprovalPage<ApprovalApplicationSnapshot> pageByApplicationIds(
            ApprovalWorkScope scope, ApprovalListQuery query, List<Long> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) return ApprovalPage.empty(query.page(), query.size());
        return page(scope, query, applicationIds);
    }

    private ApprovalPage<ApprovalApplicationSnapshot> page(ApprovalWorkScope scope, ApprovalListQuery query,
                                                             List<Long> applicationIds) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        String where = where(scope, query, parameters, applicationIds);
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + FROM + where, parameters, Long.class);
        parameters.addValue("limit", query.size());
        parameters.addValue("offset", (long) (query.page() - 1) * query.size());
        List<ApprovalApplicationSnapshot> records = jdbc.query(
                "SELECT a.id,a.application_no,a.application_type,a.batch_type,"
                        + "COALESCE(a.green_channel_batch_id,a.subsidy_batch_id) batch_id,"
                        + "a.student_id,s.student_no,s.student_name,s.college_id,c.college_name,g.grade_name,"
                        + "CASE WHEN a.application_type='GREEN_CHANNEL' THEN COALESCE((SELECT SUM(aa.declared_amount) FROM arrears_application aa WHERE aa.application_id=a.id AND aa.deleted=0),0) ELSE COALESCE((SELECT sa.expected_amount FROM subsidy_application sa WHERE sa.application_id=a.id AND sa.deleted=0 LIMIT 1),0) END declaredAmount,"
                        + "a.status,a.current_level,a.review_round,a.submit_time,a.version"
                        + FROM + where + " ORDER BY a.submit_time DESC,a.id DESC LIMIT :limit OFFSET :offset",
                parameters, snapshotMapper());
        return new ApprovalPage<>(records, total == null ? 0 : total, query.page(), query.size());
    }

    @Override
    public ApprovalApplicationDetail getRequiredApprovalDetail(Long applicationId) {
        var summary = applications.findOne(applicationId);
        MapSqlParameterSource parameters = new MapSqlParameterSource("applicationId", applicationId);
        Map<String, Object> application = jdbc.query(
                "SELECT a.id applicationId,a.application_no applicationNo,a.application_type applicationType,"
                        + "a.batch_type batchType,COALESCE(a.green_channel_batch_id,a.subsidy_batch_id) batchId,"
                        + "a.status,a.current_level currentLevel,a.review_round reviewRound,a.application_reason applicationReason,"
                        + "a.source,a.submit_time submitTime,a.create_time createTime,s.id studentId,s.student_no studentNo,"
                        + "s.student_name studentName,s.college_id collegeId,c.college_name collegeName,"
                        + "s.grade_id gradeId,g.grade_name gradeName " + FROM
                        + "WHERE a.id=:applicationId AND a.deleted=0",
                parameters, rs -> {
                    if (!rs.next()) return null;
                    Map<String, Object> value = new LinkedHashMap<>();
                    for (String column : List.of("applicationId", "applicationNo", "applicationType", "batchType", "batchId",
                            "status", "currentLevel", "reviewRound", "applicationReason", "source", "submitTime", "createTime",
                            "studentId", "studentNo", "studentName", "collegeId", "collegeName", "gradeId", "gradeName")) {
                        value.put(column, rs.getObject(column));
                    }
                    return value;
                });
        if (application == null) throw new ApplicationException("APPLICATION_NOT_FOUND", HttpStatus.NOT_FOUND, "申请不存在");
        List<ArrearsItemSnapshot> arrearsDetail = arrears.findItemsByApplicationId(applicationId);
        List<GiftApplicationItemSnapshot> giftDetail = resources.findGiftItems(applicationId);
        SubsidyApplicationSnapshot subsidyDetail = resources.findSubsidy(applicationId);
        List<Map<String, Object>> attachments = jdbc.query(
                "SELECT id,file_id fileId,original_filename originalFilename,content_type contentType,file_size fileSize,create_time createTime "
                        + "FROM application_attachment WHERE application_id=:applicationId AND deleted=0 ORDER BY id",
                parameters, (rs, rowNum) -> attachment(rs));
        return new ApprovalApplicationDetail(application, arrearsDetail, giftDetail, subsidyDetail, attachments, summary.version());
    }

    @Override
    public ApprovalDashboardData getDashboard(ApprovalWorkScope scope, ApprovalListQuery query) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        String where = where(scope, query, parameters, null);
        List<ApprovalLevelCount> levels = jdbc.query(
                "SELECT a.current_level level,COUNT(*) count" + FROM + where
                        + " GROUP BY a.current_level ORDER BY a.current_level",
                parameters, (rs, rowNum) -> new ApprovalLevelCount(rs.getString("level"), rs.getLong("count")));
        List<ApprovalCollegeCount> colleges = jdbc.query(
                "SELECT s.college_id collegeId,COALESCE(c.college_name,'未分配学院') collegeName,COUNT(*) count" + FROM + where
                        + " GROUP BY s.college_id,c.college_name ORDER BY count DESC,s.college_id",
                parameters, (rs, rowNum) -> new ApprovalCollegeCount(rs.getLong("collegeId"), rs.getString("collegeName"), rs.getLong("count")));
        List<ApprovalFunnelCount> funnel = jdbc.query(
                "SELECT a.status status,COUNT(*) count" + FROM + where
                        + " GROUP BY a.status ORDER BY a.status",
                parameters, (rs, rowNum) -> new ApprovalFunnelCount(rs.getString("status"), rs.getLong("count")));
        return new ApprovalDashboardData(levels, colleges, funnel);
    }

    @Override
    public List<Long> listScopedApplicationIds(ApprovalWorkScope scope, ApprovalListQuery query) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        return jdbc.query("SELECT a.id" + FROM + where(scope, query, parameters, null)
                + " ORDER BY a.id", parameters, (rs, rowNum) -> rs.getLong(1));
    }

    @Override
    public List<ApplicationStateSnapshot> listByBatch(BatchType batchType, Long batchId) {
        if (batchType == null || batchId == null || batchId <= 0) return List.of();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("batchType", batchType.name()).addValue("batchId", batchId);
        return jdbc.query("SELECT id,student_id,batch_type,COALESCE(green_channel_batch_id,subsidy_batch_id) batch_id,"
                        + "application_type,status,current_level,review_round,version FROM application "
                        + "WHERE deleted=0 AND batch_type=:batchType AND "
                        + "COALESCE(green_channel_batch_id,subsidy_batch_id)=:batchId ORDER BY id",
                parameters, (rs, rowNum) -> state(rs));
    }

    private String where(ApprovalWorkScope scope, ApprovalListQuery query, MapSqlParameterSource parameters,
                         List<Long> applicationIds) {
        StringBuilder sql = new StringBuilder(" WHERE a.deleted=0 ");
        if (scope.role() == UserRole.COUNSELOR) {
            sql.append("AND EXISTS (SELECT 1 FROM counselor_student cs WHERE cs.student_id=a.student_id AND cs.counselor_user_id=:scopeUserId) ");
            parameters.addValue("scopeUserId", scope.userId());
        } else if (scope.role() == UserRole.COLLEGE) {
            sql.append("AND s.college_id=:scopeCollegeId ");
            parameters.addValue("scopeCollegeId", scope.collegeId());
        }
        if (query.batchType() != null) {
            sql.append("AND a.batch_type=:batchType AND COALESCE(a.green_channel_batch_id,a.subsidy_batch_id)=:batchId ");
            parameters.addValue("batchType", query.batchType().name()).addValue("batchId", query.batchId());
        }
        if (query.applicationType() != null) { sql.append("AND a.application_type=:applicationType "); parameters.addValue("applicationType", query.applicationType().name()); }
        if (query.status() != null) { sql.append("AND a.status=:status "); parameters.addValue("status", query.status().name()); }
        if (query.collegeId() != null) { sql.append("AND s.college_id=:queryCollegeId "); parameters.addValue("queryCollegeId", query.collegeId()); }
        if (query.applicationNo() != null && !query.applicationNo().isBlank()) { sql.append("AND a.application_no LIKE :applicationNo "); parameters.addValue("applicationNo", "%" + query.applicationNo().trim() + "%"); }
        if (query.studentNo() != null && !query.studentNo().isBlank()) { sql.append("AND s.student_no LIKE :studentNo "); parameters.addValue("studentNo", "%" + query.studentNo().trim() + "%"); }
        if (query.studentName() != null && !query.studentName().isBlank()) { sql.append("AND s.student_name LIKE :studentName "); parameters.addValue("studentName", "%" + query.studentName().trim() + "%"); }
        if (applicationIds != null) { sql.append("AND a.id IN (:applicationIds) "); parameters.addValue("applicationIds", applicationIds); }
        return sql.toString();
    }

    private RowMapper<ApprovalApplicationSnapshot> snapshotMapper() {
        return (rs, rowNum) -> new ApprovalApplicationSnapshot(rs.getLong("id"), rs.getString("application_no"),
                ApplicationType.valueOf(rs.getString("application_type")), BatchType.valueOf(rs.getString("batch_type")),
                rs.getLong("batch_id"), rs.getLong("student_id"), rs.getString("student_no"), rs.getString("student_name"),
                rs.getLong("college_id"), rs.getString("college_name"), rs.getString("grade_name"),
                rs.getBigDecimal("declaredAmount"),
                ApplicationStatus.valueOf(rs.getString("status")), ApprovalLevel.valueOf(rs.getString("current_level")),
                rs.getInt("review_round"), localDateTime(rs, "submit_time"), rs.getInt("version"));
    }

    private ApplicationStateSnapshot state(ResultSet rs) throws SQLException {
        return new ApplicationStateSnapshot(rs.getLong("id"), rs.getLong("student_id"),
                BatchType.valueOf(rs.getString("batch_type")), rs.getLong("batch_id"),
                ApplicationType.valueOf(rs.getString("application_type")), ApplicationStatus.valueOf(rs.getString("status")),
                ApprovalLevel.valueOf(rs.getString("current_level")), rs.getInt("review_round"), rs.getInt("version"));
    }

    private Map<String, Object> attachment(ResultSet rs) throws SQLException {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", rs.getLong("id")); value.put("fileId", rs.getString("fileId"));
        value.put("originalFilename", rs.getString("originalFilename")); value.put("contentType", rs.getString("contentType"));
        value.put("fileSize", rs.getLong("fileSize")); value.put("createTime", rs.getObject("createTime"));
        return value;
    }

    private LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
