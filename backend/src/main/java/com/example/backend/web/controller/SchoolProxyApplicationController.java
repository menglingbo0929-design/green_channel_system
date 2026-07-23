package com.example.backend.web.controller;

import com.example.backend.common.JsonResponse;
import com.example.backend.model.dto.LoginUser;
import com.example.backend.model.dto.schoolproxy.SchoolProxyDraftDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyApplicationVO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.service.ISchoolProxyApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
//学校待申请相关功能
@RestController
@RequestMapping("/api/school-proxy")
@RequiredArgsConstructor
public class SchoolProxyApplicationController {
    private final ISchoolProxyApplicationService service;

    /** 操作人统一取自成员一登录模块写入的 JWT 上下文。 */
    private final ICurrentUserProvider currentUserProvider;
    //根据学号来查找学生信息
    @GetMapping("/students")
    public JsonResponse<SchoolProxyStudentVO> findStudent(@RequestParam String studentNo) { requireSchoolUser(); return JsonResponse.success(service.findStudent(studentNo)); }
    //学校代替学生申请绿色通道，创建一份尚未提交的申请草稿
    @PostMapping("/applications/drafts")
    public JsonResponse<SchoolProxyApplicationVO> createDraft(@Valid @RequestBody SchoolProxyDraftDTO request) { return JsonResponse.success(service.createDraft(request,requireSchoolUser())); }
    //创建申请草稿后，会在下面弹出来提示，让学校管理员代学生来提交支撑材料
    @PostMapping("/applications/{applicationId}/attachments")
    public JsonResponse<Void> upload(@PathVariable Long applicationId, @RequestPart("file") MultipartFile file, @RequestParam String requestId) { service.uploadAttachment(applicationId,file,requestId,requireSchoolUser()); return JsonResponse.successMessage("附件上传成功"); }
    //提交附件后把刚才创造的草稿提交进入三级审核
    @PostMapping("/applications/{applicationId}/submit")
    public JsonResponse<SchoolProxyApplicationVO> submit(@PathVariable Long applicationId, @RequestParam Integer version, @RequestParam String requestId) { return JsonResponse.success(service.submit(applicationId,version,requestId,requireSchoolUser()),"学校代申请已提交审核"); }

    //getter,返回当前登录的学校管理员账号id
    private Long requireSchoolUser() {
        LoginUser user = currentUserProvider.getRequiredUser();
        if (user.getRoles() == null || !user.getRoles().contains("SCHOOL")) {
            throw new SecurityException("仅学校管理员可执行学校代申请");
        }
        return user.getUserId();
    }
}
