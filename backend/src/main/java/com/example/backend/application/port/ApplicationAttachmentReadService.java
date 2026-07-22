package com.example.backend.application.port;

import com.example.backend.application.dto.ApplicationAttachmentContent;

/** Reviewer callers must establish their own scope before using this port. */
public interface ApplicationAttachmentReadService {
    ApplicationAttachmentContent readForAuthorizedReviewer(Long applicationId, Long attachmentId);
}
