package com.example.backend.web;

import com.example.backend.common.JsonResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps common application-boundary failures to stable HTTP/JSON responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<JsonResponse<Void>> handleForbidden(SecurityException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(JsonResponse.<Void>failure(exception.getMessage()).setCode(403));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<JsonResponse<Void>> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(JsonResponse.failure(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<JsonResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("请求参数校验失败");
        return ResponseEntity.badRequest().body(JsonResponse.failure(message));
    }
}
