package com.example.backend.model.dto;

import java.math.BigDecimal;

public record SubsidyApplicationSnapshot(BigDecimal expectedAmount, BigDecimal finalAmount) { }
