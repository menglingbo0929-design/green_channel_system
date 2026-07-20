package com.example.backend.application.port;

import com.example.backend.application.dto.ArrearsVoucherApplicantSnapshot;
import java.util.Collection;
import java.util.Map;

/** 与成员四单据模块的固定调用边界。 */
public interface ArrearsVoucherApplicantQueryPort {
    Map<Long, ArrearsVoucherApplicantSnapshot> findVoucherApplicantsByApplicationIds(Collection<Long> applicationIds);
}
