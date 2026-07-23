package com.example.backend.service;

import com.example.backend.model.dto.ApplicationStateSnapshot;
import com.example.backend.model.dto.ReviewableApplicationEditCommand;

/** Approval owns authorization and audit; this port owns application persistence. */
public interface ReviewableApplicationEditService {
    ApplicationStateSnapshot editForReview(Long applicationId,
                                           ReviewableApplicationEditCommand command,
                                           Long operatorId);
}
