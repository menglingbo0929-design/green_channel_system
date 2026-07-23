package com.example.backend.service.impl;

import com.example.backend.model.dto.schoolproxy.SchoolProxyDraftDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyApplicationVO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.service.ISchoolProxyApplicationService;
import com.example.backend.service.port.SchoolProxyApplicationPort;
import com.example.backend.service.port.SchoolProxyStudentQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 6.1.3 学校代申请演示流程，保持字段注入和直接调用 Port 的视频写法。 */
@Service
@RequiredArgsConstructor
public class SchoolProxyApplicationServiceImpl implements ISchoolProxyApplicationService {
    private final SchoolProxyStudentQueryPort studentPort;
    private final SchoolProxyApplicationPort applicationPort;

    @Override
    public SchoolProxyStudentVO findStudent(String studentNo) {
        return studentPort.findEnabledStudentByStudentNo(studentNo);
    }

    @Override
    public SchoolProxyApplicationVO createDraft(
            SchoolProxyDraftDTO request,
            Long operatorUserId
    ) {
        findStudent(request.getStudentNo());
        return applicationPort.createDraft(request, operatorUserId);
    }

    @Override
    public void uploadAttachment(
            Long applicationId,
            MultipartFile file,
            String requestId,
            Long operatorUserId
    ) {
        applicationPort.uploadAttachment(
                applicationId, file, requestId, operatorUserId);
    }

    @Override
    public SchoolProxyApplicationVO submit(
            Long applicationId,
            Integer version,
            String requestId,
            Long operatorUserId
    ) {
        return applicationPort.submit(
                applicationId, version, requestId, operatorUserId);
    }
}
