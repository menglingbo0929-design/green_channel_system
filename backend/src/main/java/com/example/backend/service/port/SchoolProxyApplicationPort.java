package com.example.backend.service.port;

import com.example.backend.model.dto.schoolproxy.SchoolProxyDraftDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyApplicationVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 成员二实现的学校代申请写入边界。
 *
 * 成员二拥有 application、明细、附件和资源表；该接口使成员四只编排学校端流程。
 *
 * <p>正式实现已在同一事务链中完成附件、资源预占、正式提交和
 * {@code COUNSELOR_PENDING} 审核流转。</p>
 */
public interface SchoolProxyApplicationPort {
    SchoolProxyApplicationVO createDraft(SchoolProxyDraftDTO command, Long operatorUserId);
    void uploadAttachment(Long applicationId, MultipartFile file, String requestId, Long operatorUserId);
    SchoolProxyApplicationVO submit(Long applicationId, Integer expectedVersion, String requestId, Long operatorUserId);
}
