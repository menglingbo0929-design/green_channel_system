package com.example.backend.service.adapter;

import com.example.backend.approval.api.ApplicationStatusResult;
import com.example.backend.approval.domain.ApprovalStateMachine;
import com.example.backend.approval.persistence.mapper.ApprovalRecordMapper;
import com.example.backend.approval.port.ApplicationStateQueryService;
import com.example.backend.approval.port.ApplicationStateWriteService;
import com.example.backend.approval.service.ApprovalWorkflowService;
import com.example.backend.model.vo.supplement.SupplementCompletionResultVO;
import com.example.backend.service.port.ArrearsConfirmationCompletionPort;
import com.example.backend.service.port.SupplementCompletionPort;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 将成员三的正式审核工作流适配为成员四欠费确认和线下补录所需的两个接口。
 *
 * <p>本类只负责组装成员三现有的 {@link ApprovalWorkflowService} 并转换返回值，
 * 不复制状态转换条件，也不直接更新申请状态。</p>
 */
/**
 * Historical local bridge retained for source reference only.
 * Member three now supplies the two formal completion-port adapters;
 * registering this class would create duplicate Spring beans.
 */
@Deprecated(forRemoval = false)
public class ApprovalCompletionPortAdapter implements
        ArrearsConfirmationCompletionPort,
        SupplementCompletionPort {

    /** 成员三工作流读取申请状态所需的正式适配接口。 */
    @Autowired
    private ApplicationStateQueryService applicationStateQueryService;

    /** 成员三工作流写入申请状态所需的正式适配接口。 */
    @Autowired
    private ApplicationStateWriteService applicationStateWriteService;

    /** 成员三已经实现的审核状态机。 */
    @Autowired
    private ApprovalStateMachine approvalStateMachine;

    /** 成员三审核记录 Mapper，用于保留确认和补录的状态流转记录。 */
    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    /** 欠费确认成功后，按成员三规则把申请推进到完成状态。 */
    @Override
    public void completeAfterConfirmation(
            Long applicationId,
            Integer expectedVersion,
            String requestId,
            Long operatorUserId
    ) {
        workflowService().completeAfterConfirmation(
                applicationId,
                expectedVersion,
                requestId,
                operatorUserId
        );
    }

    /** 线下补录创建后，按是否包含欠费决定进入待确认或直接完成。 */
    @Override
    public SupplementCompletionResultVO completeSupplementReview(
            Long applicationId,
            boolean containsArrears,
            Integer expectedVersion,
            String requestId,
            Long operatorUserId
    ) {
        ApplicationStatusResult result = workflowService().completeSupplementReview(
                applicationId,
                containsArrears,
                expectedVersion,
                requestId,
                operatorUserId
        );

        SupplementCompletionResultVO completion = new SupplementCompletionResultVO();
        completion.setStatus(result.status().name());
        completion.setCurrentLevel(result.currentLevel().name());
        completion.setVersion(result.version());
        return completion;
    }

    /**
     * 直接复用成员三的正式工作流实现。
     * 当前成员三配置类未将该实现注册为 Bean，因此只在适配边界完成组装。
     */
    private ApprovalWorkflowService workflowService() {
        return new ApprovalWorkflowService(
                approvalStateMachine,
                applicationStateQueryService,
                applicationStateWriteService,
                approvalRecordMapper
        );
    }
}
