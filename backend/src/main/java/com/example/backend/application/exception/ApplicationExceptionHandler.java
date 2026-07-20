package com.example.backend.application.exception;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApplicationExceptionHandler {
    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<Map<String, String>> handle(ApplicationException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("code", e.getCode(), "message", e.getMessage()));
    }
}
