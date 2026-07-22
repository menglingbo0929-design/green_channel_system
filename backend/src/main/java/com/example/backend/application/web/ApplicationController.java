package com.example.backend.application.web;

import com.example.backend.application.dto.*;
import com.example.backend.application.service.ApplicationService;
import com.example.backend.application.service.GreenChannelEligibilityService;
import com.example.backend.application.service.StudentApplicationSubmissionService;
import com.example.backend.application.domain.ApplicationType;
import com.example.backend.application.exception.ApplicationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 身份上下文接入成员一前，studentId/operatorId 仅作为受控开发占位参数。 */
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationService service;
    private final StudentApplicationSubmissionService submissions;
    private final GreenChannelEligibilityService eligibility;
    public ApplicationController(ApplicationService service, StudentApplicationSubmissionService submissions,
                                 GreenChannelEligibilityService eligibility) {
        this.service = service; this.submissions = submissions; this.eligibility = eligibility;
    }
    @PostMapping("/drafts") @ResponseStatus(HttpStatus.CREATED)
    public ApplicationStateSnapshot create(@Valid @RequestBody ApplicationDraftCommand command,
                                           @RequestHeader("X-Student-Id") Long studentId, @RequestHeader("X-User-Id") Long userId) {
        if (command.applicationType() == ApplicationType.GREEN_CHANNEL) {
            var result = eligibility.check(studentId);
            if (!result.allowed()) throw new ApplicationException(result.reasonCode(), HttpStatus.CONFLICT, result.message());
            if (!command.batchId().equals(result.batchId())) {
                throw new ApplicationException("APPLICATION_BATCH_NOT_CURRENT", HttpStatus.BAD_REQUEST, "绿色通道申请必须使用当前开放批次");
            }
        }
        return service.create(studentId, userId, command);
    }
    @GetMapping(value = "/mine", params = "eligibility")
    public GreenChannelEligibility eligibility(@RequestHeader("X-Student-Id") Long studentId) {
        return eligibility.check(studentId);
    }
    @GetMapping("/mine") public List<ApplicationSummary> mine(@RequestHeader("X-Student-Id") Long studentId) { return service.findMine(studentId); }
    @GetMapping("/{id}") public ApplicationSummary one(@PathVariable Long id) { return service.findOne(id); }
    @PutMapping("/{id}") public ApplicationSummary update(@PathVariable Long id, @Valid @RequestBody UpdateDraftRequest request, @RequestHeader("X-User-Id") Long userId) { return service.updateDraft(id, request.applicationReason(), request.version(), userId); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id, @RequestParam Integer version, @RequestHeader("X-User-Id") Long userId) { service.deleteDraft(id, version, userId); }
    @GetMapping("/{id}/arrears") public List<ArrearsItemSnapshot> arrears(@PathVariable Long id) { return service.findArrearsItems(id); }
    @PutMapping("/{id}/arrears") public List<ArrearsItemSnapshot> replaceArrears(@PathVariable Long id, @Valid @RequestBody UpdateArrearsRequest request, @RequestHeader("X-User-Id") Long userId) { return service.replaceArrearsItems(id, request.version(), request.items(), userId); }
    @GetMapping("/{id}/gifts") public List<GiftApplicationItemSnapshot> gifts(@PathVariable Long id) { return service.findGiftItems(id); }
    @PutMapping("/{id}/gifts") public List<GiftApplicationItemSnapshot> replaceGifts(@PathVariable Long id, @Valid @RequestBody UpdateGiftRequest request, @RequestHeader("X-User-Id") Long userId) { return service.replaceGiftItems(id, request.version(), request.items(), userId); }
    @GetMapping("/{id}/subsidy") public SubsidyApplicationSnapshot subsidy(@PathVariable Long id) { return service.findSubsidy(id); }
    @PutMapping("/{id}/subsidy") public SubsidyApplicationSnapshot replaceSubsidy(@PathVariable Long id, @Valid @RequestBody UpdateSubsidyRequest request, @RequestHeader("X-User-Id") Long userId) { return service.replaceSubsidy(id, request.version(), request.expectedAmount(), userId); }
    @PostMapping(path = "/{id}/attachments", consumes = "multipart/form-data") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadAttachment(@PathVariable Long id, @RequestParam String requestId, @RequestParam("file") MultipartFile file,
                                 @RequestHeader("X-Student-Id") Long studentId, @RequestHeader("X-User-Id") Long userId) { submissions.upload(id, studentId, userId, requestId, file); }
    @PostMapping("/{id}/submit")
    public ApplicationStateSnapshot submit(@PathVariable Long id, @RequestParam Integer version, @RequestParam String requestId,
                                           @RequestHeader("X-Student-Id") Long studentId, @RequestHeader("X-User-Id") Long userId) {
        submissions.submit(id, studentId, userId, version, requestId); return service.getRequiredState(id);
    }
    public record UpdateDraftRequest(@NotNull Integer version, @NotBlank String applicationReason) { }
}
