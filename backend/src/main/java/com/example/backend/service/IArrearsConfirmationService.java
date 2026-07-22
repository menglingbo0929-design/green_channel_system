package com.example.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.confirmation.ArrearsConfirmationQueryDTO;
import com.example.backend.model.dto.confirmation.ConfirmArrearsDTO;
import com.example.backend.model.vo.confirmation.ConfirmResultVO;
import com.example.backend.model.vo.confirmation.PendingArrearsApplicationVO;

/**
 * 6.1.1 欠费信息最终确认模块的服务接口。
 *
 * <p>此接口只定义“待确认申请、查看申报金额、完成确认”三类能力。
 * 欠费单据的查询、学生查看和打印不放在这里，后续由独立 Voucher 模块实现。</p>
 */
public interface IArrearsConfirmationService {

    /** 查询待确认的欠费申请列表，列表中包含学生申报金额。 */
    Page<PendingArrearsApplicationVO> listPending(
            ArrearsConfirmationQueryDTO queryDTO,
            PageDTO pageDTO);

    /** 查看一笔仍待确认申请的学生信息和申报金额。 */
    PendingArrearsApplicationVO getPendingDetail(Long applicationId);

    /** 执行最终确认并返回确认结果摘要。 */
    ConfirmResultVO confirm(
            Long applicationId,
            ConfirmArrearsDTO confirmDTO,
            Long confirmUserId);
}
