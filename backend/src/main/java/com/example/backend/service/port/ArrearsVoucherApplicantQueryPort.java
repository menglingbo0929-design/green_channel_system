package com.example.backend.service.port;

import com.example.backend.model.vo.voucher.ArrearsVoucherApplicantSnapshot;
import java.util.Collection;
import java.util.Map;

/**
 * 成员二提供的批量单据快照能力，避免成员四列表逐条查询共享申请表。
 *
 * <p>成员二已提供申请与欠费批量快照，成员一的 StudentOrganizationSnapshotQuery 也已接入；
 * 成员四通过 ApplicationReadPortAdapter 统一转换单据申请人信息，不再逐条查询学生组织表。</p>
 */
public interface ArrearsVoucherApplicantQueryPort {
    Map<Long, ArrearsVoucherApplicantSnapshot> findVoucherApplicantsByApplicationIds(Collection<Long> applicationIds);
}
