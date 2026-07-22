package com.example.backend.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.JsonResponse;
import com.example.backend.model.dto.LoginUser;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.supplement.SupplementCreateDTO;
import com.example.backend.model.dto.supplement.SupplementQueryDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.model.vo.supplement.SupplementApplicationVO;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.service.ISupplementApplicationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 6.1.4 绿色通道线下补录 Controller。
 *
 * <p>保持视频中的写法：Controller 通过字段 {@code @Autowired} 调用 Service，
 * 只负责接收 HTTP 参数和包装 JsonResponse，业务校验和事务全部放在 Service。</p>
 */
@RestController
@RequestMapping("/api/supplements")
public class SupplementApplicationController {

    @Autowired
    private ISupplementApplicationService supplementApplicationService;

    /** 操作人统一取自成员一登录模块写入的 JWT 上下文。 */
    @Autowired
    private ICurrentUserProvider currentUserProvider;

    /** 按学号查询学生，供补录表单确认学生身份。 */
    @GetMapping("/students")
    public JsonResponse<SchoolProxyStudentVO> findStudent(
            @RequestParam String studentNo
    ) {
        return JsonResponse.success(
                supplementApplicationService.findStudent(studentNo, requireSchoolUser())
        );
    }

    /** 查询 source=SUPPLEMENT 的历史补录记录。 */
    @GetMapping
    public JsonResponse<Page<SupplementApplicationVO>> pageSupplements(
            SupplementQueryDTO query,
            PageDTO page
    ) {
        return JsonResponse.success(
                supplementApplicationService.pageSupplements(query, page, requireSchoolUser())
        );
    }

    /** 查询一条补录详情。 */
    @GetMapping("/{applicationId}")
    public JsonResponse<SupplementApplicationVO> getSupplement(
            @PathVariable Long applicationId
    ) {
        return JsonResponse.success(
                supplementApplicationService.getSupplement(applicationId, requireSchoolUser())
        );
    }

    /** 创建线下补录并在同一事务中完成自动审核。 */
    @PostMapping
    public JsonResponse<SupplementApplicationVO> createSupplement(
            @Valid @RequestBody SupplementCreateDTO request
    ) {
        SupplementApplicationVO result = supplementApplicationService
                .createSupplement(request, requireSchoolUser());
        return JsonResponse.success(result, "线下补录完成");
    }

    /** 返回已通过身份校验的学校管理员用户 ID。 */
    private Long requireSchoolUser() {
        LoginUser user = currentUserProvider.getRequiredUser();
        if (user.getRoles() == null || !user.getRoles().contains("SCHOOL")) {
            throw new SecurityException("仅学校管理员可执行线下补录");
        }
        return user.getUserId();
    }
}
