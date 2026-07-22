package com.example.backend.service.adapter;

import com.example.backend.application.domain.ApplicationType;
import com.example.backend.application.domain.BatchType;
import com.example.backend.application.dto.ApplicationDraftCommand;
import com.example.backend.application.dto.ApplicationStateSnapshot;
import com.example.backend.application.dto.ApplicationSummary;
import com.example.backend.application.dto.ArrearsItemCommand;
import com.example.backend.application.service.ApplicationService;
import com.example.backend.approval.domain.ApprovalStateMachine;
import com.example.backend.approval.persistence.mapper.ApprovalRecordMapper;
import com.example.backend.approval.port.ApplicationStateQueryService;
import com.example.backend.approval.port.ApplicationStateWriteService;
import com.example.backend.approval.service.ApprovalWorkflowService;
import com.example.backend.model.dto.schoolproxy.SchoolProxyDraftDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyApplicationVO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.service.port.SchoolProxyApplicationPort;
import com.example.backend.service.port.SchoolProxyStudentQueryPort;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * 6.1.3 学校代申请写入适配器。
 *
 * <p>成员四只负责组织学校端操作；真正的申请主表、欠费明细和状态流转仍分别
 * 复用成员二的 {@link ApplicationService} 与成员三的
 * {@link ApprovalWorkflowService}，不在本适配器中重新实现业务规则。</p>
 */
/**
 * 历史本地实现保留用于代码迁移参考，不注册为 Spring Bean。
 * 学校代申请必须注入成员二的 application.service.SchoolProxyApplicationService，
 * 由其维护申请、明细、附件与资源表，成员四不得直接写这些表。
 */
@Deprecated(forRemoval = false)
public class SchoolProxyApplicationPortAdapter implements SchoolProxyApplicationPort {

    /** 成员二已经实现的申请创建、明细维护和申请查询服务。 */
    @Autowired
    private ApplicationService applicationService;

    /** 成员三状态机所需的申请状态查询接口。 */
    @Autowired
    private ApplicationStateQueryService approvalStateQueryService;

    /** 成员三状态机所需的申请状态写入接口。 */
    @Autowired
    private ApplicationStateWriteService approvalStateWriteService;

    /** 成员三已经实现的审核状态机。 */
    @Autowired
    private ApprovalStateMachine approvalStateMachine;

    /** 成员三审核记录 Mapper，用于保留首次提交记录。 */
    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    /** 复用同一套有效学生查询结果，避免再次拼装学生信息。 */
    @Autowired
    private SchoolProxyStudentQueryPort studentQueryPort;

    /** 附件元数据写入成员二维护的 application_attachment 表。 */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 创建来源为 SCHOOL_PROXY 的绿色通道草稿，并同步保存页面填写的欠费明细。
     */
    @Override
    public SchoolProxyApplicationVO createDraft(
            SchoolProxyDraftDTO command,
            Long operatorUserId
    ) {
        SchoolProxyStudentVO student =
                studentQueryPort.findEnabledStudentByStudentNo(command.getStudentNo());

        ApplicationDraftCommand draftCommand = new ApplicationDraftCommand(
                ApplicationType.GREEN_CHANNEL,
                BatchType.valueOf(command.getBatchType()),
                command.getBatchId(),
                command.getRequestId(),
                command.getApplicationReason()
        );

        ApplicationStateSnapshot state = applicationService.createSchoolProxyApplication(
                student.getStudentId(), operatorUserId, draftCommand);

        if (command.getArrearsItems() != null && !command.getArrearsItems().isEmpty()) {
            List<ArrearsItemCommand> arrearsItems = command.getArrearsItems().stream()
                    .map(item -> new ArrearsItemCommand(
                            item.getFeeItemId(),
                            item.getDeclaredAmount(),
                            item.getArrearsReasonCode()))
                    .toList();
            applicationService.replaceArrearsItems(
                    state.applicationId(), state.version(), arrearsItems, operatorUserId);
            state = applicationService.getRequiredState(state.applicationId());
        }

        return toView(state);
    }

    /**
     * 保存附件文件并登记附件元数据，供后续审核详情读取。
     */
    @Override
    @SneakyThrows
    public void uploadAttachment(
            Long applicationId,
            MultipartFile file,
            String requestId,
            Long operatorUserId
    ) {
        String fileId = UUID.randomUUID().toString();
        Path uploadDirectory = Path.of("uploads", "school-proxy").toAbsolutePath();
        Files.createDirectories(uploadDirectory);
        Path target = uploadDirectory.resolve(fileId);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        jdbcTemplate.update(
                "INSERT INTO application_attachment "
                        + "(application_id, file_id, original_filename, content_type, file_size) "
                        + "VALUES (?, ?, ?, ?, ?)",
                applicationId,
                fileId,
                file.getOriginalFilename(),
                file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                file.getSize()
        );
    }

    /**
     * 调用成员三首次提交状态机，把草稿推进到辅导员待审核状态。
     */
    @Override
    public SchoolProxyApplicationVO submit(
            Long applicationId,
            Integer expectedVersion,
            String requestId,
            Long operatorUserId
    ) {
        ApprovalWorkflowService approvalWorkflowService = new ApprovalWorkflowService(
                approvalStateMachine,
                approvalStateQueryService,
                approvalStateWriteService,
                approvalRecordMapper
        );
        approvalWorkflowService.submitInitial(
                applicationId, expectedVersion, requestId, operatorUserId);
        return toView(applicationService.getRequiredState(applicationId));
    }

    /** 把成员二的申请状态快照转换为第八页使用的返回结构。 */
    private SchoolProxyApplicationVO toView(ApplicationStateSnapshot state) {
        ApplicationSummary application = applicationService.findOne(state.applicationId());
        SchoolProxyApplicationVO result = new SchoolProxyApplicationVO();
        result.setApplicationId(state.applicationId());
        result.setApplicationNo(application.applicationNo());
        result.setSource("SCHOOL_PROXY");
        result.setStatus(state.status().name());
        result.setVersion(state.version());
        return result;
    }
}
