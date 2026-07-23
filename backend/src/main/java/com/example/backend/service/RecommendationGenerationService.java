package com.example.backend.service;

/** Generates the post-completion subsidy-entry recommendations for a student. */
public interface RecommendationGenerationService {
    void generateForCompletedApplication(Long applicationId);
}
