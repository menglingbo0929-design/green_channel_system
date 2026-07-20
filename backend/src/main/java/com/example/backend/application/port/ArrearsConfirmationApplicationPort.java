package com.example.backend.application.port;

import com.example.backend.application.dto.*;
import java.util.Collection;
import java.util.Map;

/** 成员四只读确认前申请与单据申请人快照的唯一入口。 */
public interface ArrearsConfirmationApplicationPort {
    PageResult<PendingArrearsApplication> pagePending(PendingArrearsQuery query);
    ArrearsVoucherApplicantSnapshot getConfirmationDetail(Long applicationId);
    Map<Long, ArrearsVoucherApplicantSnapshot> findVoucherApplicantsByApplicationIds(Collection<Long> applicationIds);
}
