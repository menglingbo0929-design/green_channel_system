package com.example.backend.application.port;

import com.example.backend.application.dto.ApplicationStateSnapshot;
import com.example.backend.application.dto.ReviewableApplicationEditCommand;

/** Approval owns authorization and audit; this port owns application persistence. */
public interface ReviewableApplicationEditService {
    ApplicationStateSnapshot editForReview(Long applicationId,
                                           ReviewableApplicationEditCommand command,
                                           Long operatorId);
}
