package com.example.backend.service.impl;

import com.example.backend.model.domain.Application;
import com.example.backend.model.domain.ApplicationType;
import com.example.backend.model.domain.BatchType;
import com.example.backend.model.dto.GreenChannelEligibility;
import com.example.backend.mapper.ApplicationMapper;
import com.example.backend.model.dto.BatchSnapshot;
import com.example.backend.model.dto.StudentApplicationProfile;
import com.example.backend.service.BatchQueryService;
import com.example.backend.service.StudentProfileQueryService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/** Coordinates member-one snapshots into the member-two green-channel entry decision. */
@Service
public class GreenChannelEligibilityService {
    private final BatchQueryService batches;
    private final StudentProfileQueryService students;
    private final ApplicationMapper applications;

    public GreenChannelEligibilityService(BatchQueryService batches, StudentProfileQueryService students,
                                          ApplicationMapper applications) {
        this.batches = batches;
        this.students = students;
        this.applications = applications;
    }

    public GreenChannelEligibility check(Long studentId) {
        BatchSnapshot batch;
        try {
            batch = batches.getCurrentOpenGreenChannelBatch();
        } catch (IllegalStateException exception) {
            return denied("NO_OPEN_BATCH", "当前没有开放的绿色通道批次", null);
        }

        LocalDateTime now = LocalDateTime.now();
        if (batch.getStartTime() == null || batch.getEndTime() == null
                || now.isBefore(batch.getStartTime()) || now.isAfter(batch.getEndTime())) {
            return denied("OUT_OF_APPLICATION_TIME", "当前不在该批次的学生申请时间内", batch);
        }

        StudentApplicationProfile profile = students.getRequiredProfile(studentId);
        if (!batches.isGradeEligible(batch.getBatchId(), profile.getGradeId())) {
            return denied("GRADE_NOT_ELIGIBLE", "您的年级不在当前批次的适用范围内", batch);
        }
        if (!Integer.valueOf(1).equals(profile.getInfoComplete())) {
            return denied("PROFILE_INCOMPLETE", "请先完善个人信息后再申请", batch);
        }
        if (!Integer.valueOf(1).equals(profile.getOriginLoan()) && !Integer.valueOf(1).equals(profile.getCampusLoan())) {
            return denied("LOAN_CONDITION_NOT_MET", "需已申请生源地贷款或拟申请校园地贷款", batch);
        }
        Application existing = applications.findActiveByUnique(
                studentId, ApplicationType.GREEN_CHANNEL, BatchType.GREEN_CHANNEL, batch.getBatchId());
        if (existing != null) {
            return denied("APPLICATION_ALREADY_EXISTS", "当前批次已存在您的绿色通道申请", batch);
        }
        return new GreenChannelEligibility(true, null, "可以申请绿色通道", batch.getBatchId(), batch.getBatchName());
    }

    private GreenChannelEligibility denied(String code, String message, BatchSnapshot batch) {
        return new GreenChannelEligibility(false, code, message,
                batch == null ? null : batch.getBatchId(), batch == null ? null : batch.getBatchName());
    }
}
