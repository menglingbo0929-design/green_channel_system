package com.example.backend.service.adapter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.application.domain.Application;
import com.example.backend.application.domain.ApplicationSource;
import com.example.backend.application.domain.ApplicationStatus;
import com.example.backend.application.domain.ApplicationType;
import com.example.backend.application.domain.ApprovalLevel;
import com.example.backend.application.domain.BatchType;
import com.example.backend.application.dto.ArrearsItemCommand;
import com.example.backend.application.mapper.ApplicationMapper;
import com.example.backend.application.mapper.ApplicationOperationMapper;
import com.example.backend.application.service.ApplicationService;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.supplement.SupplementCreateDTO;
import com.example.backend.model.dto.supplement.SupplementQueryDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.model.vo.supplement.SupplementApplicationVO;
import com.example.backend.service.port.SupplementApplicationPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 6.1.4 线下补录申请适配器。
 *
 * <p>本类使用成员二现有的 application、欠费、礼包和补助数据结构保存真实记录，
 * source 固定写成 SUPPLEMENT；成员四 Service 随后调用成员三状态机完成自动审核。</p>
 */
/**
 * 历史本地实现保留用于代码迁移参考，不注册为 Spring Bean。
 * 线下补录必须注入成员二的 application.service.SupplementApplicationService，
 * 申请、明细、资源和补录历史均由成员二模块维护。
 */
@Deprecated(forRemoval = false)
public class SupplementApplicationPortAdapter implements SupplementApplicationPort {

    /** 成员二申请主表 Mapper。 */
    @Autowired
    private ApplicationMapper applicationMapper;

    /** 成员二幂等操作记录 Mapper。 */
    @Autowired
    private ApplicationOperationMapper operationMapper;

    /** 复用成员二已经实现的欠费明细写入和状态快照查询。 */
    @Autowired
    private ApplicationService applicationService;

    /** 用于写入礼包、补助明细以及读取补录历史。 */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 创建一条真实的 SUPPLEMENT 草稿，并写入与申请类型对应的明细。
     */
    @Override
    public SupplementApplicationVO createSupplementDraft(
            SupplementCreateDTO command,
            SchoolProxyStudentVO student,
            String batchType,
            Long operatorUserId
    ) {
        Long existingId = operationMapper.findApplicationIdByRequestId(command.getRequestId());
        if (existingId != null) {
            return findSupplementById(existingId, operatorUserId);
        }

        Application application = new Application();
        application.setApplicationNo(nextApplicationNo(command.getApplicationType()));
        application.setStudentId(student.getStudentId());
        application.setApplicationType(ApplicationType.valueOf(command.getApplicationType()));
        application.setSource(ApplicationSource.SUPPLEMENT);
        application.setBatchType(BatchType.valueOf(batchType));
        if ("GREEN_CHANNEL".equals(batchType)) {
            application.setGreenChannelBatchId(command.getBatchId());
        } else {
            application.setSubsidyBatchId(command.getBatchId());
        }
        application.setStatus(ApplicationStatus.DRAFT);
        application.setCurrentLevel(ApprovalLevel.STUDENT);
        application.setReviewRound(0);
        application.setVersion(0);
        application.setApplicationReason(command.getApplicationReason());
        application.setCreateBy(operatorUserId);
        application.setUpdateBy(operatorUserId);
        applicationMapper.insert(application);

        operationMapper.insert(
                application.getId(),
                "CREATE_SUPPLEMENT",
                command.getRequestId(),
                operatorUserId
        );
        jdbcTemplate.update(
                "UPDATE application_operation_record "
                        + "SET result_snapshot=JSON_OBJECT('handledAt', ?, 'supplementReason', ?) "
                        + "WHERE request_id=?",
                command.getHandledAt().toString(),
                command.getSupplementReason(),
                command.getRequestId()
        );

        if (command.getArrearsItems() != null && !command.getArrearsItems().isEmpty()) {
            List<ArrearsItemCommand> items = command.getArrearsItems().stream()
                    .map(item -> new ArrearsItemCommand(
                            item.getFeeItemId(),
                            item.getDeclaredAmount(),
                            item.getArrearsReasonCode()))
                    .toList();
            applicationService.replaceArrearsItems(
                    application.getId(), 0, items, operatorUserId);
        }

        if (command.getGiftItems() != null && !command.getGiftItems().isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO gift_application(application_id) VALUES (?)",
                    application.getId()
            );
            Long giftApplicationId = jdbcTemplate.queryForObject(
                    "SELECT id FROM gift_application "
                            + "WHERE application_id=? AND deleted=0 LIMIT 1",
                    Long.class,
                    application.getId()
            );
            command.getGiftItems().forEach(item -> jdbcTemplate.update(
                    "INSERT INTO gift_application_item "
                            + "(gift_application_id, batch_gift_item_id, quantity) VALUES (?, ?, ?)",
                    giftApplicationId,
                    item.getGiftItemId(),
                    item.getQuantity()
            ));
        }

        if (!"GREEN_CHANNEL".equals(command.getApplicationType())) {
            jdbcTemplate.update(
                    "INSERT INTO subsidy_application(application_id, expected_amount) VALUES (?, ?)",
                    application.getId(),
                    command.getSubsidyAmount()
            );
        }

