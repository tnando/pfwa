package com.pfwa.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Field-level validation error DTO.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldError(
        String field,
        String message,
        String code,
        String rejectedValue
) {
    public static FieldError of(String field, String message, String code) {
        return new FieldError(field, message, code, null);
    }
}
