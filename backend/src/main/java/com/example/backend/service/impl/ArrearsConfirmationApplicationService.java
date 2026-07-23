package com.example.backend.service.impl;

import com.example.backend.model.dto.*;
import com.example.backend.mapper.ApplicationMapper;
import com.example.backend.mapper.ArrearsApplicationMapper;
import com.example.backend.service.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ArrearsConfirmationApplicationService implements ApplicationArrearsQueryService {
    private final ApplicationMapper applicationMapper; private final ArrearsApplicationMapper arrearsMapper;
    private final StudentOrganizationSnapshotQuery studentQuery;
    public ArrearsConfirmationApplicationService(ApplicationMapper applicationMapper, ArrearsApplicationMapper arrearsMapper,
                                                 StudentOrganizationSnapshotQuery studentQuery) {
        this.applicationMapper = applicationMapper; this.arrearsMapper = arrearsMapper; this.studentQuery = studentQuery;
    }
    @Override public PageResult<PendingArrearsApplication> pagePending(PendingArrearsQuery query) {
        return new PageResult<>(arrearsMapper.countPending(), query.pageNo(), query.pageSize(), arrearsMapper.pagePending(query.pageSize(), query.offset()));
    }
    @Override public ArrearsVoucherApplicantSnapshot getConfirmationDetail(Long applicationId) {
        return findVoucherApplicantsByApplicationIds(List.of(applicationId)).get(applicationId);
    }
    @Override public Map<Long, ArrearsVoucherApplicantSnapshot> findVoucherApplicantsByApplicationIds(Collection<Long> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) return Map.of();
        var applications = applicationIds.stream().distinct().map(applicationMapper::findRequired).toList();
        Map<Long, StudentOrganizationSnapshot> students = studentQuery.findByStudentIds(applications.stream().map(a -> a.getStudentId()).toList());
        Map<Long, List<ArrearsItemSnapshot>> items = new HashMap<>();
        for (ArrearsItemSnapshot item : arrearsMapper.findItemsByApplicationIds(applications.stream().map(a -> a.getId()).toList())) items.computeIfAbsent(item.applicationId(), ignored -> new ArrayList<>()).add(item);
        Map<Long, ArrearsVoucherApplicantSnapshot> result = new LinkedHashMap<>();
        for (var application : applications) {
            var student = students.get(application.getStudentId());
            List<ArrearsItemSnapshot> itemList = items.getOrDefault(application.getId(), List.of());
            BigDecimal total = itemList.stream().map(ArrearsItemSnapshot::declaredAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            result.put(application.getId(), new ArrearsVoucherApplicantSnapshot(application.getId(), application.getApplicationNo(), application.getVersion(), student.studentId(), student.studentNo(), student.studentName(), student.collegeName(), student.majorName(), student.gradeName(), student.className(), total, List.copyOf(itemList)));
        }
        return result;
    }
}
