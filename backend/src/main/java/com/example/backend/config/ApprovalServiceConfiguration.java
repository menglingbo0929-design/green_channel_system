package com.example.backend.config;

import com.example.backend.model.domain.ApprovalStateMachine;
import com.example.backend.service.ApprovalWorkbenchQueryService;
import com.example.backend.mapper.ApprovalRecordMapper;
import com.example.backend.mapper.ApprovalSubmissionRecordMapper;
import com.example.backend.mapper.MessageReadRecordMapper;
import com.example.backend.mapper.SystemMessageMapper;
import com.example.backend.service.ApprovalMessageRecipientResolver;
import com.example.backend.service.ApprovalApplicationQueryPort;
import com.example.backend.service.ApprovalResourceService;
import com.example.backend.service.ApplicationStateQueryService;
import com.example.backend.service.ApplicationStateWriteService;
import com.example.backend.service.ArrearsDocumentService;
import com.example.backend.service.StudentScopeService;
import com.example.backend.service.impl.ApprovalBatchSubmissionService;
import com.example.backend.service.impl.ApprovalCancellationService;
import com.example.backend.service.impl.ApprovalReviewService;
import com.example.backend.service.impl.ApprovalWorkflowService;
import com.example.backend.service.impl.DefaultSystemMessageService;
import com.example.backend.service.impl.DefaultApprovalWorkbenchQueryService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class ApprovalServiceConfiguration {

    @Bean
    ApprovalStateMachine approvalStateMachine() {
        return new ApprovalStateMachine();
    }

    @Bean
    Clock approvalClock() {
        return Clock.systemDefaultZone();
    }

    /**
     * 审核流转、最终确认完成回调共用同一个工作流服务。
     *
     * 这里不能使用 {@code @ConditionalOnBean}：MyBatis Mapper 与跨模块状态适配器
     * 的 Bean 定义在条件判断阶段尚未完全注册时，会使整个工作流服务被跳过，
     * 进而导致 {@code ApprovalCompletionService} 无法注入。所需依赖直接作为
     * 方法参数注入，由 Spring 在创建该 Bean 时解析即可。
     */
    @Bean
    ApprovalWorkflowService approvalWorkflowService(
            ApprovalStateMachine stateMachine,
            ApplicationStateQueryService stateQueryService,
            ApplicationStateWriteService stateWriteService,
            ApprovalRecordMapper approvalRecordMapper
    ) {
        return new ApprovalWorkflowService(
                stateMachine,
                stateQueryService,
                stateWriteService,
                approvalRecordMapper
        );
    }

    @Bean
    ApprovalReviewService approvalReviewService(
            ApplicationStateQueryService stateQueryService,
            ApplicationStateWriteService stateWriteService,
            ApprovalRecordMapper approvalRecordMapper,
            ApprovalResourceService resourceService,
            com.example.backend.service.ApplicationDetailService detailService,
            ApprovalMessageRecipientResolver recipientResolver,
            ObjectProvider<com.example.backend.service.SystemMessageService> messageServiceProvider,
            StudentScopeService studentScopeService,
            com.example.backend.service.ReviewableApplicationEditService applicationEditService
    ) {
        return new ApprovalReviewService(
                stateQueryService,
                stateWriteService,
                approvalRecordMapper,
                resourceService,
                detailService,
                recipientResolver,
                messageServiceProvider,
                studentScopeService,
                applicationEditService
        );
    }

    @Bean
    ApprovalWorkbenchQueryService approvalWorkbenchQueryService(
            ApplicationStateQueryService stateQueryService,
            StudentScopeService studentScopeService,
            ApprovalApplicationQueryPort applicationQueryPort,
            ApprovalRecordMapper approvalRecordMapper
    ) {
        return new DefaultApprovalWorkbenchQueryService(
                stateQueryService, studentScopeService, applicationQueryPort, approvalRecordMapper
        );
    }

    @Bean
    ApprovalBatchSubmissionService approvalBatchSubmissionService(
            ApprovalSubmissionRecordMapper submissionRecords,
            ApprovalRecordMapper approvalRecords,
            ApplicationStateQueryService stateQueryService,
            ApplicationStateWriteService stateWriteService,
            com.example.backend.service.ApprovalBatchQueryService batchQueryService,
            com.example.backend.service.ApprovalSubmissionApplicationQueryService applicationQueryService,
            StudentScopeService scopeService,
            ApprovalResourceService resourceService,
            Clock approvalClock
    ) {
        return new ApprovalBatchSubmissionService(
                submissionRecords,
                approvalRecords,
                stateQueryService,
                stateWriteService,
                batchQueryService,
                applicationQueryService,
                scopeService,
                resourceService,
                approvalClock
        );
    }

    @Bean
    ApprovalCancellationService approvalCancellationService(
            ApprovalStateMachine stateMachine,
            ApplicationStateQueryService stateQueryService,
            ApplicationStateWriteService stateWriteService,
            ApprovalRecordMapper approvalRecordMapper,
            ApprovalResourceService resourceService,
            ArrearsDocumentService arrearsDocumentService,
            ApprovalMessageRecipientResolver messageRecipientResolver,
            ObjectProvider<com.example.backend.service.SystemMessageService> systemMessageService
    ) {
        return new ApprovalCancellationService(
                stateMachine,
                stateQueryService,
                stateWriteService,
                approvalRecordMapper,
                resourceService,
                arrearsDocumentService,
                messageRecipientResolver,
                systemMessageService
        );
    }

    @Bean
    @ConditionalOnBean({SystemMessageMapper.class, MessageReadRecordMapper.class})
    DefaultSystemMessageService systemMessageService(
            SystemMessageMapper systemMessageMapper,
            MessageReadRecordMapper messageReadRecordMapper,
            Clock approvalClock
    ) {
        return new DefaultSystemMessageService(
                systemMessageMapper,
                messageReadRecordMapper,
                approvalClock
        );
    }
}
