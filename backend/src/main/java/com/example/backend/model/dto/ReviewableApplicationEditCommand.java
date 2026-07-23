package com.example.backend.model.dto;

import java.math.BigDecimal;
import java.util.List;

/** Reviewer-editable fields. Student profile fields are deliberately absent. */
public record ReviewableApplicationEditCommand(
        Integer expectedVersion,
        String applicationReason,
        List<ArrearsItemCommand> arrearsItems,
        List<GiftApplicationItemCommand> giftItems,
        BigDecimal expectedSubsidyAmount
) { }
