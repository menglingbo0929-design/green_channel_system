package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.mapper.ArrearsConfirmationMapper;
import com.example.backend.model.domain.ArrearsConfirmation;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.confirmation.ArrearsConfirmationQueryDTO;
import com.example.backend.model.dto.confirmation.ConfirmArrearsDTO;
import com.example.backend.model.vo.confirmation.ConfirmResultVO;
import com.example.backend.model.vo.confirmation.PendingArrearsApplicationVO;
import com.example.backend.service.IArrearsConfirmationService;
import com.example.backend.service.port.ArrearsConfirmationApplicationPort;
import com.example.backend.service.port.ArrearsConfirmationCompletionPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 6.1.1 欠费最终确认演示实现。
 *
 * <p>保持视频里的直接业务流程：读取待确认申请、写确认记录、调用状态流转、
 * 返回结果。重复确认继续由数据库唯一约束控制，不在 Service 中包装异常。</p>
 */
@Service
public class ArrearsConfirmationServiceImpl
        implements IArrearsConfirmationService {

    /** 只操作成员四拥有的 arrears_confirmation 表。 */
    @Autowired
    private ArrearsConfirmationMapper arrearsConfirmationMapper;

    @Autowired
    private ObjectProvider<ArrearsConfirmationApplicationPort> applicationPortProvider;

    @Autowired
    private ObjectProvider<ArrearsConfirmationCompletionPort> completionPortProvider;

    @Override
    public Page<PendingArrearsApplicationVO> listPending(
            ArrearsConfirmationQueryDTO queryDTO,
            PageDTO pageDTO
    ) {
        PageDTO actualPage = pageDTO == null ? new PageDTO() : pageDTO;
        return applicationPortProvider.getObject()
                .findPendingPage(queryDTO, actualPage);
    }

    @Override
    public PendingArrearsApplicationVO getPendingDetail(Long applicationId) {
        return applicationPortProvider.getObject()
                .findPendingByApplicationId(applicationId);
    }

    @Override
    @Transactional
    public ConfirmResultVO confirm(
            Long applicationId,
            ConfirmArrearsDTO confirmDTO,
            Long confirmUserId
    ) {
        PendingArrearsApplicationVO pendingApplication = getPendingDetail(applicationId);
        LocalDateTime now = LocalDateTime.now();

        ArrearsConfirmation confirmation = new ArrearsConfirmation()
                .setApplicationId(applicationId)
                .setVoucherNo(buildVoucherNo(applicationId, now.getYear()))
                .setAppliedAmount(pendingApplication.getAppliedAmount())
                .setConfirmedAmount(confirmDTO.getConfirmedAmount())
                .setConfirmUserId(confirmUserId)
                .setRequestId(confirmDTO.getRequestId())
                .setConfirmedAt(now)
                .setCreatedAt(now)
                .setUpdatedAt(now)
                .setDeleted(0L);
        arrearsConfirmationMapper.insert(confirmation);

        completionPortProvider.getObject().completeAfterConfirmation(
                applicationId,
                confirmDTO.getVersion(),
                confirmDTO.getRequestId(),
                confirmUserId
        );

        return new ConfirmResultVO()
                .setConfirmationId(confirmation.getId())
                .setApplicationId(applicationId)
                .setAppliedAmount(pendingApplication.getAppliedAmount())
                .setConfirmedAmount(confirmation.getConfirmedAmount())
                .setVoucherNo(confirmation.getVoucherNo())
                .setConfirmUserId(confirmUserId)
                .setConfirmedAt(now)
                .setApplicationStatus("COMPLETED");
    }

    /** 单据号继续使用已确定的 GC + 年份 + 六位申请 ID。 */
    private String buildVoucherNo(Long applicationId, int confirmYear) {
        return String.format("GC%d%06d", confirmYear, applicationId);
    }
}
