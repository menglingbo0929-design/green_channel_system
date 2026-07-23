package com.example.backend.model.dto;

import java.time.LocalDateTime;

public record StudentRecommendationView(
        Long id, Long batchId, String recommendationType, String content,
        boolean read, String targetPath, LocalDateTime createTime
) { }
