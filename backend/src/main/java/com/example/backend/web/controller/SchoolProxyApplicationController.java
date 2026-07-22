package com.example.backend.web.controller;

import com.example.backend.common.JsonResponse;
import com.example.backend.model.dto.LoginUser;
import com.example.backend.model.dto.schoolproxy.SchoolProxyDraftDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyApplicationVO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.service.ISchoolProxyApplicationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 6.1.3 学校代申请 Controller：查学生、建草稿、传附件、正式提交。 */
@RestController
@RequestMapping("/api/school-proxy")
public class SchoolProxyApplicationController {

    @Autowired
    private ISchoolProxyApplicationService service;

    /** 操作人统一取自成员一登录模块写入的 JWT 上下文。 */
    @Autowired
    private ICurrentUserProvider currentUserProvider;

    @GetMapping("/students")
    public JsonResponse<SchoolProxyStudentVO> findStudent(@RequestParam String studentNo) { requireSchoolUser(); return JsonResponse.success(service.findStudent(studentNo)); }
    @PostMapping("/applications/drafts")
    public JsonResponse<SchoolProxyApplicationVO> createDraft(@Valid @RequestBody SchoolProxyDraftDTO request) { return JsonResponse.success(service.createDraft(request,requireSchoolUser())); }
    @PostMapping("/applications/{applicationId}/attachments")
    public JsonResponse<Void> upload(@PathVariable Long applicationId, @RequestPart("file") MultipartFile file, @RequestParam String requestId) { service.uploadAttachment(applicationId,file,requestId,requireSchoolUser()); return JsonResponse.successMessage("附件上传成功"); }
    @PostMapping("/applications/{applicationId}/submit")
    public JsonResponse<SchoolProxyApplicationVO> submit(@PathVariable Long applicationId, @RequestParam Integer version, @RequestParam String requestId) { return JsonResponse.success(service.submit(applicationId,version,requestId,requireSchoolUser()),"学校代申请已提交审核"); }

    /** 返回已通过身份校验的学校管理员用户 ID。 */
    private Long requireSchoolUser() {
        LoginUser user = currentUserProvider.getRequiredUser();
        if (user.getRoles() == null || !user.getRoles().contains("SCHOOL")) {
            throw new SecurityException("仅学校管理员可执行学校代申请");
        }
        return user.getUserId();
    }
}
