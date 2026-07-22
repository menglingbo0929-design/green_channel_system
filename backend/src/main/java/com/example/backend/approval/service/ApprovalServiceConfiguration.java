package com.example.backend.approval.service;

import com.example.backend.approval.domain.ApprovalStateMachine;
import com.example.backend.approval.api.ApprovalWorkbenchQueryService;
import com.example.backend.approval.persistence.mapper.ApprovalRecordMapper;
import com.example.backend.approval.persistence.mapper.ApprovalSubmissionRecordMapper;
import com.example.backend.approval.persistence.mapper.MessageReadRecordMapper;
import com.example.backend.approval.persistence.mapper.SystemMessageMapper;
import com.example.backend.approval.port.ApprovalMessageRecipientResolver;
import com.example.backend.approval.port.ApprovalApplicationQueryPort;
import com.example.backend.approval.port.ApprovalResourceService;
import com.example.backend.approval.port.ApplicationStateQueryService;
import com.example.backend.approval.port.ApplicationStateWriteService;
import com.example.backend.approval.port.ArrearsDocumentService;
import com.example.backend.approval.port.StudentScopeService;
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
            ObjectProvider<ApprovalResourceService> resourceServiceProvider,
            ObjectProvider<com.example.backend.application.port.ApplicationDetailService> detailServiceProvider,
            ObjectProvider<ApprovalMessageRecipientResolver> recipientResolverProvider,
            ObjectProvider<com.example.backend.approval.api.SystemMessageService> messageServiceProvider,
            ObjectProvider<StudentScopeService> studentScopeServiceProvider,
            ObjectProvider<com.example.backend.application.port.ReviewableApplicationEditService> applicationEditServiceProvider
    ) {
        return new ApprovalReviewService(
                stateQueryService,
                stateWriteService,
                approvalRecordMapper,
                resourceServiceProvider,
                detailServiceProvider,
                recipientResolverProvider,
                messageServiceProvider,
                studentScopeServiceProvider,
                applicationEditServiceProvider
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
            ObjectProvider<com.example.backend.approval.port.ApprovalBatchQueryService> batchQueryProvider,
            ObjectProvider<com.example.backend.approval.port.ApprovalSubmissionApplicationQueryService> applicationQueryProvider,
            ObjectProvider<StudentScopeService> scopeServiceProvider,
            ObjectProvider<ApprovalResourceService> resourceServiceProvider,
            Clock approvalClock
    ) {
        return new ApprovalBatchSubmissionService(
                submissionRecords,
                approvalRecords,
                stateQueryService,
                stateWriteService,
                batchQueryProvider,
                applicationQueryProvider,
                scopeServiceProvider,
                resourceServiceProvider,
                approvalClock
        );
    }

    @Bean
    @ConditionalOnBean({
            ApplicationStateQueryService.class,
            ApplicationStateWriteService.class,
            ApprovalRecordMapper.class,
            ApprovalResourceService.class,
            ArrearsDocumentService.class,
            ApprovalMessageRecipientResolver.class,
            com.example.backend.approval.api.SystemMessageService.class
    })
    ApprovalCancellationService approvalCancellationService(
            ApprovalStateMachine stateMachine,
            ApplicationStateQueryService stateQueryService,
            ApplicationStateWriteService stateWriteService,
            ApprovalRecordMapper approvalRecordMapper,
            ApprovalResourceService resourceService,
            ArrearsDocumentService arrearsDocumentService,
            ApprovalMessageRecipientResolver messageRecipientResolver,
            com.example.backend.approval.api.SystemMessageService systemMessageService
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
