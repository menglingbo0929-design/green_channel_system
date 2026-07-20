package com.example.backend.application.web;

import com.example.backend.application.dto.*;
import com.example.backend.application.service.ApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 身份上下文接入成员一前，studentId/operatorId 仅作为受控开发占位参数。 */
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationService service;
    public ApplicationController(ApplicationService service) { this.service = service; }
    @PostMapping("/drafts") @ResponseStatus(HttpStatus.CREATED)
    public ApplicationStateSnapshot create(@Valid @RequestBody ApplicationDraftCommand command,
                                           @RequestHeader("X-Student-Id") Long studentId, @RequestHeader("X-User-Id") Long userId) { return service.create(studentId, userId, command); }
    @GetMapping("/mine") public List<ApplicationSummary> mine(@RequestHeader("X-Student-Id") Long studentId) { return service.findMine(studentId); }
    @GetMapping("/{id}") public ApplicationSummary one(@PathVariable Long id) { return service.findOne(id); }
    @PutMapping("/{id}") public ApplicationSummary update(@PathVariable Long id, @Valid @RequestBody UpdateDraftRequest request, @RequestHeader("X-User-Id") Long userId) { return service.updateDraft(id, request.applicationReason(), request.version(), userId); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id, @RequestParam Integer version, @RequestHeader("X-User-Id") Long userId) { service.deleteDraft(id, version, userId); }
    @GetMapping("/{id}/arrears") public List<ArrearsItemSnapshot> arrears(@PathVariable Long id) { return service.findArrearsItems(id); }
    @PutMapping("/{id}/arrears") public List<ArrearsItemSnapshot> replaceArrears(@PathVariable Long id, @Valid @RequestBody UpdateArrearsRequest request, @RequestHeader("X-User-Id") Long userId) { return service.replaceArrearsItems(id, request.version(), request.items(), userId); }
    public record UpdateDraftRequest(@NotNull Integer version, @NotBlank String applicationReason) { }
}
