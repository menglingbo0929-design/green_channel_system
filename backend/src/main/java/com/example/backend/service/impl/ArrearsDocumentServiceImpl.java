package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.approval.port.ArrearsDocumentService;
import com.example.backend.mapper.ArrearsConfirmationMapper;
import com.example.backend.model.domain.ArrearsConfirmation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 成员四向成员三学校取消流程提供的欠费单据协作实现。
 *
 * <p>本类只操作成员四拥有的 {@code arrears_confirmation} 表。取消申请时，
 * 成员三负责申请状态、资源、审核记录和消息；本类只将仍然有效的欠费确认单据
 * 标记为逻辑作废，不物理删除记录。</p>
 */
@Service
@RequiredArgsConstructor
public class ArrearsDocumentServiceImpl implements ArrearsDocumentService {

    /** 成员四确认表 Mapper，只访问本模块拥有的单据记录。 */
    private final ArrearsConfirmationMapper arrearsConfirmationMapper;

    /**
     * 当前确认表没有“线下领取/发放已完成”的持久化字段或外部履约模块。
     * 因而成员四在第一阶段没有可判定的不可逆线下处理记录，返回 false。
     * 后续若引入履约表，应由该表负责人扩展此判断，而不是在确认表猜测状态。
     */
    @Override
    public boolean hasIrreversibleOfflineProcessing(Long applicationId) {
        return false;
    }

    /**
     * 将当前申请的有效欠费确认单据作废。
     *
     * <p>没有单据或单据此前已作废时不更新任何记录，因此同一取消请求的重复调用
     * 保持幂等。逻辑删除值写为本行主键，继续遵守
     * {@code uk_arrears_confirmation_application_id_deleted} 的唯一约束。</p>
     *
     * <p>该方法必须加入成员三的取消事务；作废更新失败时会抛出并使整个取消流程回滚。</p>
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void voidDocumentForCancellation(
            Long applicationId,
            String reason,
            Long operatorId
    ) {
        ArrearsConfirmation confirmation = arrearsConfirmationMapper.selectOne(
                new LambdaQueryWrapper<ArrearsConfirmation>()
                        .eq(ArrearsConfirmation::getApplicationId, applicationId)
                        .eq(ArrearsConfirmation::getDeleted, 0L)
        );
        if (confirmation == null) {
            return;
        }

        confirmation.setDeleted(confirmation.getId());
        confirmation.setUpdatedAt(LocalDateTime.now());
        arrearsConfirmationMapper.updateById(confirmation);
    }
}
