package com.example.backend.service;

import com.example.backend.model.dto.ApplicationAttachmentContent;

/** Reviewer callers must establish their own scope before using this port. */
public interface ApplicationAttachmentReadService {
    ApplicationAttachmentContent readForAuthorizedReviewer(Long applicationId, Long attachmentId);
}
