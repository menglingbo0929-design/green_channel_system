package com.example.backend.service.impl;

import com.example.backend.model.domain.Application;
import com.example.backend.model.domain.ApplicationSource;
import com.example.backend.model.domain.ApplicationStatus;
import com.example.backend.common.exception.ApplicationException;
import com.example.backend.mapper.ApplicationMapper;
import com.example.backend.mapper.ApplicationOperationMapper;
import com.example.backend.mapper.ApplicationResourceMapper;
import com.example.backend.model.dto.ApplicationAttachmentSnapshot;
import com.example.backend.model.dto.ApplicationAttachmentContent;
import com.example.backend.service.ApplicationAttachmentReadService;
import com.example.backend.service.ApprovalTransitionService;
import com.example.backend.service.ApprovalResourceService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Student-facing attachment and submit boundary owned by member two. */
@Service
public class StudentApplicationSubmissionService implements ApplicationAttachmentReadService {
    private final ApplicationMapper applications;
    private final ApplicationResourceMapper resources;
    private final ApplicationOperationMapper operations;
    private final ApprovalResourceService resourceService;
    private final ApprovalTransitionService transitionService;
    @Value("${application.attachment-storage-path:./private-uploads}") private String attachmentStoragePath;

    public StudentApplicationSubmissionService(ApplicationMapper applications, ApplicationResourceMapper resources,
                                                ApplicationOperationMapper operations, ApprovalResourceService resourceService,
                                                ApprovalTransitionService transitionService) {
        this.applications = applications; this.resources = resources; this.operations = operations;
        this.resourceService = resourceService; this.transitionService = transitionService;
    }

    @Transactional
    public void upload(Long applicationId, Long studentId, Long operatorId, String requestId, MultipartFile file) {
        Application application = requiredStudentApplication(applicationId, studentId);
        requireEditable(application); validateRequestId(requestId);
        if (operations.countByApplicationOperationRequest(applicationId, "UPLOAD_ATTACHMENT", requestId) > 0) return;
        Attachment payload = validate(file);
        String fileId = UUID.randomUUID().toString().replace("-", "");
        Path directory = Path.of(attachmentStoragePath == null ? "./private-uploads" : attachmentStoragePath, "student").toAbsolutePath().normalize();
        Path target = directory.resolve(fileId).normalize();
        try {
            Files.createDirectories(directory); Files.write(target, payload.content(), StandardOpenOption.CREATE_NEW);
            resources.insertAttachment(applicationId, fileId, payload.name(), payload.contentType(), payload.content().length);
            operations.insert(applicationId, "UPLOAD_ATTACHMENT", requestId, operatorId);
        } catch (IOException | RuntimeException exception) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) { }
            throw new ApplicationException("APPLICATION_ATTACHMENT_STORE_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "附件保存失败");
        }
    }

    @Transactional
    public void submit(Long applicationId, Long studentId, Long operatorId, Integer version, String requestId) {
        Application application = requiredStudentApplication(applicationId, studentId);
        validateRequestId(requestId); requireEditable(application);
        if (!application.getVersion().equals(version)) throw conflict("APPLICATION_VERSION_CONFLICT", "申请版本已变化");
        if (resources.countActiveAttachments(applicationId) < 1) throw conflict("APPLICATION_ATTACHMENT_REQUIRED", "正式提交前至少上传一份证明附件");
        boolean resubmission = application.getStatus().isReturned();
        if (!resubmission) {
            resourceService.reserveOnSubmit(applicationId, requestId, operatorId);
        }
        if (resubmission) {
            transitionService.resubmitReturned(applicationId, version, requestId, operatorId);
        } else {
            transitionService.submitInitial(applicationId, version, requestId, operatorId);
        }
    }

    public ApplicationAttachmentContent readAttachment(Long applicationId, Long studentId, Long attachmentId) {
        requiredStudentApplication(applicationId, studentId);
        return readForAuthorizedReviewer(applicationId, attachmentId);
    }

    @Override
    public ApplicationAttachmentContent readForAuthorizedReviewer(Long applicationId, Long attachmentId) {
        ApplicationAttachmentSnapshot attachment = resources.findAttachment(applicationId, attachmentId);
        if (attachment == null) throw new ApplicationException("APPLICATION_ATTACHMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "附件不存在");
        Path file = Path.of(attachmentStoragePath == null ? "./private-uploads" : attachmentStoragePath, "student", attachment.fileId()).toAbsolutePath().normalize();
        try {
            if (!Files.isRegularFile(file)) throw new ApplicationException("APPLICATION_ATTACHMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "附件文件不存在");
            return new ApplicationAttachmentContent(attachment.originalFilename(), attachment.contentType(), Files.readAllBytes(file));
        } catch (IOException exception) {
            throw new ApplicationException("APPLICATION_ATTACHMENT_READ_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "附件读取失败");
        }
    }

    private Application requiredStudentApplication(Long applicationId, Long studentId) {
        Application application = applications.findRequired(applicationId);
        if (application == null || application.getSource() != ApplicationSource.STUDENT || !application.getStudentId().equals(studentId)) {
            throw new ApplicationException("APPLICATION_FORBIDDEN", HttpStatus.FORBIDDEN, "无权操作该申请");
        }
        return application;
    }
    private void requireEditable(Application application) { if (application.getStatus() != ApplicationStatus.DRAFT && application.getStatus() != ApplicationStatus.COUNSELOR_RETURNED && application.getStatus() != ApplicationStatus.COLLEGE_RETURNED && application.getStatus() != ApplicationStatus.SCHOOL_RETURNED) throw conflict("APPLICATION_INVALID_STATUS", "当前申请不能上传附件或正式提交"); }
    private void validateRequestId(String requestId) { if (requestId == null || requestId.isBlank() || requestId.length() > 64) throw conflict("APPLICATION_REQUEST_INVALID", "requestId 必须为 1 到 64 个字符"); }
    private Attachment validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 10L * 1024 * 1024) throw conflict("APPLICATION_ATTACHMENT_SIZE_EXCEEDED", "附件必须介于 1 B 与 10 MiB 之间");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().replace('\\', '/'); name = name.substring(name.lastIndexOf('/') + 1);
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase() : ""; String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        try { byte[] content = file.getBytes(); boolean pdf = ext.equals(".pdf") && type.equals("application/pdf") && starts(content, "%PDF-".getBytes()); boolean png = ext.equals(".png") && type.equals("image/png") && starts(content, new byte[]{(byte)0x89,0x50,0x4e,0x47}); boolean jpeg = (ext.equals(".jpg") || ext.equals(".jpeg")) && type.equals("image/jpeg") && starts(content, new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff}); if (!pdf && !png && !jpeg) throw conflict("APPLICATION_ATTACHMENT_TYPE_INVALID", "仅支持 PDF、JPG、JPEG 或 PNG 证明附件"); return new Attachment(name, type, content); } catch (IOException exception) { throw conflict("APPLICATION_ATTACHMENT_STORE_FAILED", "附件读取失败"); }
    }
    private boolean starts(byte[] content, byte[] prefix) { return content.length >= prefix.length && Arrays.equals(Arrays.copyOf(content, prefix.length), prefix); }
    private ApplicationException conflict(String code, String message) { return new ApplicationException(code, HttpStatus.CONFLICT, message); }
    private record Attachment(String name, String contentType, byte[] content) { }
}
