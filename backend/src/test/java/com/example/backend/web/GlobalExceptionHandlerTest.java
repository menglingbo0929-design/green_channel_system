package com.example.backend.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsSecurityFailuresToForbidden() {
        var response = handler.handleForbidden(new SecurityException("forbidden"));

        assertEquals(403, response.getStatusCode().value());
        assertEquals(403, response.getBody().getCode());
        assertEquals("forbidden", response.getBody().getMessage());
    }

    @Test
    void mapsInvalidArgumentsToBadRequest() {
        var response = handler.handleBadRequest(new IllegalArgumentException("invalid"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals(400, response.getBody().getCode());
        assertEquals("invalid", response.getBody().getMessage());
    }
}
