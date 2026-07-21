package com.example.backend.web.controller;

import com.example.backend.common.JsonResponse;
import com.example.backend.model.dto.BatchVO;
import com.example.backend.model.dto.CreateBatchRequest;
import com.example.backend.model.dto.UpdateBatchRequest;
import com.example.backend.service.GreenChannelBatchService;
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

    /** 列表 */
    @GetMapping
    public JsonResponse<List<BatchVO>> list() {
        return JsonResponse.success(batchService.list());
    }

    /** 详情 */
    @GetMapping("{id}")
    public JsonResponse<BatchVO> detail(@PathVariable Long id) {
        return JsonResponse.success(batchService.getDetail(id));
    }

    /** 创建 */
    @PostMapping
    public JsonResponse<BatchVO> create(@Valid @RequestBody CreateBatchRequest request) {
        return JsonResponse.success(batchService.create(request), "创建成功");
    }

    /** 编辑 */
    @PutMapping("{id}")
    public JsonResponse<BatchVO> update(@PathVariable Long id,
                                        @Valid @RequestBody UpdateBatchRequest request) {
        return JsonResponse.success(batchService.update(id, request), "更新成功");
    }

    /** 切换启用/停用 */
    @PutMapping("{id}/status")
    public JsonResponse<Void> toggleStatus(@PathVariable Long id) {
        batchService.toggleStatus(id);
        return JsonResponse.successMessage("操作成功");
    }

    /** 设置适用年级 */
    @PutMapping("{id}/grades")
    public JsonResponse<Void> setGrades(@PathVariable Long id,
                                        @RequestBody Map<String, List<Long>> body) {
        batchService.setEligibleGrades(id, body.get("gradeIds"));
        return JsonResponse.successMessage("设置成功");
    }
}
