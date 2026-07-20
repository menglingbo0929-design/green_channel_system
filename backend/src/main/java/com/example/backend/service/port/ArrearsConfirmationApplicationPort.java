package com.example.backend.service.port;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.confirmation.ArrearsConfirmationQueryDTO;
import com.example.backend.model.vo.confirmation.PendingArrearsApplicationVO;

/**
 * 成员二提供给成员四的“待确认欠费申请读取”边界接口（Port）。
 *
 * <p>这个接口本身不对应任何数据库表，也不新增 Mapper。它只负责读取：
 *
 * <ul>
 *     <li>成员二从其维护的 application、arrears_application 等表取待确认申请与申报金额；</li>
 *     <li>只返回状态为 CONFIRM_PENDING 的有效申请，并返回 application 的 version；</li>
 *     <li>成员四据此校验金额并写入自己拥有的 arrears_confirmation 表。</li>
 * </ul>
 *
 * <p>完成状态由 {@link ArrearsConfirmationCompletionPort} 单独负责，避免成员二读取能力
 * 与成员三状态机职责混在同一个实现中。</p>
 *
 * <p>TODO(成员二/成员四)：成员二已提供 {@code ArrearsConfirmationApplicationService}
 * 基础实现；待成员一学生组织快照能力合入后，由成员四完成真实待确认数据联调并关闭 TODO。</p>
 */
public interface ArrearsConfirmationApplicationPort {

    /**
     * 读取学校端待确认的欠费申请列表。
     *
     * <p>返回对象中的学生信息、申请编号和申报金额由拥有这些数据的模块负责提供；
     * 成员四不在本模块拼写任何 application、student、arrears_application 的字段名。</p>
     */
    Page<PendingArrearsApplicationVO> findPendingPage(
            ArrearsConfirmationQueryDTO queryDTO,
            PageDTO pageDTO);

    /**
     * 读取一笔仍处于 CONFIRM_PENDING 的欠费申请，用于在确认前获得可信的申报金额快照。
     * 若申请不存在、不是欠费申请或状态不允许确认，实现方应抛出 BusinessException。
     */
    PendingArrearsApplicationVO findPendingByApplicationId(Long applicationId);

}
