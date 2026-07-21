package com.example.backend.service.port;

import com.example.backend.model.dto.schoolproxy.SchoolProxyDraftDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyApplicationVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 成员二实现的学校代申请写入边界。
 *
 * 成员二拥有 application、明细、附件和资源表；该接口使成员四只编排学校端流程。
 *
 * <p>TODO(成员二)：在现有学校代申请主表创建入口上补齐附件、资源预占和正式提交；
 * 成员三已经通过 {@code ApprovalTransitionService.submitInitial} 提供写入 SUBMIT 审核记录并推进到
 * {@code COUNSELOR_PENDING} 的正式能力，成员二须在同一提交事务中调用。</p>
 */
public interface SchoolProxyApplicationPort {
    SchoolProxyApplicationVO createDraft(SchoolProxyDraftDTO command, Long operatorUserId);
    void uploadAttachment(Long applicationId, MultipartFile file, String requestId, Long operatorUserId);
    SchoolProxyApplicationVO submit(Long applicationId, Integer expectedVersion, String requestId, Long operatorUserId);
}
