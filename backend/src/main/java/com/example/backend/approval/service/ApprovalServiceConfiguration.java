package com.example.backend.approval.service;

import com.example.backend.approval.domain.ApprovalStateMachine;
import com.example.backend.approval.persistence.mapper.ApprovalRecordMapper;
import com.example.backend.approval.persistence.mapper.MessageReadRecordMapper;
import com.example.backend.approval.persistence.mapper.SystemMessageMapper;
import com.example.backend.approval.port.ApprovalMessageRecipientResolver;
import com.example.backend.approval.port.ApprovalResourceService;
import com.example.backend.approval.port.ApplicationStateQueryService;
import com.example.backend.approval.port.ApplicationStateWriteService;
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

    @Bean
    @ConditionalOnBean({
            ApplicationStateQueryService.class,
            ApplicationStateWriteService.class,
            ApprovalRecordMapper.class
    })
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
    @ConditionalOnBean({
            ApplicationStateQueryService.class,
            ApplicationStateWriteService.class,
            ApprovalRecordMapper.class
    })
    ApprovalReviewService approvalReviewService(
            ApplicationStateQueryService stateQueryService,
            ApplicationStateWriteService stateWriteService,
            ApprovalRecordMapper approvalRecordMapper,
            ObjectProvider<ApprovalResourceService> resourceServiceProvider,
            ObjectProvider<com.example.backend.application.port.ApplicationDetailService> detailServiceProvider,
            ObjectProvider<ApprovalMessageRecipientResolver> recipientResolverProvider,
            ObjectProvider<com.example.backend.approval.api.SystemMessageService> messageServiceProvider,
            ObjectProvider<StudentScopeService> studentScopeServiceProvider
    ) {
        return new ApprovalReviewService(
                stateQueryService,
                stateWriteService,
                approvalRecordMapper,
                resourceServiceProvider,
                detailServiceProvider,
                recipientResolverProvider,
                messageServiceProvider,
                studentScopeServiceProvider
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
