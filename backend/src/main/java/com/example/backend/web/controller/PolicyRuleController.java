package com.example.backend.web.controller;

import com.example.backend.common.JsonResponse;
import com.example.backend.model.dto.LoginUser;
import com.example.backend.model.dto.PolicyRuleRequest;
import com.example.backend.model.dto.PolicyRuleVO;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.service.PolicyRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/policy-rules")
@RequiredArgsConstructor
public class PolicyRuleController {

    private final PolicyRuleService policyRuleService;
    private final ICurrentUserProvider currentUsers;

    @GetMapping("/enabled")
    public JsonResponse<List<PolicyRuleVO>> enabled(
            @RequestParam(defaultValue = "GREEN_CHANNEL") String batchType) {
        return JsonResponse.success(policyRuleService.listEnabledRules(batchType));
    }

    @GetMapping
    public JsonResponse<List<PolicyRuleVO>> list() {
        requireSchool();
        return JsonResponse.success(policyRuleService.listAllRules());
    }

    @PostMapping
    public JsonResponse<PolicyRuleVO> create(@Valid @RequestBody PolicyRuleRequest request) {
        requireSchool();
        return JsonResponse.success(policyRuleService.create(request), "政策规则创建成功");
    }

    @PutMapping("/{id}")
    public JsonResponse<PolicyRuleVO> update(
            @PathVariable Long id,
            @Valid @RequestBody PolicyRuleRequest request) {
        requireSchool();
        return JsonResponse.success(policyRuleService.update(id, request), "政策规则更新成功");
    }

    @DeleteMapping("/{id}")
    public JsonResponse<Void> delete(@PathVariable Long id) {
        requireSchool();
        policyRuleService.delete(id);
        return JsonResponse.successMessage("政策规则删除成功");
    }

    private void requireSchool() {
        LoginUser user = currentUsers.getRequiredUser();
        if (user.getRoles() == null || !user.getRoles().contains("SCHOOL")) {
            throw new SecurityException("仅学校管理员可维护政策规则");
        }
    }
}
