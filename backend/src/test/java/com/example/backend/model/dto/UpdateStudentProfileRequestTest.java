package com.example.backend.model.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateStudentProfileRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsAndNormalizesLegacyChineseDifficultyLevel() {
        var request = new UpdateStudentProfileRequest("10000000000", 1, 0, "困难", null);

        assertTrue(validator.validate(request).isEmpty());
        assertEquals("DIFFICULTY", request.normalizedDifficultyLevel());
    }

    @Test
    void acceptsEmptyDifficultyLevelAndStoresItAsNull() {
        var request = new UpdateStudentProfileRequest("10000000000", 1, 0, "", null);

        assertTrue(validator.validate(request).isEmpty());
        assertNull(request.normalizedDifficultyLevel());
    }

    @Test
    void rejectsUnknownDifficultyLevel() {
        var request = new UpdateStudentProfileRequest("10000000000", 1, 0, "UNKNOWN", null);

        assertEquals(1, validator.validate(request).size());
    }
}
