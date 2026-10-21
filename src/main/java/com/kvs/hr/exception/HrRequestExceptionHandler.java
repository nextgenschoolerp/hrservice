package com.kvs.hr.exception;

import com.kvs.core.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.UUID;

/**
 * Handles validation and malformed request exceptions for hrservice, returning standard ApiErrorResponse envelopes.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class HrRequestExceptionHandler {

    private static final String TRACE_HEADER = "X-Trace-Id";

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                             HttpServletRequest request,
                                                             HttpServletResponse response) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Request body is missing or contains an invalid value", request, response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                               HttpServletRequest request,
                                                               HttpServletResponse response) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", ex.getMessage(), request, response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                               HttpServletRequest request,
                                                               HttpServletResponse response) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Invalid value for parameter '" + ex.getName() + "'", request, response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request,
                                                             HttpServletResponse response) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, request, response);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                                HttpServletRequest request,
                                                                HttpServletResponse response) {
        return build(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE",
                "This endpoint returns " + ex.getSupportedMediaTypes() + "; send a matching Accept header", request, response);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String errorCode, String message,
                                                   HttpServletRequest request, HttpServletResponse response) {
        log.warn("Rejected request {} {}: {}", request.getMethod(), request.getRequestURI(), message);
        String traceId = request.getHeader(TRACE_HEADER);
        if (traceId != null && !response.containsHeader(TRACE_HEADER)) {
            response.setHeader(TRACE_HEADER, traceId);
        }
        ApiErrorResponse body = ApiErrorResponse.builder()
                .success(false)
                .status(status.value())
                .errorCode(errorCode)
                .message(message)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
