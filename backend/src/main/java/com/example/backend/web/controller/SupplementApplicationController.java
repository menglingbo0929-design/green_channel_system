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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
//线下申请补录
//和待申请区分，一个是申请，一个是补信息，
@RestController
@RequestMapping("/api/supplements")
@RequiredArgsConstructor
public class SupplementApplicationController {

    private final ISupplementApplicationService supplementApplicationService;
    private final ICurrentUserProvider currentUsers;
    //按学号查询学生信息
    @GetMapping("/students")
    public JsonResponse<SchoolProxyStudentVO> findStudent(@RequestParam String studentNo) {
        return JsonResponse.success(supplementApplicationService
                .findStudent(studentNo, currentSchoolUserId()));
    }
    //支持线下补录页面中补录历史表格的查看和分页
    @GetMapping
    public JsonResponse<Page<SupplementApplicationVO>> pageSupplements(
            SupplementQueryDTO query, PageDTO page) {
        return JsonResponse.success(supplementApplicationService
                .pageSupplements(query, page, currentSchoolUserId()));
    }
    //查看补录历史中某条记录的内容
    @GetMapping("/{applicationId}")
    public JsonResponse<SupplementApplicationVO> getSupplement(@PathVariable Long applicationId) {
        return JsonResponse.success(supplementApplicationService
                .getSupplement(applicationId, currentSchoolUserId()));
    }
    //如果学生已经有线下办理过绿色通道业务但是系统上没有显示，学校管理员会把这个补进去
    @PostMapping
    public JsonResponse<SupplementApplicationVO> createSupplement(
            @Valid @RequestBody SupplementCreateDTO request) {
        return JsonResponse.success(supplementApplicationService
                .createSupplement(request, currentSchoolUserId()), "线下补录完成");
    }
    //获得执行操作的学校管理员的id，记录该次线下补录由谁来进行
    private Long currentSchoolUserId() {
        LoginUser user = currentUsers.getRequiredUser();
        if (user.getRoles() == null || !user.getRoles().contains("SCHOOL")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only SCHOOL users can manage supplements");
        }
        return user.getUserId();
    }

}
