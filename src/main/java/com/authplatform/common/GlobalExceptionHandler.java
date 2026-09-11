package com.authplatform.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage() == null ? "Request failed" : ex.getMessage();
        String lower = msg.toLowerCase();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (lower.contains("not found")) status = HttpStatus.NOT_FOUND;
        else if (lower.contains("locked") || lower.contains("too many")) status = HttpStatus.TOO_MANY_REQUESTS;
        else if (lower.contains("invalid") && lower.contains("token") || lower.contains("expired")) status = HttpStatus.UNAUTHORIZED;
        else if (lower.contains("forbidden") || lower.contains("admin only")) status = HttpStatus.FORBIDDEN;
        return error(status, msg);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message));
    }
}
