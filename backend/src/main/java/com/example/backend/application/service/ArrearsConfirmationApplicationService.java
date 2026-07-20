package com.example.backend.application.service;

import com.example.backend.application.domain.ApplicationStatus;
import com.example.backend.application.dto.*;
import com.example.backend.application.exception.ApplicationException;
import com.example.backend.application.mapper.ApplicationMapper;
import com.example.backend.application.mapper.ArrearsApplicationMapper;
import com.example.backend.application.port.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ArrearsConfirmationApplicationService implements ArrearsConfirmationApplicationPort, ArrearsVoucherApplicantQueryPort {
    private final ApplicationMapper applicationMapper; private final ArrearsApplicationMapper arrearsMapper;
    private final ObjectProvider<StudentOrganizationSnapshotQuery> studentQueryProvider;
    public ArrearsConfirmationApplicationService(ApplicationMapper applicationMapper, ArrearsApplicationMapper arrearsMapper,
                                                 ObjectProvider<StudentOrganizationSnapshotQuery> studentQueryProvider) {
        this.applicationMapper = applicationMapper; this.arrearsMapper = arrearsMapper; this.studentQueryProvider = studentQueryProvider;
    }
    @Override public PageResult<PendingArrearsApplication> pagePending(PendingArrearsQuery query) {
        query.validate(); return new PageResult<>(arrearsMapper.countPending(), query.pageNo(), query.pageSize(), arrearsMapper.pagePending(query.pageSize(), query.offset()));
    }
    @Override public ArrearsVoucherApplicantSnapshot getConfirmationDetail(Long applicationId) {
        return findVoucherApplicantsByApplicationIds(List.of(applicationId)).get(applicationId);
    }
    @Override public Map<Long, ArrearsVoucherApplicantSnapshot> findVoucherApplicantsByApplicationIds(Collection<Long> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) return Map.of();
        if (applicationIds.size() > 100) throw new ApplicationException("APPLICATION_BATCH_LIMIT_EXCEEDED", HttpStatus.BAD_REQUEST, "一次最多查询 100 个申请快照");
        var applications = applicationIds.stream().distinct().map(applicationMapper::findRequired).toList();
        if (applications.stream().anyMatch(a -> a.getStatus() != ApplicationStatus.CONFIRM_PENDING && a.getStatus() != ApplicationStatus.COMPLETED)) throw new ApplicationException("APPLICATION_INVALID_STATUS", HttpStatus.CONFLICT, "申请不处于可确认或已完成状态");
        StudentOrganizationSnapshotQuery studentQuery = studentQueryProvider.getIfAvailable();
        if (studentQuery == null) throw new ApplicationException("DEPENDENCY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "缺少成员一 StudentOrganizationSnapshotQuery 实现");
        Map<Long, StudentOrganizationSnapshot> students = studentQuery.findByStudentIds(applications.stream().map(a -> a.getStudentId()).toList());
        Map<Long, List<ArrearsItemSnapshot>> items = new HashMap<>();
        for (ArrearsItemSnapshot item : arrearsMapper.findItemsByApplicationIds(applications.stream().map(a -> a.getId()).toList())) items.computeIfAbsent(item.applicationId(), ignored -> new ArrayList<>()).add(item);
        Map<Long, ArrearsVoucherApplicantSnapshot> result = new LinkedHashMap<>();
        for (var application : applications) {
            var student = students.get(application.getStudentId());
            if (student == null) throw new ApplicationException("DEPENDENCY_DATA_INCOMPLETE", HttpStatus.SERVICE_UNAVAILABLE, "成员一未返回申请人的组织快照");
            List<ArrearsItemSnapshot> itemList = items.getOrDefault(application.getId(), List.of());
            if (itemList.isEmpty()) throw new ApplicationException("APPLICATION_ARREARS_NOT_FOUND", HttpStatus.CONFLICT, "申请没有有效欠费明细");
            BigDecimal total = itemList.stream().map(ArrearsItemSnapshot::declaredAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            result.put(application.getId(), new ArrearsVoucherApplicantSnapshot(application.getId(), application.getApplicationNo(), application.getVersion(), student.studentId(), student.studentNo(), student.studentName(), student.collegeName(), student.majorName(), student.gradeName(), student.className(), total, List.copyOf(itemList)));
        }
        return result;
    }
}
