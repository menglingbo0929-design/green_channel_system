package com.example.backend.service.adapter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.model.dto.ArrearsItemSnapshot;
import com.example.backend.model.dto.ArrearsVoucherApplicantSnapshot;
import com.example.backend.model.dto.PageResult;
import com.example.backend.model.dto.PendingArrearsApplication;
import com.example.backend.model.dto.PendingArrearsQuery;
import com.example.backend.model.dto.StudentOrganizationSnapshot;
import com.example.backend.service.StudentOrganizationSnapshotQuery;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.confirmation.ArrearsConfirmationQueryDTO;
import com.example.backend.model.vo.confirmation.PendingArrearsApplicationVO;
import com.example.backend.model.vo.voucher.ArrearsVoucherItemVO;
import com.example.backend.service.port.ArrearsVoucherApplicantQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将成员二已经提交的申请读取接口适配为成员四确认和单据模块所需的返回对象。
 *
 * <p>本适配器不写 application、student 或欠费明细表，也不重新编写查询 SQL。
 * 当前成员二分页接口提供申请 ID、编号、版本、学生 ID 和申报金额；成员四在当前页内
 * 一次批量调用成员一的 StudentOrganizationSnapshotQuery，补齐学号、姓名及组织字段。
 * 详情和单据继续复用成员二的批量申请快照，不逐行访问其他成员维护的数据表。</p>
 */
@Component
@RequiredArgsConstructor
public class ApplicationReadPortAdapter implements
        com.example.backend.service.port.ArrearsConfirmationApplicationPort,
        ArrearsVoucherApplicantQueryPort {

    /** 成员二拥有的正式只读接口。 */
    private final com.example.backend.service.ApplicationArrearsQueryService applicationReadPort;

    /**
     * 成员一已经提供的学生与组织只读快照。
     *
     * <p>列表只按当前页的 studentId 做一次批量读取，既不逐行查询，也不让成员四
     * 直接访问 student、college、major、grade、class 等成员一维护的数据表。</p>
     */
    private final StudentOrganizationSnapshotQuery studentOrganizationSnapshotQuery;

    /**
     * 接通页面 8 的待确认分页。
     * 成员二当前接口尚未接收申请编号、学号和姓名筛选，所以本方法不在内存中伪造筛选结果。
     */
    @Override
    public Page<PendingArrearsApplicationVO> findPendingPage(
            ArrearsConfirmationQueryDTO queryDTO,
            PageDTO pageDTO
    ) {
        int pageNo = pageDTO == null || pageDTO.getPageNo() == null ? 1 : pageDTO.getPageNo();
        int pageSize = pageDTO == null || pageDTO.getPageSize() == null ? 10 : pageDTO.getPageSize();
        PageResult<PendingArrearsApplication> source = applicationReadPort.pagePending(
                new PendingArrearsQuery(pageNo, pageSize)
        );

        Page<PendingArrearsApplicationVO> result = new Page<>(
                source.pageNo(), source.pageSize(), source.total()
        );
        Map<Long, StudentOrganizationSnapshot> students = studentOrganizationSnapshotQuery
                .findByStudentIds(source.records().stream()
                        .map(PendingArrearsApplication::studentId)
                        .distinct()
                        .toList());
        result.setRecords(source.records().stream()
                .map(application -> toPendingVO(
                        application,
                        students.get(application.studentId())
                ))
                .toList());
        return result;
    }

    /** 确认前详情使用成员二批量快照，学生组织字段由成员一正式快照能力提供。 */
    @Override
    public PendingArrearsApplicationVO findPendingByApplicationId(Long applicationId) {
        return toPendingDetail(applicationReadPort.getConfirmationDetail(applicationId));
    }

    /** 单据列表一次批量转换所有申请快照，避免成员四逐行查询其他成员的数据表。 */
    @Override
    public Map<Long, com.example.backend.model.vo.voucher.ArrearsVoucherApplicantSnapshot>
            findVoucherApplicantsByApplicationIds(Collection<Long> applicationIds) {
        Map<Long, ArrearsVoucherApplicantSnapshot> source =
                applicationReadPort.findVoucherApplicantsByApplicationIds(applicationIds);
        Map<Long, com.example.backend.model.vo.voucher.ArrearsVoucherApplicantSnapshot> result =
                new LinkedHashMap<>();
        source.forEach((applicationId, snapshot) -> result.put(
                applicationId,
                toVoucherApplicant(snapshot)
        ));
        return result;
    }

    private PendingArrearsApplicationVO toPendingVO(
            PendingArrearsApplication source,
            StudentOrganizationSnapshot student
    ) {
        PendingArrearsApplicationVO result = new PendingArrearsApplicationVO()
                .setApplicationId(source.applicationId())
                .setApplicationNo(source.applicationNo())
                .setVersion(source.version())
                .setStudentId(source.studentId())
                .setAppliedAmount(source.appliedAmount())
                .setStatus("CONFIRM_PENDING");
        if (student == null) return result;
        return result
                .setStudentNo(student.studentNo())
                .setStudentName(student.studentName())
                .setCollegeName(student.collegeName())
                .setMajorName(student.majorName())
                .setGradeName(student.gradeName())
                .setClassName(student.className());
    }

    private PendingArrearsApplicationVO toPendingDetail(ArrearsVoucherApplicantSnapshot source) {
        return new PendingArrearsApplicationVO()
                .setApplicationId(source.applicationId())
                .setApplicationNo(source.applicationNo())
                .setVersion(source.version())
                .setStudentId(source.studentId())
                .setStudentNo(source.studentNo())
                .setStudentName(source.studentName())
                .setCollegeName(source.collegeName())
                .setMajorName(source.majorName())
                .setGradeName(source.gradeName())
                .setClassName(source.className())
                .setAppliedAmount(source.appliedAmount())
                .setStatus("CONFIRM_PENDING");
    }

    private com.example.backend.model.vo.voucher.ArrearsVoucherApplicantSnapshot
            toVoucherApplicant(ArrearsVoucherApplicantSnapshot source) {
        com.example.backend.model.vo.voucher.ArrearsVoucherApplicantSnapshot result =
                new com.example.backend.model.vo.voucher.ArrearsVoucherApplicantSnapshot();
        result.setApplicationId(source.applicationId());
        result.setStudentNo(source.studentNo());
        result.setStudentName(source.studentName());
        result.setCollegeName(source.collegeName());
        result.setMajorName(source.majorName());
        result.setGradeName(source.gradeName());
        result.setClassName(source.className());
        result.setArrearsItems(toVoucherItems(source.arrearsItems()));
        return result;
    }

    private List<ArrearsVoucherItemVO> toVoucherItems(List<ArrearsItemSnapshot> items) {
        if (items == null) return List.of();
        return items.stream().map(item -> {
            ArrearsVoucherItemVO result = new ArrearsVoucherItemVO();
            result.setFeeItemName(item.feeItemName());
            result.setDeclaredAmount(item.declaredAmount());
            return result;
        }).toList();
    }
}
