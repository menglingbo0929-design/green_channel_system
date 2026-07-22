package com.example.backend.service;

import com.example.backend.model.dto.schoolproxy.SchoolProxyDraftDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyApplicationVO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import org.springframework.web.multipart.MultipartFile;

/** 6.1.3 学校代申请的成员四业务入口。 */
public interface ISchoolProxyApplicationService {
    SchoolProxyStudentVO findStudent(String studentNo);
    SchoolProxyApplicationVO createDraft(SchoolProxyDraftDTO request, Long operatorUserId);
    void uploadAttachment(Long applicationId, MultipartFile file, String requestId, Long operatorUserId);
    SchoolProxyApplicationVO submit(Long applicationId, Integer version, String requestId, Long operatorUserId);
}
