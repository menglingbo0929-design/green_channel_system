package com.example.backend.approval.integration;

import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApprovalLevel;
import com.example.backend.approval.port.ApplicationStateQueryService;
import com.example.backend.approval.port.ApplicationStateSnapshot;
import com.example.backend.approval.port.ApplicationStateWriteService;
import org.springframework.stereotype.Component;

/**
 * Adapts the application module's state owner services to the approval module contract.
 * The application module remains the only physical writer of the application table.
 */
@Component
public class ApplicationStateServiceAdapter implements
        ApplicationStateQueryService,
        ApplicationStateWriteService {

    private final com.example.backend.application.port.ApplicationStateQueryService queryService;
    private final com.example.backend.application.port.ApplicationStateWriteService writeService;

    public ApplicationStateServiceAdapter(
            com.example.backend.application.port.ApplicationStateQueryService queryService,
            com.example.backend.application.port.ApplicationStateWriteService writeService
    ) {
        this.queryService = queryService;
        this.writeService = writeService;
    }

    @Override
    public ApplicationStateSnapshot getRequiredState(Long applicationId) {
        return toApprovalSnapshot(queryService.getRequiredState(applicationId));
    }

    @Override
    public ApplicationStateSnapshot updateState(
            Long applicationId,
            ApplicationStatus expectedStatus,
            ApplicationStatus targetStatus,
            ApprovalLevel targetLevel,
            Integer expectedVersion,
            Long operatorId
    ) {
        return toApprovalSnapshot(writeService.updateState(
                applicationId,
                toApplicationStatus(expectedStatus),
                toApplicationStatus(targetStatus),
                toApplicationLevel(targetLevel),
                expectedVersion,
                operatorId
        ));
    }

    @Override
    public ApplicationStateSnapshot incrementReviewRoundAndUpdateState(
            Long applicationId,
            ApplicationStatus expectedStatus,
            ApplicationStatus targetStatus,
            ApprovalLevel targetLevel,
            Integer expectedVersion,
            Long operatorId
    ) {
        return toApprovalSnapshot(writeService.incrementReviewRoundAndUpdateState(
                applicationId,
                toApplicationStatus(expectedStatus),
                toApplicationStatus(targetStatus),
                toApplicationLevel(targetLevel),
                expectedVersion,
                operatorId
        ));
    }

    private ApplicationStateSnapshot toApprovalSnapshot(
            com.example.backend.application.dto.ApplicationStateSnapshot source
    ) {
        return new ApplicationStateSnapshot(
                source.applicationId(),
                source.studentId(),
                com.example.backend.approval.persistence.type.BatchType.valueOf(source.batchType().name()),
                source.batchId(),
                com.example.backend.approval.domain.ApplicationType.valueOf(source.applicationType().name()),
                ApplicationStatus.valueOf(source.status().name()),
                ApprovalLevel.valueOf(source.currentLevel().name()),
                source.reviewRound(),
                source.version()
        );
    }

    private com.example.backend.application.domain.ApplicationStatus toApplicationStatus(
            ApplicationStatus status
    ) {
        return com.example.backend.application.domain.ApplicationStatus.valueOf(status.name());
    }

    private com.example.backend.application.domain.ApprovalLevel toApplicationLevel(
            ApprovalLevel level
    ) {
        return com.example.backend.application.domain.ApprovalLevel.valueOf(level.name());
    }
}
