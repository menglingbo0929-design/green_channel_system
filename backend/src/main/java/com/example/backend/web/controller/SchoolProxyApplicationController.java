package com.example.backend.web.controller;

import com.example.backend.common.JsonResponse;
import com.example.backend.model.dto.schoolproxy.SchoolProxyDraftDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyApplicationVO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
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

    @GetMapping("/students")
    public JsonResponse<SchoolProxyStudentVO> findStudent(@RequestParam String studentNo) { return JsonResponse.success(service.findStudent(studentNo)); }
    @PostMapping("/applications/drafts")
    public JsonResponse<SchoolProxyApplicationVO> createDraft(@Valid @RequestBody SchoolProxyDraftDTO request, @RequestHeader(value="X-User-Id",required=false) Long userId) { return JsonResponse.success(service.createDraft(request,userId)); }
    @PostMapping("/applications/{applicationId}/attachments")
    public JsonResponse<Void> upload(@PathVariable Long applicationId, @RequestPart("file") MultipartFile file, @RequestParam String requestId, @RequestHeader(value="X-User-Id",required=false) Long userId) { service.uploadAttachment(applicationId,file,requestId,userId); return JsonResponse.successMessage("附件上传成功"); }
    @PostMapping("/applications/{applicationId}/submit")
    public JsonResponse<SchoolProxyApplicationVO> submit(@PathVariable Long applicationId, @RequestParam Integer version, @RequestParam String requestId, @RequestHeader(value="X-User-Id",required=false) Long userId) { return JsonResponse.success(service.submit(applicationId,version,requestId,userId),"学校代申请已提交审核"); }
}
