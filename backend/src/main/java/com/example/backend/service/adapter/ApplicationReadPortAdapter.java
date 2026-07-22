package com.example.backend.service.adapter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.application.dto.ArrearsItemSnapshot;
import com.example.backend.application.dto.ArrearsVoucherApplicantSnapshot;
import com.example.backend.application.dto.PageResult;
import com.example.backend.application.dto.PendingArrearsApplication;
import com.example.backend.application.dto.PendingArrearsQuery;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.confirmation.ArrearsConfirmationQueryDTO;
import com.example.backend.model.vo.confirmation.PendingArrearsApplicationVO;
import com.example.backend.model.vo.voucher.ArrearsVoucherItemVO;
import com.example.backend.service.port.ArrearsVoucherApplicantQueryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将成员二已经提交的申请读取接口适配为成员四确认和单据模块所需的返回对象。
 *
 * <p>本适配器不写 application、student 或欠费明细表，也不重新编写查询 SQL。
 * 当前成员二分页接口只提供申请 ID、编号、版本、学生 ID 和申报金额，因此列表先返回
 * 这些已经确定的字段；学生姓名和组织字段仍需成员一的 StudentOrganizationSnapshotQuery
 * 合入后，由成员二详情/批量快照接口统一补齐。</p>
 */
@Component
public class ApplicationReadPortAdapter implements
        com.example.backend.service.port.ArrearsConfirmationApplicationPort,
        ArrearsVoucherApplicantQueryPort {

    /** 成员二拥有的正式只读接口。 */
    @Autowired
    private com.example.backend.application.port.ArrearsConfirmationApplicationPort applicationReadPort;

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
        result.setRecords(source.records().stream().map(this::toPendingVO).toList());
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

    private PendingArrearsApplicationVO toPendingVO(PendingArrearsApplication source) {
        return new PendingArrearsApplicationVO()
                .setApplicationId(source.applicationId())
                .setApplicationNo(source.applicationNo())
                .setVersion(source.version())
                .setStudentId(source.studentId())
                .setAppliedAmount(source.appliedAmount())
                .setStatus("CONFIRM_PENDING");
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
