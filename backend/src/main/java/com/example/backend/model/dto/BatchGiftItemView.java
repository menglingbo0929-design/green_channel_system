package com.example.backend.model.dto;

public record BatchGiftItemView(Long id, Long batchId, Long giftItemId, String giftItemName,
                                Integer stockTotal, Integer reservedCount, Integer usedCount,
                                Integer perStudentLimit, Integer version) { }
