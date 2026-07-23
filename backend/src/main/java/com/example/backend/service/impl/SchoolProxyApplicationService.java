package com.example.backend.service.impl;

import com.example.backend.model.domain.*;
import com.example.backend.model.dto.*;
import com.example.backend.common.exception.ApplicationException;
import com.example.backend.mapper.ApplicationMapper;
import com.example.backend.mapper.ApplicationOperationMapper;
import com.example.backend.mapper.ApplicationResourceMapper;
import com.example.backend.service.ApprovalTransitionService;
import com.example.backend.service.ApprovalResourceService;
import com.example.backend.model.dto.schoolproxy.*;
import com.example.backend.model.vo.schoolproxy.*;
import com.example.backend.service.port.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 成员二学校代申请：主表与明细由本模块写入，学生查找和审核流转经跨成员 Port。 */
@Service
public class SchoolProxyApplicationService implements SchoolProxyApplicationPort {
    private final ApplicationService applications; private final ApplicationMapper mapper;
    private final ApplicationOperationMapper operations; private final ApplicationResourceMapper resources;
    private final SchoolProxyStudentQueryPort studentQueries;
    private final ApprovalTransitionService transitionService;
    private final ApprovalResourceService resourceService;
    @Value("${application.attachment-storage-path:./private-uploads}") private String attachmentStoragePath;
    public SchoolProxyApplicationService(ApplicationService applications, ApplicationMapper mapper, ApplicationOperationMapper operations,
                                         ApplicationResourceMapper resources, SchoolProxyStudentQueryPort studentQueries,
                                         ApprovalTransitionService transitionService, ApprovalResourceService resourceService) {
        this.applications=applications; this.mapper=mapper; this.operations=operations; this.resources=resources;
        this.studentQueries=studentQueries; this.transitionService=transitionService; this.resourceService=resourceService;
    }
    @Override @Transactional public SchoolProxyApplicationVO createDraft(SchoolProxyDraftDTO command, Long operatorUserId) {
        Long previous=operations.findApplicationIdByRequestId(command.getRequestId());
        if(previous!=null) return toView(requireProxy(previous));
        if (!"GREEN_CHANNEL".equals(command.getBatchType())) throw bad("SCHOOL_PROXY_BATCH_TYPE_INVALID", "学校代申请仅支持绿色通道批次");
        SchoolProxyStudentVO student=studentQueries.findEnabledStudentByStudentNo(command.getStudentNo());
        if(student==null || student.getStudentId()==null) throw new ApplicationException("SCHOOL_PROXY_STUDENT_NOT_FOUND", HttpStatus.NOT_FOUND, "学生不存在或已停用");
        ApplicationStateSnapshot state=applications.createSchoolProxyApplication(student.getStudentId(), operatorUserId,
                new ApplicationDraftCommand(ApplicationType.GREEN_CHANNEL, BatchType.GREEN_CHANNEL, command.getBatchId(), command.getRequestId(), command.getApplicationReason()));
        if(!command.getArrearsItems().isEmpty()) {
            applications.replaceArrearsItems(state.applicationId(), state.version(), command.getArrearsItems().stream()
                    .map(x -> new ArrearsItemCommand(x.getFeeItemId(),x.getDeclaredAmount(),x.getArrearsReasonCode())).toList(), operatorUserId);
        }
        state=applications.getRequiredState(state.applicationId());
        if(!command.getGiftItems().isEmpty()) applications.replaceGiftItems(state.applicationId(), state.version(), toGiftItems(command.getBatchId(), command.getGiftItems()), operatorUserId);
        return toView(requireProxy(state.applicationId()));
    }
    @Override @Transactional public void uploadAttachment(Long applicationId, org.springframework.web.multipart.MultipartFile file, String requestId, Long operatorUserId) {
        Application application = requireProxy(applicationId);
        if (application.getStatus() != ApplicationStatus.DRAFT && !isReturned(application.getStatus())) throw bad("APPLICATION_INVALID_STATUS", "仅草稿或退回申请可上传附件");
        validateRequestId(requestId);
        if (operations.countByApplicationOperationRequest(applicationId, "UPLOAD_ATTACHMENT", requestId) > 0) return;
        AttachmentPayload payload = validateAttachment(file);
        String fileId = UUID.randomUUID().toString().replace("-", "");
        Path directory = Path.of(attachmentStoragePath == null ? "./private-uploads" : attachmentStoragePath, "school-proxy").toAbsolutePath().normalize();
        Path target = directory.resolve(fileId).normalize();
        if (!target.startsWith(directory)) throw bad("APPLICATION_ATTACHMENT_TYPE_INVALID", "附件存储路径无效");
        try {
            Files.createDirectories(directory);
            Files.write(target, payload.content(), StandardOpenOption.CREATE_NEW);
            resources.insertAttachment(applicationId, fileId, payload.originalFilename(), payload.contentType(), payload.content().length);
            operations.insert(applicationId, "UPLOAD_ATTACHMENT", requestId, operatorUserId);
        } catch (IOException | RuntimeException exception) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) { }
            throw new ApplicationException("APPLICATION_ATTACHMENT_STORE_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "附件保存失败");
        }
    }
    @Override @Transactional public SchoolProxyApplicationVO submit(Long applicationId, Integer expectedVersion, String requestId, Long operatorUserId) {
        Application application = requireProxy(applicationId);
        validateRequestId(requestId);
        if (application.getStatus() != ApplicationStatus.DRAFT && !isReturned(application.getStatus())) throw bad("APPLICATION_INVALID_STATUS", "当前申请不能正式提交");
        if (resources.countActiveAttachments(applicationId) < 1) throw bad("APPLICATION_ATTACHMENT_REQUIRED", "正式提交前至少上传一份证明附件");
        resourceService.reserveOnSubmit(applicationId, requestId, operatorUserId);
        transitionService.submitInitial(applicationId, expectedVersion, requestId, operatorUserId);
        return toView(requireProxy(applicationId));
    }
    private List<GiftApplicationItemCommand> toGiftItems(Long batchId, List<SchoolProxyGiftItemDTO> items) {
        return items.stream().map(item -> { Long id=resources.findBatchGiftItemId(batchId,item.getGiftItemId());
            if(id==null) throw bad("SCHOOL_PROXY_GIFT_ITEM_INVALID", "礼包物品不属于当前批次"); return new GiftApplicationItemCommand(id,item.getQuantity()); }).toList();
    }
    private Application requireProxy(Long id) { Application a=mapper.findBySource(id,ApplicationSource.SCHOOL_PROXY); if(a==null) throw new ApplicationException("SCHOOL_PROXY_APPLICATION_NOT_FOUND",HttpStatus.NOT_FOUND,"学校代申请不存在"); return a; }
    private boolean isReturned(ApplicationStatus status) { return status == ApplicationStatus.COUNSELOR_RETURNED || status == ApplicationStatus.COLLEGE_RETURNED || status == ApplicationStatus.SCHOOL_RETURNED; }
    private void validateRequestId(String requestId) { if (requestId == null || requestId.isBlank() || requestId.length() > 64) throw bad("APPLICATION_REQUEST_INVALID", "requestId 必须为 1 到 64 个字符"); }
    private AttachmentPayload validateAttachment(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 10L * 1024 * 1024) throw bad("APPLICATION_ATTACHMENT_SIZE_EXCEEDED", "附件必须介于 1 B 与 10 MiB 之间");
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().replace('\\', '/');
        original = original.substring(original.lastIndexOf('/') + 1);
        String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')).toLowerCase() : "";
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        try {
            byte[] content = file.getBytes();
            boolean pdf = extension.equals(".pdf") && contentType.equals("application/pdf") && startsWith(content, "%PDF-".getBytes());
            boolean png = extension.equals(".png") && contentType.equals("image/png") && startsWith(content, new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47});
            boolean jpeg = (extension.equals(".jpg") || extension.equals(".jpeg")) && contentType.equals("image/jpeg") && startsWith(content, new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});
            if (!pdf && !png && !jpeg) throw bad("APPLICATION_ATTACHMENT_TYPE_INVALID", "仅支持 PDF、JPG、JPEG 或 PNG 证明附件");
            return new AttachmentPayload(original, contentType, content);
        } catch (IOException exception) { throw new ApplicationException("APPLICATION_ATTACHMENT_STORE_FAILED", HttpStatus.BAD_REQUEST, "附件读取失败"); }
    }
    private boolean startsWith(byte[] content, byte[] prefix) { return content.length >= prefix.length && Arrays.equals(Arrays.copyOf(content, prefix.length), prefix); }
    private record AttachmentPayload(String originalFilename, String contentType, byte[] content) { }
    private SchoolProxyApplicationVO toView(Application a) { SchoolProxyApplicationVO v=new SchoolProxyApplicationVO(); v.setApplicationId(a.getId());v.setApplicationNo(a.getApplicationNo());v.setSource(a.getSource().name());v.setStatus(a.getStatus().name());v.setVersion(a.getVersion());return v; }
    private ApplicationException bad(String code,String message){return new ApplicationException(code,HttpStatus.BAD_REQUEST,message);}
}