        return findSupplementById(application.getId(), operatorUserId);
    }

    /** 查询真实补录历史，并应用页面 8 提供的筛选条件。 */
    @Override
    public Page<SupplementApplicationVO> findSupplementPage(
            SupplementQueryDTO query,
            PageDTO page,
            Long operatorUserId
    ) {
        int pageNo = page.getPageNo() == null ? 1 : page.getPageNo();
        int pageSize = page.getPageSize() == null ? 10 : page.getPageSize();

        StringBuilder where = new StringBuilder(
                " WHERE a.deleted=0 AND a.source='SUPPLEMENT'");
        List<Object> parameters = new ArrayList<>();
        if (query != null && query.getStudentNo() != null && !query.getStudentNo().isBlank()) {
            where.append(" AND s.student_no=?");
            parameters.add(query.getStudentNo());
        }
        if (query != null && query.getApplicationType() != null
                && !query.getApplicationType().isBlank()) {
            where.append(" AND a.application_type=?");
            parameters.add(query.getApplicationType());
        }
        if (query != null && query.getBatchId() != null) {
            where.append(" AND a.batch_id=?");
            parameters.add(query.getBatchId());
        }
        if (query != null && query.getStatus() != null && !query.getStatus().isBlank()) {
            where.append(" AND a.status=?");
            parameters.add(query.getStatus());
        }

        String from = " FROM application a JOIN student s ON s.id=a.student_id "
                + "LEFT JOIN application_operation_record o "
                + "ON o.application_id=a.id AND o.operation_type='CREATE_SUPPLEMENT'";
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*)" + from + where,
                Long.class,
                parameters.toArray()
        );

        List<Object> pageParameters = new ArrayList<>(parameters);
        pageParameters.add(pageSize);
        pageParameters.add((pageNo - 1) * pageSize);
        List<SupplementApplicationVO> records = jdbcTemplate.query(
                selectSql() + from + where + " ORDER BY a.create_time DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> mapRow(rs),
                pageParameters.toArray()
        );

        Page<SupplementApplicationVO> result = new Page<>(pageNo, pageSize, total == null ? 0 : total);
        result.setRecords(records);
        return result;
    }

    /** 按申请 ID 读取一条真实的 SUPPLEMENT 记录。 */
    @Override
    public SupplementApplicationVO findSupplementById(
            Long applicationId,
            Long operatorUserId
    ) {
        String from = " FROM application a JOIN student s ON s.id=a.student_id "
                + "LEFT JOIN application_operation_record o "
                + "ON o.application_id=a.id AND o.operation_type='CREATE_SUPPLEMENT'";
        return jdbcTemplate.queryForObject(
                selectSql() + from
                        + " WHERE a.id=? AND a.deleted=0 AND a.source='SUPPLEMENT'",
                (rs, rowNum) -> mapRow(rs),
                applicationId
        );
    }

    /** 补录列表和详情共用同一套字段，避免两处返回格式不一致。 */
    private String selectSql() {
        return "SELECT a.id application_id,a.application_no,a.student_id,"
                + "s.student_no,s.student_name,a.application_type,a.batch_type,a.batch_id,"
                + "a.source,a.status,a.current_level,a.version,a.create_by,"
                + "EXISTS(SELECT 1 FROM arrears_application ar "
                + "WHERE ar.application_id=a.id AND ar.deleted=0) contains_arrears,"
                + "JSON_UNQUOTE(JSON_EXTRACT(o.result_snapshot,'$.handledAt')) handled_at,"
                + "JSON_UNQUOTE(JSON_EXTRACT(o.result_snapshot,'$.supplementReason')) supplement_reason";
    }

    /** 将数据库行转换为页面 8 使用的补录 VO。 */
    private SupplementApplicationVO mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        SupplementApplicationVO result = new SupplementApplicationVO();
        result.setApplicationId(rs.getLong("application_id"));
        result.setApplicationNo(rs.getString("application_no"));
        result.setStudentId(rs.getLong("student_id"));
        result.setStudentNo(rs.getString("student_no"));
        result.setStudentName(rs.getString("student_name"));
        result.setApplicationType(rs.getString("application_type"));
        result.setBatchType(rs.getString("batch_type"));
        result.setBatchId(rs.getLong("batch_id"));
        result.setSource(rs.getString("source"));
        result.setStatus(rs.getString("status"));
        result.setCurrentLevel(rs.getString("current_level"));
        result.setVersion(rs.getInt("version"));
        result.setContainsArrears(rs.getBoolean("contains_arrears"));
        result.setSupplementUserId(rs.getLong("create_by"));
        String handledAt = rs.getString("handled_at");
        if (handledAt != null) {
            result.setSupplementedAt(java.time.LocalDateTime.parse(handledAt));
        }
        result.setSupplementReason(rs.getString("supplement_reason"));
        return result;
    }

    /** 按申请类型生成便于演示和辨识的申请编号。 */
    private String nextApplicationNo(String applicationType) {
        String prefix = switch (applicationType) {
            case "LIVING_SUBSIDY" -> "LS";
            case "TRAVEL_SUBSIDY" -> "TS";
            default -> "GC";
        };
        long suffix = Math.abs(System.nanoTime() % 1_000_000);
        return prefix + LocalDate.now().toString().replace("-", "")
                + String.format("%06d", suffix);
    }
}
