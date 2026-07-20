package com.example.backend.service.port;

import com.example.backend.model.dto.schoolproxy.SchoolProxyDraftDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyApplicationVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 成员二实现的学校代申请写入边界。
 *
 * 成员二拥有 application、明细、附件和资源表；该接口使成员四只编排学校端流程。
 *
 * <p>TODO(成员二)：在现有学校代申请主表创建入口上补齐欠费/礼包明细、附件和提交；
 * TODO(成员三)：为正式提交写 SUBMIT 审核记录并推进到 COUNSELOR_PENDING。</p>
 */
public interface SchoolProxyApplicationPort {
    SchoolProxyApplicationVO createDraft(SchoolProxyDraftDTO command, Long operatorUserId);
    void uploadAttachment(Long applicationId, MultipartFile file, String requestId, Long operatorUserId);
    SchoolProxyApplicationVO submit(Long applicationId, Integer expectedVersion, String requestId, Long operatorUserId);
}
