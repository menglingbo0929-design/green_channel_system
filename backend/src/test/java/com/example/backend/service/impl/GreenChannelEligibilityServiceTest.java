package com.example.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.model.domain.Application;
import com.example.backend.model.dto.GreenChannelEligibility;
import com.example.backend.mapper.ApplicationMapper;
import com.example.backend.model.dto.BatchSnapshot;
import com.example.backend.model.dto.StudentApplicationProfile;
import com.example.backend.service.BatchQueryService;
import com.example.backend.service.StudentProfileQueryService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class GreenChannelEligibilityServiceTest {
    private final BatchQueryService batches = mock(BatchQueryService.class);
    private final StudentProfileQueryService students = mock(StudentProfileQueryService.class);
    private final ApplicationMapper applications = mock(ApplicationMapper.class);
    private final GreenChannelEligibilityService service = new GreenChannelEligibilityService(batches, students, applications);

    @Test
    void reportsNoOpenBatchWithoutQueryingStudentData() {
        when(batches.getCurrentOpenGreenChannelBatch()).thenThrow(new IllegalStateException());

        GreenChannelEligibility result = service.check(9L);

        assertFalse(result.allowed());
        assertEquals("NO_OPEN_BATCH", result.reasonCode());
        verify(students, never()).getRequiredProfile(9L);
    }

    @Test
    void reportsLoanConditionAfterCheckingTheOpenBatchAndProfile() {
        when(batches.getCurrentOpenGreenChannelBatch()).thenReturn(openBatch());
        when(students.getRequiredProfile(9L)).thenReturn(profile(0, 0));
        when(batches.isGradeEligible(3L, 7L)).thenReturn(true);

        GreenChannelEligibility result = service.check(9L);

        assertFalse(result.allowed());
        assertEquals("LOAN_CONDITION_NOT_MET", result.reasonCode());
    }

    @Test
    void rejectsAStudentWithAnExistingApplicationInTheCurrentBatch() {
        when(batches.getCurrentOpenGreenChannelBatch()).thenReturn(openBatch());
        when(students.getRequiredProfile(9L)).thenReturn(profile(1, 1));
        when(batches.isGradeEligible(3L, 7L)).thenReturn(true);
        when(applications.findActiveByUnique(9L, com.example.backend.model.domain.ApplicationType.GREEN_CHANNEL,
                com.example.backend.model.domain.BatchType.GREEN_CHANNEL, 3L)).thenReturn(new Application());

        GreenChannelEligibility result = service.check(9L);

        assertFalse(result.allowed());
        assertEquals("APPLICATION_ALREADY_EXISTS", result.reasonCode());
    }

    @Test
    void allowsAStudentWhoMeetsEveryRule() {
        when(batches.getCurrentOpenGreenChannelBatch()).thenReturn(openBatch());
        when(students.getRequiredProfile(9L)).thenReturn(profile(1, 0));
        when(batches.isGradeEligible(3L, 7L)).thenReturn(true);

        GreenChannelEligibility result = service.check(9L);

        assertTrue(result.allowed());
        assertEquals(3L, result.batchId());
    }

    private BatchSnapshot openBatch() {
        return BatchSnapshot.builder().batchId(3L).batchName("2026 年绿色通道")
                .startTime(LocalDateTime.now().minusDays(1)).endTime(LocalDateTime.now().plusDays(1))
                .eligibleGradeIds(List.of(7L)).build();
    }

    private StudentApplicationProfile profile(int originLoan, int campusLoan) {
        return StudentApplicationProfile.builder().studentId(9L).gradeId(7L).infoComplete(1)
                .originLoan(originLoan).campusLoan(campusLoan).build();
    }
}
