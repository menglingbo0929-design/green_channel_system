package com.example.backend.application.dto;

public record BatchGiftItemView(Long id, Long batchId, Long giftItemId, String giftItemName,
                                Integer stockTotal, Integer reservedCount, Integer usedCount,
                                Integer perStudentLimit, Integer version) { }
