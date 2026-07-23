package com.example.backend.service;

import com.example.backend.model.dto.*;
import java.util.Collection;
import java.util.Map;

/** 成员四只读确认前申请与单据申请人快照的唯一入口。 */
public interface ApplicationArrearsQueryService {
    PageResult<PendingArrearsApplication> pagePending(PendingArrearsQuery query);
    ArrearsVoucherApplicantSnapshot getConfirmationDetail(Long applicationId);
    Map<Long, ArrearsVoucherApplicantSnapshot> findVoucherApplicantsByApplicationIds(Collection<Long> applicationIds);
}
