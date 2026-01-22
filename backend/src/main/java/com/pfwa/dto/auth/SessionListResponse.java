package com.pfwa.dto.auth;

import java.util.List;

/**
 * Response DTO for listing active sessions.
 */
public record SessionListResponse(
        List<SessionDto> sessions
) {
}
