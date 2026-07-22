package com.example.backend.service.adapter;

import com.example.backend.application.dto.StudentOrganizationSnapshot;
import com.example.backend.application.port.StudentOrganizationSnapshotQuery;
import com.example.backend.model.dto.StudentApplicationProfile;
import com.example.backend.service.StudentProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将成员一现有的学生画像查询服务适配为成员二批量快照接口。
 * 单据、欠费确认和统计模块统一消费这一份学生与组织名称。
 */
@Component
@RequiredArgsConstructor
public class StudentOrganizationSnapshotQueryAdapter
        implements StudentOrganizationSnapshotQuery {
    private final StudentProfileQueryService studentProfileQueryService;

    /** 按学生 ID 批量组装只读组织快照。 */
    @Override
    public Map<Long, StudentOrganizationSnapshot> findByStudentIds(
            Collection<Long> studentIds
    ) {
        Map<Long, StudentOrganizationSnapshot> result = new LinkedHashMap<>();
        studentIds.stream().distinct().forEach(studentId -> {
            StudentApplicationProfile profile =
                    studentProfileQueryService.getRequiredProfile(studentId);
            result.put(studentId, new StudentOrganizationSnapshot(
                    profile.getStudentId(),
                    profile.getStudentNo(),
                    profile.getStudentName(),
                    profile.getCollegeName(),
                    profile.getMajorName(),
                    profile.getGradeName(),
                    profile.getClassName()
            ));
        });
        return result;
    }
}
