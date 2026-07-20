package com.example.backend.service.port;

import com.example.backend.model.vo.voucher.ArrearsVoucherApplicantSnapshot;
import java.util.Collection;
import java.util.Map;

/**
 * 成员二提供的批量单据快照能力，避免成员四列表逐条查询共享申请表。
 *
 * <p>TODO(成员一/成员四)：成员二已经实现申请和欠费批量快照；待成员一提供
 * StudentOrganizationSnapshotQuery 后，由成员四完成学生组织字段和单据权限联调。</p>
 */
public interface ArrearsVoucherApplicantQueryPort {
    Map<Long, ArrearsVoucherApplicantSnapshot> findVoucherApplicantsByApplicationIds(Collection<Long> applicationIds);
}
