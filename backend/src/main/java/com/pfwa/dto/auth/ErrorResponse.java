package com.pfwa.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response DTO.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        String message,
        Instant timestamp,
        String path,
        String requestId,
        List<FieldError> fieldErrors
) {
    public static ErrorResponse of(String error, String message, String path) {
        return new ErrorResponse(error, message, Instant.now(), path, null, null);
    }

    public static ErrorResponse of(String error, String message, String path, String requestId) {
        return new ErrorResponse(error, message, Instant.now(), path, requestId, null);
    }

    public static ErrorResponse withFieldErrors(String error, String message, String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(error, message, Instant.now(), path, null, fieldErrors);
    }
}
