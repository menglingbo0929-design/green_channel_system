package com.example.backend.web.controller;

import com.example.backend.common.JsonResponse;
import com.example.backend.model.dto.CreateSubsidyBatchRequest;
import com.example.backend.model.dto.SubsidyBatchVO;
import com.example.backend.model.dto.UpdateSubsidyBatchRequest;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.service.SubsidyBatchService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/batch/subsidy")
@RequiredArgsConstructor
public class SubsidyBatchController {
    private final SubsidyBatchService batchService;
    private final ICurrentUserProvider currentUsers;

    @GetMapping public JsonResponse<List<SubsidyBatchVO>> list() { requireConfigurationManager(); return JsonResponse.success(batchService.list()); }
    @PostMapping public JsonResponse<SubsidyBatchVO> create(@Valid @RequestBody CreateSubsidyBatchRequest request) { requireConfigurationManager(); return JsonResponse.success(batchService.create(request), "创建成功"); }
    @PutMapping("/{id}") public JsonResponse<SubsidyBatchVO> update(@PathVariable Long id, @Valid @RequestBody UpdateSubsidyBatchRequest request) { requireConfigurationManager(); return JsonResponse.success(batchService.update(id, request), "更新成功"); }
    @PutMapping("/{id}/status") public JsonResponse<Void> toggleStatus(@PathVariable Long id) { requireConfigurationManager(); batchService.toggleStatus(id); return JsonResponse.successMessage("操作成功"); }

    private void requireConfigurationManager() {
        var user = currentUsers.getRequiredUser();
        boolean allowed = user.getRoles() != null && user.getRoles().stream().anyMatch(role -> role.equals("SCHOOL") || role.equals("COLLEGE"));
        if (!allowed) throw new SecurityException("仅学校或学院管理员可维护补助批次");
    }
}
