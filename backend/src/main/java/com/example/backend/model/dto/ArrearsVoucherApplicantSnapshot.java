package com.example.backend.model.dto;

import java.math.BigDecimal;
import java.util.List;

public record ArrearsVoucherApplicantSnapshot(Long applicationId, String applicationNo, Integer version,
                                              Long studentId, String studentNo, String studentName, String collegeName,
                                              String majorName, String gradeName, String className,
                                              BigDecimal appliedAmount, List<ArrearsItemSnapshot> arrearsItems) { }
