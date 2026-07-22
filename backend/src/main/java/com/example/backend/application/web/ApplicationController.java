package com.example.backend.application.web;

import com.example.backend.application.domain.ApplicationType;
import com.example.backend.application.dto.ApplicationDraftCommand;
import com.example.backend.application.dto.ApplicationStateSnapshot;
import com.example.backend.application.dto.ApplicationSummary;
import com.example.backend.application.dto.ArrearsItemSnapshot;
import com.example.backend.application.dto.GiftApplicationItemSnapshot;
import com.example.backend.application.dto.GreenChannelEligibility;
import com.example.backend.application.dto.SubsidyApplicationSnapshot;
import com.example.backend.application.dto.UpdateArrearsRequest;
import com.example.backend.application.dto.UpdateGiftRequest;
import com.example.backend.application.dto.UpdateSubsidyRequest;
import com.example.backend.application.exception.ApplicationException;
import com.example.backend.application.service.ApplicationService;
import com.example.backend.application.service.GreenChannelEligibilityService;
import com.example.backend.application.service.StudentApplicationSubmissionService;
import com.example.backend.model.dto.LoginUser;
import com.example.backend.security.ICurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Student application endpoints derive identity exclusively from the authenticated JWT context. */
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationService service;
    private final StudentApplicationSubmissionService submissions;
    private final GreenChannelEligibilityService eligibility;
    private final ICurrentUserProvider currentUsers;

    public ApplicationController(ApplicationService service, StudentApplicationSubmissionService submissions,
                                 GreenChannelEligibilityService eligibility, ICurrentUserProvider currentUsers) {
        this.service = service;
        this.submissions = submissions;
        this.eligibility = eligibility;
        this.currentUsers = currentUsers;
    }

    @PostMapping("/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationStateSnapshot create(@Valid @RequestBody ApplicationDraftCommand command) {
        LoginUser user = currentStudent();
        if (command.applicationType() == ApplicationType.GREEN_CHANNEL) {
            var result = eligibility.check(user.getStudentId());
            if (!result.allowed()) throw new ApplicationException(result.reasonCode(), HttpStatus.CONFLICT, result.message());
            if (!command.batchId().equals(result.batchId())) {
                throw new ApplicationException("APPLICATION_BATCH_NOT_CURRENT", HttpStatus.BAD_REQUEST, "绿色通道申请必须使用当前开放批次");
            }
        }
        return service.create(user.getStudentId(), user.getUserId(), command);
    }

    @GetMapping(value = "/mine", params = "eligibility")
    public GreenChannelEligibility eligibility() {
        return eligibility.check(currentStudent().getStudentId());
    }

    @GetMapping("/mine")
    public List<ApplicationSummary> mine() {
        return service.findMine(currentStudent().getStudentId());
    }

    @GetMapping("/{id}")
    public ApplicationSummary one(@PathVariable Long id) {
        assertOwns(id);
        return service.findOne(id);
    }

    @PutMapping("/{id}")
    public ApplicationSummary update(@PathVariable Long id, @Valid @RequestBody UpdateDraftRequest request) {
        LoginUser user = assertOwns(id);
        return service.updateDraft(id, request.applicationReason(), request.version(), user.getUserId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestParam Integer version) {
        LoginUser user = assertOwns(id);
        service.deleteDraft(id, version, user.getUserId());
    }

    @GetMapping("/{id}/arrears")
    public List<ArrearsItemSnapshot> arrears(@PathVariable Long id) {
        assertOwns(id);
        return service.findArrearsItems(id);
    }

    @PutMapping("/{id}/arrears")
    public List<ArrearsItemSnapshot> replaceArrears(@PathVariable Long id, @Valid @RequestBody UpdateArrearsRequest request) {
        LoginUser user = assertOwns(id);
        return service.replaceArrearsItems(id, request.version(), request.items(), user.getUserId());
    }

    @GetMapping("/{id}/gifts")
    public List<GiftApplicationItemSnapshot> gifts(@PathVariable Long id) {
        assertOwns(id);
        return service.findGiftItems(id);
    }

    @PutMapping("/{id}/gifts")
    public List<GiftApplicationItemSnapshot> replaceGifts(@PathVariable Long id, @Valid @RequestBody UpdateGiftRequest request) {
        LoginUser user = assertOwns(id);
        return service.replaceGiftItems(id, request.version(), request.items(), user.getUserId());
    }

    @GetMapping("/{id}/subsidy")
    public SubsidyApplicationSnapshot subsidy(@PathVariable Long id) {
        assertOwns(id);
        return service.findSubsidy(id);
    }

    @PutMapping("/{id}/subsidy")
    public SubsidyApplicationSnapshot replaceSubsidy(@PathVariable Long id, @Valid @RequestBody UpdateSubsidyRequest request) {
        LoginUser user = assertOwns(id);
        return service.replaceSubsidy(id, request.version(), request.expectedAmount(), user.getUserId());
    }

    @PostMapping(path = "/{id}/attachments", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadAttachment(@PathVariable Long id, @RequestParam String requestId, @RequestParam("file") MultipartFile file) {
        LoginUser user = assertOwns(id);
        submissions.upload(id, user.getStudentId(), user.getUserId(), requestId, file);
    }

    @GetMapping("/{id}/attachments/{attachmentId}/content")
    public ResponseEntity<ByteArrayResource> readAttachment(@PathVariable Long id, @PathVariable Long attachmentId) {
        LoginUser user = assertOwns(id);
        var content = submissions.readAttachment(id, user.getStudentId(), attachmentId);
        MediaType type;
        try { type = MediaType.parseMediaType(content.contentType()); } catch (Exception ignored) { type = MediaType.APPLICATION_OCTET_STREAM; }
        return ResponseEntity.ok().contentType(type).contentLength(content.content().length)
                .header("Content-Disposition", ContentDisposition.inline().filename(content.originalFilename()).build().toString())
                .body(new ByteArrayResource(content.content()));
    }

    @PostMapping("/{id}/submit")
    public ApplicationStateSnapshot submit(@PathVariable Long id, @RequestParam Integer version, @RequestParam String requestId) {
        LoginUser user = assertOwns(id);
        submissions.submit(id, user.getStudentId(), user.getUserId(), version, requestId);
        return service.getRequiredState(id);
    }

    private LoginUser currentStudent() {
        LoginUser user = currentUsers.getRequiredUser();
        if (user.getUserId() == null || user.getStudentId() == null || user.getRoles() == null || !user.getRoles().contains("STUDENT")) {
            throw new ApplicationException("APPLICATION_FORBIDDEN", HttpStatus.FORBIDDEN, "当前登录用户不是已关联学生档案的学生账号");
        }
        return user;
    }

    private LoginUser assertOwns(Long applicationId) {
        LoginUser user = currentStudent();
        if (!user.getStudentId().equals(service.getRequiredState(applicationId).studentId())) {
            throw new ApplicationException("APPLICATION_FORBIDDEN", HttpStatus.FORBIDDEN, "无权访问其他学生的申请");
        }
        return user;
    }

    public record UpdateDraftRequest(@NotNull Integer version, @NotBlank String applicationReason) { }
}
