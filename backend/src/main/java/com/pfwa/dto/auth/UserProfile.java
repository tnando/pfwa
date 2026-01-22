package com.pfwa.dto.auth;

import com.pfwa.entity.User;

import java.util.UUID;

/**
 * DTO representing user profile information returned in responses.
 */
public record UserProfile(
        UUID id,
        String email,
        String firstName,
        String lastName
) {
    /**
     * Creates a UserProfile from a User entity.
     */
    public static UserProfile fromEntity(User user) {
        return new UserProfile(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );
    }
}
