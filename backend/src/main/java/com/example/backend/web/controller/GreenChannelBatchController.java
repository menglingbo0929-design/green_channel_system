package com.example.backend.web.controller;

import com.example.backend.common.JsonResponse;
import com.example.backend.model.dto.BatchVO;
import com.example.backend.model.dto.CreateBatchRequest;
import com.example.backend.model.dto.UpdateBatchRequest;
import com.example.backend.service.GreenChannelBatchService;
import com.example.backend.security.ICurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch/green-channel")
@RequiredArgsConstructor
public class GreenChannelBatchController {

    private final GreenChannelBatchService batchService;
    private final ICurrentUserProvider currentUsers;

    /** 列表 */
    @GetMapping
    public JsonResponse<List<BatchVO>> list() {
        requireSchool();
        return JsonResponse.success(batchService.list());
    }

    /** 详情 */
    @GetMapping("{id}")
    public JsonResponse<BatchVO> detail(@PathVariable Long id) {
        requireSchool();
        return JsonResponse.success(batchService.getDetail(id));
    }

    /** 创建 */
    @PostMapping
    public JsonResponse<BatchVO> create(@Valid @RequestBody CreateBatchRequest request) {
        requireSchool();
        return JsonResponse.success(batchService.create(request), "创建成功");
    }

    /** 编辑 */
    @PutMapping("{id}")
    public JsonResponse<BatchVO> update(@PathVariable Long id,
                                        @Valid @RequestBody UpdateBatchRequest request) {
        requireSchool();
        return JsonResponse.success(batchService.update(id, request), "更新成功");
    }

    /** 切换启用/停用 */
    @PutMapping("{id}/status")
    public JsonResponse<Void> toggleStatus(@PathVariable Long id) {
        requireSchool();
        batchService.toggleStatus(id);
        return JsonResponse.successMessage("操作成功");
    }

    /** 设置适用年级 */
    @PutMapping("{id}/grades")
    public JsonResponse<Void> setGrades(@PathVariable Long id,
                                        @RequestBody Map<String, List<Long>> body) {
        requireSchool();
        batchService.setEligibleGrades(id, body.get("gradeIds"));
        return JsonResponse.successMessage("设置成功");
    }

    private void requireSchool() {
        var user = currentUsers.getRequiredUser();
        if (user.getRoles() == null || !user.getRoles().contains("SCHOOL")) {
            throw new SecurityException("仅学校管理员可维护绿色通道批次");
        }
    }
}
