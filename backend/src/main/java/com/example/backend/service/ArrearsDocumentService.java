package com.example.backend.service;

/**
 * Member-four integration point for cancelling an application that may already
 * have an arrears confirmation document. The member-four module owns the
 * document table and its physical updates.
 */
public interface ArrearsDocumentService {

    boolean hasIrreversibleOfflineProcessing(Long applicationId);

    void voidDocumentForCancellation(Long applicationId, String reason, Long operatorId);
}
