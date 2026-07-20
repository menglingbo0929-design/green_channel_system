package com.example.backend.application.port;

import com.example.backend.application.dto.StudentOrganizationSnapshot;
import java.util.Collection;
import java.util.Map;

/** 由成员一实现的批量只读能力；成员二不越权查询学生和组织表。 */
public interface StudentOrganizationSnapshotQuery {
    Map<Long, StudentOrganizationSnapshot> findByStudentIds(Collection<Long> studentIds);
}
