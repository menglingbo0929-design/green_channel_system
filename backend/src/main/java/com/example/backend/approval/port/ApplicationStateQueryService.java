package com.example.backend.approval.port;

public interface ApplicationStateQueryService {

    ApplicationStateSnapshot getRequiredState(Long applicationId);
}
