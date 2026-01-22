package com.pfwa.dto.auth;

/**
 * Simple response DTO containing only a message.
 */
public record MessageResponse(
        String message
) {
    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}
