package com.example.backend.application.dto;

import java.math.BigDecimal;

public record SubsidyApplicationSnapshot(BigDecimal expectedAmount, BigDecimal finalAmount) { }
