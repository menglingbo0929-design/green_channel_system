package com.example.backend.application.dto;

/** Private attachment payload returned only through an authorized boundary. */
public record ApplicationAttachmentContent(String originalFilename, String contentType, byte[] content) { }
