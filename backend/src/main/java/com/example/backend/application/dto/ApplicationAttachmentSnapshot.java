package com.example.backend.application.dto;

/** Metadata only; the storage path is intentionally never exposed. */
public record ApplicationAttachmentSnapshot(
        Long id, Long applicationId, String fileId, String originalFilename,
        String contentType, long fileSize
) { }
