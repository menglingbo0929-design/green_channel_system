package com.example.backend.web.controller;

import com.example.backend.common.JsonResponse;
import com.example.backend.mapper.StudentMapper;
import com.example.backend.model.domain.Student;
import com.example.backend.model.dto.BatchSnapshot;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.service.BatchQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
public class ApplicationBatchController {

    private final BatchQueryService batches;
    private final ICurrentUserProvider currentUserProvider;
    private final StudentMapper studentMapper;

    /** 学生申请中心：只返回本人年级在当前时间可申请的批次。 */
    @GetMapping("/available")
    public JsonResponse<List<BatchSnapshot>> available(@RequestParam String applicationType) {
        Long studentId = currentUserProvider.getRequiredUser().getStudentId();
        if (studentId == null) throw new IllegalStateException("当前登录账号未关联学生档案");
        Student student = studentMapper.selectById(studentId);
        if (student == null || student.getDeleted() != 0 || student.getEnabled() != 1) {
            throw new IllegalStateException("当前学生档案不存在或已停用");
        }
        return JsonResponse.success(batches.listAvailableBatches(applicationType, student.getGradeId()));
    }

    /** 审批工作台：按业务批次类型返回所有启用、开放的批次。 */
    @GetMapping("/open")
    public JsonResponse<List<BatchSnapshot>> open(@RequestParam String batchType) {
        currentUserProvider.getRequiredUser();
        return JsonResponse.success(batches.listOpenBatches(batchType));
    }
}
